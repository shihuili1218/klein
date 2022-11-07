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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.common.serialization.Hessian2Util;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.PaxosMemberConfiguration;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.Proposal;
import com.ofcoder.klein.consensus.paxos.rpc.vo.AcceptReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.AcceptRes;
import com.ofcoder.klein.consensus.paxos.rpc.vo.BaseReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.PrepareReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.PrepareRes;
import com.ofcoder.klein.rpc.facade.RpcContext;
import com.ofcoder.klein.storage.facade.Instance;
import com.ofcoder.klein.storage.facade.LogManager;
import com.ofcoder.klein.storage.facade.StorageEngine;

/**
 * @author far.liu
 */
public class Acceptor implements Lifecycle<ConsensusProp> {
    private static final Logger LOG = LoggerFactory.getLogger(Acceptor.class);

    private final PaxosNode self;
    private LogManager<Proposal> logManager;

    public Acceptor(PaxosNode self) {
        this.self = self;
    }

    @Override
    public void init(ConsensusProp op) {
        logManager = StorageEngine.<Proposal>getInstance().getLogManager();
    }

    @Override
    public void shutdown() {

    }

    public void handleAcceptRequest(AcceptReq req, RpcContext context) {
        LOG.info("processing the accept message from node-{}", req.getNodeId());

        self.setCurInstanceId(req.getInstanceId());

        try {
            logManager.getLock().writeLock().lock();

            final long selfProposalNo = self.getCurProposalNo();
            final PaxosMemberConfiguration memberConfiguration = self.getMemberConfiguration().createRef();
            Instance<Proposal> localInstance = logManager.getInstance(req.getInstanceId());
            if (localInstance == null) {
                localInstance = Instance.Builder.<Proposal>anInstance()
                        .instanceId(req.getInstanceId())
                        .proposalNo(req.getProposalNo())
                        .state(Instance.State.PREPARED)
                        .applied(new AtomicBoolean(false))
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
                        .proposalNo(selfProposalNo)
                        .instanceId(req.getInstanceId())
                        .instanceState(localInstance.getState())
                        .build();
                logManager.updateInstance(localInstance);
                context.response(ByteBuffer.wrap(Hessian2Util.serialize(res)));
                return;
            }

            AcceptRes.Builder resBuilder = AcceptRes.Builder.anAcceptRes()
                    .nodeId(self.getSelf().getId())
                    .instanceId(localInstance.getInstanceId())
                    .proposalNo(selfProposalNo);

            if (localInstance.getState() == Instance.State.CONFIRMED) {
                resBuilder.result(false)
                        .instanceState(localInstance.getState());
            } else {
                localInstance.setState(Instance.State.ACCEPTED);
                localInstance.setProposalNo(req.getProposalNo());
                localInstance.setGrantedValue(req.getData());
                logManager.updateInstance(localInstance);

                resBuilder.result(true)
                        .instanceState(localInstance.getState());
            }
            context.response(ByteBuffer.wrap(Hessian2Util.serialize(resBuilder.build())));
        } finally {
            logManager.getLock().writeLock().unlock();
        }
    }

    public PrepareRes handlePrepareRequest(PrepareReq req, boolean isSelf) {
        LOG.info("processing the prepare message from node-{}", req.getNodeId());
        final long curProposalNo = self.getCurProposalNo();
        final PaxosMemberConfiguration memberConfiguration = self.getMemberConfiguration().createRef();

        if (!checkPrepareReqValidity(memberConfiguration, curProposalNo, req, isSelf)) {
            PrepareRes res = PrepareRes.Builder.aPrepareRes()
                    .nodeId(self.getSelf().getId())
                    .result(false)
                    .proposalNo(curProposalNo)
                    .instances(new ArrayList<>()).build();
            return res;
        } else {
            List<Instance<Proposal>> instances = logManager.getInstanceNoConfirm();
            PrepareRes res = PrepareRes.Builder.aPrepareRes()
                    .nodeId(self.getSelf().getId())
                    .result(true)
                    .proposalNo(curProposalNo)
                    .instances(instances).build();
            return res;
        }
    }

    private boolean checkPrepareReqValidity(final PaxosMemberConfiguration paxosMemberConfiguration, final long selfProposalNo
            , final BaseReq req, final boolean isSelf) {
        boolean checkProposalNo = isSelf ? req.getProposalNo() < selfProposalNo : req.getProposalNo() <= selfProposalNo;
        if (!paxosMemberConfiguration.isValid(req.getNodeId())
                || req.getMemberConfigurationVersion() < paxosMemberConfiguration.getVersion()
                || checkProposalNo) {
            return false;
        }
        self.setCurProposalNo(req.getProposalNo());
        return true;
    }


    private boolean checkAcceptReqValidity(final PaxosMemberConfiguration paxosMemberConfiguration, final long selfProposalNo, BaseReq req) {
        if (!paxosMemberConfiguration.isValid(req.getNodeId())
                || req.getMemberConfigurationVersion() < paxosMemberConfiguration.getVersion()
                || req.getProposalNo() < selfProposalNo) {
            return false;
        }
        self.setCurProposalNo(req.getProposalNo());
        return true;
    }

}
