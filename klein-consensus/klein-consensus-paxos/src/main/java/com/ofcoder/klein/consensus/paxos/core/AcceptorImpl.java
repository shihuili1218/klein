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

import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.Proposal;
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
    private LogManager<Proposal> logManager;

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
        LOG.info("processing the accept message from node-{}, instanceId: {}, data.size: {}", req.getNodeId(), req.getInstanceId(), req.getData().size());

        try {
            logManager.getLock().writeLock().lock();

            final long selfProposalNo = self.getCurProposalNo();
            final long selfInstanceId = self.getCurInstanceId();
            final PaxosMemberConfiguration memberConfiguration = memberConfig.createRef();

            // check proposals include change member
            for (Proposal datum : req.getData()) {
                if (datum.getData() instanceof ChangeMemberOp) {
                    ChangeMemberOp data = (ChangeMemberOp) datum.getData();
                    memberConfig.seenNewConfig(data.getVersion(), data.getNewConfig());
                }
            }

            if (req.getInstanceId() <= self.getLastCheckpoint() || req.getInstanceId() <= RoleAccessor.getLearner().getLastAppliedInstanceId()) {
                return AcceptRes.Builder.anAcceptRes()
                        .nodeId(self.getSelf().getId())
                        .result(false)
                        .instanceState(Instance.State.CONFIRMED)
                        .curInstanceId(selfInstanceId)
                        .curProposalNo(selfProposalNo).build();
            }

            Instance<Proposal> localInstance = logManager.getInstance(req.getInstanceId());
            if (localInstance == null) {
                localInstance = Instance.Builder.<Proposal>anInstance()
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
            if (!checkAcceptReqValidity(memberConfiguration, selfProposalNo, req)) {
                AcceptRes res = AcceptRes.Builder.anAcceptRes()
                        .nodeId(self.getSelf().getId())
                        .result(false)
                        .curProposalNo(selfProposalNo)
                        .curInstanceId(selfInstanceId)
                        .instanceState(localInstance.getState())
                        .build();
                logManager.updateInstance(localInstance);
                return res;
            }

            AcceptRes.Builder resBuilder = AcceptRes.Builder.anAcceptRes()
                    .nodeId(self.getSelf().getId())
                    .curInstanceId(selfInstanceId)
                    .curProposalNo(selfProposalNo);

            if (localInstance.getState() == Instance.State.CONFIRMED) {
                resBuilder.result(false)
                        .instanceState(localInstance.getState());
            } else {
                localInstance.setState(Instance.State.ACCEPTED);
                localInstance.setProposalNo(req.getProposalNo());
                localInstance.setGrantedValue(req.getData());
                localInstance.setChecksum(req.getChecksum());
                logManager.updateInstance(localInstance);

                resBuilder.result(true)
                        .instanceState(localInstance.getState());
            }
            return resBuilder.build();
        } finally {
            self.updateCurProposalNo(req.getProposalNo());
            self.updateCurInstanceId(req.getInstanceId());

            logManager.getLock().writeLock().unlock();
        }
    }

    @Override
    public PrepareRes handlePrepareRequest(final PrepareReq req, final boolean isSelf) {
        LOG.info("processing the prepare message from node-{}, isSelf: {}", req.getNodeId(), isSelf);
        final long curProposalNo = self.getCurProposalNo();
        final long curInstanceId = self.getCurInstanceId();
        long lastCheckpoint = self.getLastCheckpoint();
        final PaxosMemberConfiguration memberConfiguration = memberConfig.createRef();

        PrepareRes.Builder res = PrepareRes.Builder.aPrepareRes()
                .nodeId(self.getSelf().getId())
                .curProposalNo(curProposalNo)
                .curInstanceId(curInstanceId)
                .nodeState(NodeState.Builder.aNodeState()
                        .nodeId(self.getSelf().getId())
                        .maxInstanceId(curInstanceId)
                        .lastCheckpoint(lastCheckpoint)
                        .lastAppliedInstanceId(RoleAccessor.getLearner().getLastAppliedInstanceId())
                        .build());

        if (!checkPrepareReqValidity(memberConfiguration, curProposalNo, req, isSelf)) {
            return res.result(false).instances(new ArrayList<>()).build();
        } else {
            List<Instance<Proposal>> instances = logManager.getInstanceNoConfirm();
            return res.result(true).instances(instances).build();
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
        self.updateCurProposalNo(req.getProposalNo());
        return true;
    }

    private boolean checkAcceptReqValidity(final PaxosMemberConfiguration paxosMemberConfiguration, final long selfProposalNo, final BaseReq req) {
        if (!paxosMemberConfiguration.isValid(req.getNodeId())
                || req.getMemberConfigurationVersion() < paxosMemberConfiguration.getVersion()
                || req.getProposalNo() < selfProposalNo) {
            return false;
        }
        return true;
    }

}
