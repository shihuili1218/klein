/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ofcoder.klein.consensus.paxos;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.ofcoder.klein.consensus.facade.Consensus;
import com.ofcoder.klein.consensus.facade.MemberConfiguration;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.facade.nwr.Nwr;
import com.ofcoder.klein.consensus.facade.sm.SM;
import com.ofcoder.klein.consensus.facade.sm.SMRegistry;
import com.ofcoder.klein.consensus.paxos.core.Master;
import com.ofcoder.klein.consensus.paxos.core.RuntimeAccessor;
import com.ofcoder.klein.consensus.paxos.core.sm.MasterSM;
import com.ofcoder.klein.consensus.paxos.core.sm.MemberRegistry;
import com.ofcoder.klein.consensus.paxos.rpc.AcceptProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.ConfirmProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.HeartbeatProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.LearnProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.NewMasterProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.PreElectProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.PrepareProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.PushCompleteDataProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.RedirectProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.SnapSyncProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.vo.RedirectReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.RedirectRes;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.RpcClient;
import com.ofcoder.klein.rpc.facade.RpcEngine;
import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.spi.Join;
import com.ofcoder.klein.storage.facade.LogManager;

/**
 * Paxos Consensus.
 *
 * @author far.liu
 */
@Join
public class PaxosConsensus implements Consensus {
    private static final Logger LOG = LoggerFactory.getLogger(PaxosConsensus.class);
    private final PaxosNode self;
    private final ConsensusProp prop;
    private final RpcClient client;
    private final Proxy proxy;

    public PaxosConsensus(final ConsensusProp prop) {
        this.prop = prop;
        this.client = ExtensionLoader.getExtensionLoader(RpcClient.class).getJoin();
        ExtensionLoader.getExtensionLoader(Nwr.class).register(this.prop.getNwr());

        // reload self information from storage.
        LogManager<Proposal> logManager = ExtensionLoader.getExtensionLoader(LogManager.class).getJoin();
        this.self = (PaxosNode) logManager.loadMetaData(PaxosNode.Builder.aPaxosNode()
                .curInstanceId(0)
                .curProposalNo(0)
                .lastCheckpoint(0)
                .self(prop.getSelf())
                .build());
        LOG.info("self info: {}", self);

        initEngine();

        if (self.getSelf().isOutsider() || prop.getPaxosProp().isWriteOnMaster()) {
            this.proxy = new RedirectProxy(this.prop, this.self);
        } else {
            this.proxy = new DirectProxy(this.prop);
        }
    }

    private void initEngine() {

        MemberRegistry.getInstance().init(this.self.getSelf(), this.prop.getMembers());
        registerProcessor();
        MemberRegistry.getInstance().getMemberConfiguration().getAllMembers().forEach(this.client::createConnection);

        RuntimeAccessor.create(this.prop, this.self);

        LOG.info("cluster info: {}", MemberRegistry.getInstance().getMemberConfiguration());
    }

    private void registerProcessor() {
        // negotiation
        RpcEngine.registerProcessor(new PrepareProcessor(this.self));
        RpcEngine.registerProcessor(new AcceptProcessor(this.self));
        RpcEngine.registerProcessor(new ConfirmProcessor(this.self));
        RpcEngine.registerProcessor(new LearnProcessor(this.self));
        // master
        RpcEngine.registerProcessor(new HeartbeatProcessor(this.self));
        RpcEngine.registerProcessor(new SnapSyncProcessor(this.self));
        RpcEngine.registerProcessor(new NewMasterProcessor(this.self));
        RpcEngine.registerProcessor(new RedirectProcessor(this.self, this.prop));
        RpcEngine.registerProcessor(new PushCompleteDataProcessor(this.self));
        RpcEngine.registerProcessor(new PreElectProcessor(this.self));

    }

    @Override
    public void loadSM(final String group, final SM sm) {
        RuntimeAccessor.getLearner().loadSM(group, sm);
    }

    @Override
    public <E extends Serializable, D extends Serializable> Result<D> propose(final String group, final E data, final boolean apply) {
        Proposal proposal = new Proposal(group, data);
        return proxy.propose(proposal, apply);
    }

    @Override
    public void preheating() {
//        propose(Proposal.Noop.GROUP, Proposal.Noop.DEFAULT, true);
        SMRegistry.register(MasterSM.GROUP, new MasterSM());

        if (!this.prop.isJoinCluster()) {
            RuntimeAccessor.getMaster().lookMaster();
        } else {
            joinCluster(0);
        }
    }

    private void joinCluster(final int times) {
        int cur = times + 1;
//        if (cur >= 3) {
//            throw new ChangeMemberException("members change failed after trying many times");
//        }
        // add member
        boolean result = false;
        for (Endpoint member : MemberRegistry.getInstance().getMemberConfiguration().getMembersWithout(self.getSelf().getId())) {
            RedirectReq req = RedirectReq.Builder.aRedirectReq()
                    .nodeId(self.getSelf().getId())
                    .redirect(RedirectReq.CHANGE_MEMBER)
                    .changeOp(Master.ADD)
                    .changeTarget(Sets.newHashSet(self.getSelf()))
                    .build();
            RedirectRes changeRes = this.client.sendRequestSync(member, req, 2000);
            if (changeRes != null && changeRes.isChangeResult()) {
                result = true;
                break;
            }
        }
        if (!result) {
            joinCluster(cur);
        }
    }

    @Override
    public void shutdown() {
        if (RuntimeAccessor.getProposer() != null) {
            RuntimeAccessor.getProposer().shutdown();
        }
        if (RuntimeAccessor.getMaster() != null) {
            RuntimeAccessor.getMaster().shutdown();
        }
        if (RuntimeAccessor.getAcceptor() != null) {
            RuntimeAccessor.getAcceptor().shutdown();
        }
        if (RuntimeAccessor.getLearner() != null) {
            RuntimeAccessor.getLearner().shutdown();
        }
    }

    @Override
    public MemberConfiguration getMemberConfig() {
        return MemberRegistry.getInstance().getMemberConfiguration().createRef();
    }

    @Override
    public void addMember(final Endpoint endpoint) {
        if (getMemberConfig().isValid(endpoint.getId())) {
            return;
        }
        RuntimeAccessor.getMaster().changeMember(Master.ADD, Sets.newHashSet(endpoint));
    }

    @Override
    public void removeMember(final Endpoint endpoint) {
        if (!getMemberConfig().isValid(endpoint.getId())) {
            return;
        }
        RuntimeAccessor.getMaster().changeMember(Master.REMOVE, Sets.newHashSet(endpoint));
    }

}
