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
import com.ofcoder.klein.consensus.facade.Nwr;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.facade.sm.SM;
import com.ofcoder.klein.consensus.facade.sm.SMRegistry;
import com.ofcoder.klein.consensus.paxos.core.Master;
import com.ofcoder.klein.consensus.paxos.core.RoleAccessor;
import com.ofcoder.klein.consensus.paxos.core.sm.MasterSM;
import com.ofcoder.klein.consensus.paxos.core.sm.MemberRegistry;
import com.ofcoder.klein.consensus.paxos.rpc.AcceptProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.ConfirmProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.HeartbeatProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.LearnProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.NewMasterProcessor;
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
    private PaxosNode self;
    private ConsensusProp prop;
    private RpcClient client;
    private Proxy proxy;
    private Nwr nwr;

    @Override
    public <E extends Serializable, D extends Serializable> Result<D> propose(final String group, final E data, final boolean apply) {
        Proposal proposal = new Proposal(group, data);
        return this.proxy.propose(proposal, apply);
    }

    private void loadSM(final String group, final SM sm) {
        RoleAccessor.getLearner().loadSM(group, sm);
    }

    @Override
    public void setListener(final LifecycleListener listener) {
        RoleAccessor.getMaster().addHealthyListener(healthy -> {
            if (Master.ElectState.allowPropose(healthy)) {
                listener.prepared();
            }
        });
    }

    @Override
    public void init(final ConsensusProp op) {
        this.prop = op;
        this.client = ExtensionLoader.getExtensionLoader(RpcClient.class).getJoin();
        this.nwr = ExtensionLoader.getExtensionLoader(Nwr.class).getJoin(this.prop.getNwr());

        loadNode();
        this.proxy = this.prop.getPaxosProp().isWrite() ? new RedirectProxy(this.prop, this.self) : new DirectProxy(this.prop);

        MemberRegistry.getInstance().init(this.prop.getMembers(), this.nwr);
        registerProcessor();
        MemberRegistry.getInstance().getMemberConfiguration().getAllMembers().forEach(it -> this.client.createConnection(it));

        RoleAccessor.create(this.prop, this.self);
        SMRegistry.register(MasterSM.GROUP, new MasterSM());
        SMRegistry.getSms().forEach(this::loadSM);
        LOG.info("cluster info: {}", MemberRegistry.getInstance().getMemberConfiguration());
        if (!this.prop.isJoinCluster()) {
            RoleAccessor.getMaster().electingMaster();
            preheating();
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

    private void preheating() {
//        propose(Proposal.Noop.GROUP, Proposal.Noop.DEFAULT, true);
    }

    private void loadNode() {
        // reload self information from storage.

        LogManager<Proposal> logManager = ExtensionLoader.getExtensionLoader(LogManager.class).getJoin();
        this.self = (PaxosNode) logManager.loadMetaData(PaxosNode.Builder.aPaxosNode()
                .curInstanceId(0)
                .curAppliedInstanceId(0)
                .curProposalNo(0)
                .lastCheckpoint(0)
                .self(prop.getSelf())
                .build());

        LOG.info("self info: {}", self);
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
    }

    @Override
    public void shutdown() {
        if (RoleAccessor.getProposer() != null) {
            RoleAccessor.getProposer().shutdown();
        }
        if (RoleAccessor.getMaster() != null) {
            RoleAccessor.getMaster().shutdown();
        }
        if (RoleAccessor.getAcceptor() != null) {
            RoleAccessor.getAcceptor().shutdown();
        }
        if (RoleAccessor.getLearner() != null) {
            RoleAccessor.getLearner().shutdown();
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
        RoleAccessor.getMaster().changeMember(Master.ADD, Sets.newHashSet(endpoint));
    }

    @Override
    public void removeMember(final Endpoint endpoint) {
        if (!getMemberConfig().isValid(endpoint.getId())) {
            return;
        }
        RoleAccessor.getMaster().changeMember(Master.REMOVE, Sets.newHashSet(endpoint));
    }

}
