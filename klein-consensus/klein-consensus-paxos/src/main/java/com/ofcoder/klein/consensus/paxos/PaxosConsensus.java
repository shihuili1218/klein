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

import com.ofcoder.klein.consensus.facade.Consensus;
import com.ofcoder.klein.consensus.facade.MemberConfiguration;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.facade.nwr.Nwr;
import com.ofcoder.klein.consensus.facade.sm.SM;
import com.ofcoder.klein.consensus.facade.sm.SMRegistry;
import com.ofcoder.klein.consensus.facade.sm.SystemOp;
import com.ofcoder.klein.consensus.paxos.core.RuntimeAccessor;
import com.ofcoder.klein.consensus.paxos.core.sm.ChangeMemberOp;
import com.ofcoder.klein.consensus.paxos.core.sm.MasterSM;
import com.ofcoder.klein.consensus.paxos.core.sm.MemberManagerSM;
import com.ofcoder.klein.consensus.paxos.core.sm.MemberRegistry;
import com.ofcoder.klein.consensus.paxos.core.sm.PaxosMemberConfiguration;
import com.ofcoder.klein.consensus.paxos.rpc.AcceptProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.ConfirmProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.ElasticProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.HeartbeatProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.LearnProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.NewMasterProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.PreElectProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.PrepareProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.RedirectProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.SnapSyncProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.vo.ElasticReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.ElasticRes;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.RpcClient;
import com.ofcoder.klein.rpc.facade.RpcEngine;
import com.ofcoder.klein.serializer.hessian2.Hessian2Util;
import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.spi.Join;
import com.ofcoder.klein.storage.facade.LogManager;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final ProposeProxy proposeProxy;

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

        if (self.getSelf().isOutsider() || prop.getPaxosProp().isEnableMaster()) {
            this.proposeProxy = new MasterProposeProxy(this.prop, this.self);
        } else {
            this.proposeProxy = new UniversalProposeProxy(this.prop);
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
        RpcEngine.registerProcessor(new SnapSyncProcessor(this.self));
        RpcEngine.registerProcessor(new ElasticProcessor(this.self, this));
        // master
        RpcEngine.registerProcessor(new HeartbeatProcessor(this.self));
        RpcEngine.registerProcessor(new NewMasterProcessor(this.self));
        RpcEngine.registerProcessor(new RedirectProcessor(this.self, this.prop));
        RpcEngine.registerProcessor(new PreElectProcessor(this.self));
    }

    @Override
    public void loadSM(final String group, final SM sm) {
        RuntimeAccessor.getLearner().loadSM(group, sm);
    }

    @Override
    public <E extends Serializable, D extends Serializable> Result<D> propose(final String group, final E data, final boolean apply) {
        try {
            byte[] serializedData = Hessian2Util.serialize(data);
            Proposal proposal = new Proposal(group, serializedData, data instanceof SystemOp);
            return proposeProxy.propose(proposal, apply);
        } catch (Exception e) {
            LOG.error("Failed to serialize proposal data", e);
            Result.Builder<D> builder = Result.Builder.aResult();
            builder.state(Result.State.FAILURE);
            return builder.build();
        }
    }

    @Override
    public Result<Long> readIndex(final String group) {
        return proposeProxy.readIndex(group);
    }

    @Override
    public void preheating() {
//        propose(Proposal.Noop.GROUP, Proposal.Noop.DEFAULT, true);
        SMRegistry.register(MemberManagerSM.GROUP, new MemberManagerSM());
        SMRegistry.register(MasterSM.GROUP, new MasterSM());

        if (prop.getPaxosProp().isEnableMaster()) {
            RuntimeAccessor.getMaster().searchMaster();
        }
        if (this.prop.isElastic()) {
            joinCluster();
        }
    }

    private void joinCluster() {
        // add member
        ElasticReq req = new ElasticReq();
        req.setEndpoint(self.getSelf());
        req.setOp(ElasticReq.LAUNCH);

        for (Endpoint endpoint : MemberRegistry.getInstance().getMemberConfiguration().getAllMembers()) {
            try {
                ElasticRes res = client.sendRequestSync(endpoint, req);
                if (res.isResult()) {
                    return;
                }
            } catch (Exception e) {
                LOG.warn("join cluster fail, {}", e.getMessage());
            }
        }
    }

    private void exitCluster() {
        // remove member
        ElasticReq req = new ElasticReq();
        req.setEndpoint(self.getSelf());
        req.setOp(ElasticReq.SHUTDOWN);

        for (Endpoint endpoint : MemberRegistry.getInstance().getMemberConfiguration().getAllMembers()) {
            try {
                ElasticRes res = client.sendRequestSync(endpoint, req);
                if (res.isResult()) {
                    return;
                }
            } catch (Exception e) {
                LOG.warn("exit cluster fail, {}", e.getMessage());
            }
        }
    }

    @Override
    public void shutdown() {
        if (prop.isElastic()) {
            exitCluster();
        }
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
    public boolean changeMember(final List<Endpoint> add, final List<Endpoint> remove) {
        // todo: master in remove, transfer master
        PaxosMemberConfiguration curConfiguration = MemberRegistry.getInstance().getMemberConfiguration();

        // It only takes effect in the image
        Set<Endpoint> newConfig = curConfiguration.createRef().startJoinConsensus(add, remove);

        ChangeMemberOp firstPhase = new ChangeMemberOp();
        firstPhase.setNodeId(prop.getSelf().getId());
        firstPhase.setNewConfig(newConfig);
        firstPhase.setPhase(ChangeMemberOp.FIRST_PHASE);

        Result<Serializable> first = propose(MemberManagerSM.GROUP, firstPhase, false);
        LOG.info("change member first phase, add: {}, remove: {}, result: {}", add, remove, first.getState());

        if (first.getState() == Result.State.SUCCESS) {
            ChangeMemberOp secondPhase = new ChangeMemberOp();
            secondPhase.setNodeId(prop.getSelf().getId());
            secondPhase.setNewConfig(newConfig);
            secondPhase.setPhase(ChangeMemberOp.SECOND_PHASE);

            try {
                byte[] serializedData = Hessian2Util.serialize(secondPhase);
                Result<Serializable> second = propose(MemberManagerSM.GROUP, serializedData, false);
                LOG.info("change member second phase, add: {}, remove: {}, result: {}", add, remove, first.getState());
                return second.getState() == Result.State.SUCCESS;
            } catch (Exception e) {
                LOG.error("Failed to serialize second phase data. Phase: {}, NodeId: {}, NewConfig: {}",
                    secondPhase.getPhase(), secondPhase.getNodeId(), secondPhase.getNewConfig(), e);
                return false;
            }
        } else {
            return false;
        }
    }

}
