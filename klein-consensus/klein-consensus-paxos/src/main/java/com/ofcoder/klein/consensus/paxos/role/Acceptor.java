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
package com.ofcoder.klein.consensus.paxos.role;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.rpc.vo.PrepareReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.PrepareRes;
import com.ofcoder.klein.rpc.facade.RpcContext;
import com.ofcoder.klein.rpc.facade.serialization.Hessian2Util;
import com.ofcoder.klein.storage.facade.Instance;
import com.ofcoder.klein.storage.facade.LogManager;
import com.ofcoder.klein.storage.facade.StorageEngine;

/**
 * @author far.liu
 */
public class Acceptor implements Lifecycle<ConsensusProp> {
    private static final Logger LOG = LoggerFactory.getLogger(Acceptor.class);

    private final PaxosNode self;
    private LogManager logManager;

    public Acceptor(PaxosNode self) {
        this.self = self;
    }

    @Override
    public void init(ConsensusProp op) {
        logManager = StorageEngine.getLogManager();
    }

    @Override
    public void shutdown() {

    }


    public void handlePrepareRequest(PrepareReq req, RpcContext context) {
        LOG.info("处理prepare消息，{}", self.getSelf().getId());

        try {
            logManager.getLock().writeLock().lock();

            PrepareRes.Builder resBuilder = PrepareRes.Builder.aPrepareRes()
                    .nodeId(self.getSelf().getId());

            if (req.getProposalNo() > self.getCurProposalNo()) {
                resBuilder.result(true);
                resBuilder.proposalNo(req.getProposalNo());
            } else {
                resBuilder.result(false);
                resBuilder.proposalNo(self.getCurProposalNo());
            }

            Instance localInstance = logManager.getInstance(req.getInstanceId());
            if (localInstance == null) {
                localInstance = Instance.Builder.anInstance()
                        .grantedProposalNo(req.getProposalNo())
                        .instanceId(req.getInstanceId())
                        .state(Instance.State.PREPARED)
                        .build();
                logManager.updateInstance(localInstance);
                resBuilder.grantValue(null);
                resBuilder.state(Instance.State.PREPARED);
            } else {
                resBuilder.grantValue(localInstance.getGrantedValue());
                resBuilder.state(localInstance.getState());
            }
            context.response(ByteBuffer.wrap(Hessian2Util.serialize(resBuilder.build())));
        } finally {
            logManager.getLock().writeLock().unlock();
        }

    }

    private void grantPrepare(PrepareReq req, RpcContext context) {

    }

}
