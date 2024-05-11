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
package com.ofcoder.klein.consensus.paxos.core;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.consensus.facade.Command;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.core.sm.ChangeMemberOp;
import com.ofcoder.klein.consensus.paxos.core.sm.MemberRegistry;
import com.ofcoder.klein.consensus.paxos.core.sm.PaxosMemberConfiguration;
import com.ofcoder.klein.consensus.paxos.rpc.vo.AcceptReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.AcceptRes;
import com.ofcoder.klein.consensus.paxos.rpc.vo.BaseReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.NodeState;
import com.ofcoder.klein.consensus.paxos.rpc.vo.PrepareReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.PrepareRes;
import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.storage.facade.Instance;
import com.ofcoder.klein.storage.facade.LogManager;

/**
 * Acceptor implement.
 *
 * @author far.liu
 */
public class AcceptorImpl implements Acceptor {
    private static final Logger LOG = LoggerFactory.getLogger(AcceptorImpl.class);

    private final PaxosNode self;
    private final PaxosMemberConfiguration memberConfig;
    private LogManager<Command> logManager;
    private final Object negLock = new Object();

    public AcceptorImpl(final PaxosNode self) {
        this.self = self;
        this.memberConfig = MemberRegistry.getInstance().getMemberConfiguration();
    }

    @Override
    public void init(final ConsensusProp op) {
        logManager = ExtensionLoader.getExtensionLoader(LogManager.class).getJoin();
    }

    @Override
    public void shutdown() {
        // do nothing.
    }

    @Override
    public AcceptRes handleAcceptRequest(final AcceptReq req, final boolean isSelf) {
        LOG.info("processing the accept request from node-{}, instanceId: {}, proposalNO: {}, checksum: {}", req.getNodeId(), req.getInstanceId(), req.getProposalNo(), req.getChecksum());

        final long selfProposalNo = self.getCurProposalNo();
        final long selfInstanceId = self.getCurInstanceId();
        final PaxosMemberConfiguration memberConfiguration = memberConfig.createRef();
        final long selfCheckpoint = RuntimeAccessor.getLearner().getLastCheckpoint();
        final NodeState selfState = NodeState.Builder.aNodeState()
                .nodeId(self.getSelf().getId())
                .maxInstanceId(selfInstanceId)
                .lastCheckpoint(selfCheckpoint)
                .lastAppliedInstanceId(RuntimeAccessor.getLearner().getLastAppliedInstanceId())
                .build();

        if (req.getInstanceId() <= selfCheckpoint || req.getInstanceId() <= RuntimeAccessor.getLearner().getLastAppliedInstanceId()) {

            return AcceptRes.Builder.anAcceptRes()
                    .nodeId(self.getSelf().getId())
                    .result(false)
                    .instanceState(Instance.State.CONFIRMED)
                    .nodeState(selfState)
                    .curInstanceId(selfInstanceId)
                    .curProposalNo(selfProposalNo).build();
        }
        synchronized (negLock) {
            try {
                logManager.getLock(req.getInstanceId()).writeLock().lock();

                Instance<Command> localInstance = logManager.getInstance(req.getInstanceId());
                if (localInstance == null) {
                    localInstance = Instance.Builder.<Command>anInstance()
                            .instanceId(req.getInstanceId())
                            .proposalNo(req.getProposalNo())
                            .state(Instance.State.PREPARED)
                            .build();
                }

                // This check logic must be in the synchronized block to avoid the following situations
                // T1: acc<proposalNo = 1> check result of true                  ---- wait
                // T2: pre<proposalNo = 2>                                       ---- granted
                // T2: acc<proposalNo = 2> check result is true                  ---- granted
                // T1: overwrites the accept request from T2
                if (!checkAcceptReqValidity(memberConfiguration, selfProposalNo, req, isSelf)) {
                    AcceptRes res = AcceptRes.Builder.anAcceptRes()
                            .nodeId(self.getSelf().getId())
                            .result(false)
                            .curProposalNo(selfProposalNo)
                            .curInstanceId(selfInstanceId)
                            .instanceState(localInstance.getState())
                            .nodeState(selfState)
                            .build();
                    logManager.updateInstance(localInstance);
                    return res;
                }

                // check proposals include change member
                for (Command datum : req.getData()) {
                    if (datum.getData() instanceof ChangeMemberOp) {
                        ChangeMemberOp data = (ChangeMemberOp) datum.getData();
                        memberConfig.seenNewConfig(data.getVersion(), data.getNewConfig());
                    }
                }

                AcceptRes.Builder resBuilder = AcceptRes.Builder.anAcceptRes()
                        .nodeId(self.getSelf().getId())
                        .curInstanceId(selfInstanceId)
                        .curProposalNo(selfProposalNo);

                if (localInstance.getState() == Instance.State.CONFIRMED) {
                    resBuilder.result(false)
                            .instanceState(localInstance.getState())
                            .nodeState(selfState);
                } else {
                    localInstance.setState(Instance.State.ACCEPTED);
                    localInstance.setProposalNo(req.getProposalNo());
                    localInstance.setGrantedValue(req.getData());
                    localInstance.setChecksum(req.getChecksum());
                    logManager.updateInstance(localInstance);

                    resBuilder.result(true)
                            .instanceState(localInstance.getState())
                            .nodeState(selfState);
                }
                return resBuilder.build();
            } finally {
                self.updateCurInstanceId(req.getInstanceId());

                logManager.getLock(req.getInstanceId()).writeLock().unlock();
            }
        }
    }

    @Override
    public PrepareRes handlePrepareRequest(final PrepareReq req, final boolean isSelf) {
        LOG.info("processing the prepare request from node-{}, {}, isSelf: {}", req.getNodeId(), req.getProposalNo(), isSelf);
        synchronized (negLock) {
            final long curProposalNo = self.getCurProposalNo();
            final long curInstanceId = self.getCurInstanceId();
            long lastCheckpoint = RuntimeAccessor.getLearner().getLastCheckpoint();
            final PaxosMemberConfiguration memberConfiguration = memberConfig.createRef();

            PrepareRes.Builder res = PrepareRes.Builder.aPrepareRes()
                    .nodeId(self.getSelf().getId())
                    .curProposalNo(curProposalNo)
                    .curInstanceId(curInstanceId);

            if (!checkPrepareReqValidity(memberConfiguration, curProposalNo, req, isSelf)) {
                return res.result(false).instances(new ArrayList<>()).build();
            } else {
                List<Instance<Command>> instances = logManager.getInstanceNoConfirm();
                return res.result(true).instances(instances).build();
            }
        }
    }

    private boolean checkPrepareReqValidity(final PaxosMemberConfiguration paxosMemberConfiguration,
                                            final long selfProposalNo, final BaseReq req,
                                            final boolean isSelf) {
        boolean checkProposalNo = isSelf ? req.getProposalNo() >= selfProposalNo : req.getProposalNo() > selfProposalNo;
        if (!paxosMemberConfiguration.isValid(req.getNodeId())
                || req.getMemberConfigurationVersion() < paxosMemberConfiguration.getVersion()
                || !checkProposalNo) {

            LOG.info("checkPrepareReqValidity, req.version: {}, local.version: {}, req.proposalNo: {}, local.proposalNo: {}, checkProposalNo: {}",
                    req.getMemberConfigurationVersion(), paxosMemberConfiguration.getVersion(), req.getProposalNo(), selfProposalNo, checkProposalNo);
            return false;
        }
        if (!isSelf) {
            if (RuntimeAccessor.getSkipPrepare().get() == ProposerImpl.PrepareState.PREPARED) {
                RuntimeAccessor.getSkipPrepare().compareAndSet(ProposerImpl.PrepareState.PREPARED, ProposerImpl.PrepareState.NO_PREPARE);
            }
        }
        self.updateCurProposalNo(req.getProposalNo());
        return true;
    }

    private boolean checkAcceptReqValidity(final PaxosMemberConfiguration paxosMemberConfiguration, final long selfProposalNo, final BaseReq req,
                                           final boolean isSelf) {
        if (!paxosMemberConfiguration.isValid(req.getNodeId())
                || req.getMemberConfigurationVersion() < paxosMemberConfiguration.getVersion()
                || req.getProposalNo() < selfProposalNo) {
            return false;
        }
        if (!isSelf) {
            if (RuntimeAccessor.getSkipPrepare().get() == ProposerImpl.PrepareState.PREPARED) {
                RuntimeAccessor.getSkipPrepare().compareAndSet(ProposerImpl.PrepareState.PREPARED, ProposerImpl.PrepareState.NO_PREPARE);
            }
        }
        self.updateCurProposalNo(req.getProposalNo());
        return true;
    }

}
