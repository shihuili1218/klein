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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.common.serialization.Hessian2Util;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
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

        long diffId = req.getInstanceId() - self.getCurInstanceId();
        if (diffId > 0) {
            self.addInstanceId(diffId);
        }

        try {
            logManager.getLock().writeLock().lock();

            final long selfProposalNo = self.getCurProposalNo();
            Instance<Proposal> localInstance = logManager.getInstance(req.getInstanceId());
            if (localInstance == null) {
                localInstance = Instance.Builder.<Proposal>anInstance()
                        .instanceId(req.getInstanceId())
                        .proposalNo(req.getProposalNo())
                        .state(Instance.State.PREPARED)
                        .applied(new AtomicBoolean(false))
                        .build();
            }

            if (!checkAcceptReqValidity(req)) {
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

    public void handlePrepareRequest(PrepareReq req, RpcContext context) {
        LOG.info("processing the prepare message from node-{}", req.getNodeId());
        try {
            logManager.getLock().writeLock().lock();
            if (!checkPrepareReqValidity(req)) {
                PrepareRes res = PrepareRes.Builder.aPrepareRes()
                        .nodeId(self.getSelf().getId())
                        .result(false)
                        .proposalNo(self.getCurProposalNo())
                        .instances(null).build();
                context.response(ByteBuffer.wrap(Hessian2Util.serialize(res)));
            } else {
                List<Instance<Proposal>> instances = logManager.getInstanceNoConfirm();
                PrepareRes res = PrepareRes.Builder.aPrepareRes()
                        .nodeId(self.getSelf().getId())
                        .result(true)
                        .proposalNo(self.getCurProposalNo())
                        .instances(instances).build();

                context.response(ByteBuffer.wrap(Hessian2Util.serialize(res)));
            }
        } finally {
            logManager.getLock().writeLock().unlock();
        }
    }

    private boolean checkPrepareReqValidity(BaseReq req) {
        long selfProposalNo = self.getCurProposalNo();
        if (!self.getMemberConfiguration().isValid(req.getNodeId())
                || req.getMemberConfigurationVersion() < self.getMemberConfiguration().getVersion()
                || req.getProposalNo() <= selfProposalNo) {
            return false;
        }
        self.setCurProposalNo(req.getProposalNo());
        return true;
    }


    private boolean checkAcceptReqValidity(BaseReq req) {
        long selfProposalNo = self.getCurProposalNo();
        if (!self.getMemberConfiguration().isValid(req.getNodeId())
                || req.getMemberConfigurationVersion() < self.getMemberConfiguration().getVersion()
                || req.getProposalNo() < selfProposalNo) {
            return false;
        }
        if (req.getProposalNo() > selfProposalNo) {
            self.setCurProposalNo(req.getProposalNo());
        }
        return true;
    }

}
