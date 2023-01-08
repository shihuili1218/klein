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
package com.ofcoder.klein.consensus.paxos.rpc;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.common.serialization.Hessian2Util;
import com.ofcoder.klein.consensus.facade.AbstractRpcProcessor;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.Proposal;
import com.ofcoder.klein.consensus.paxos.core.RoleAccessor;
import com.ofcoder.klein.consensus.paxos.rpc.vo.PushCompleteDataReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.PushCompleteDataRes;
import com.ofcoder.klein.rpc.facade.RpcContext;
import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.storage.facade.Instance;
import com.ofcoder.klein.storage.facade.LogManager;

/**
 * PushCompleteDataProcessor.
 *
 * @author 释慧利
 */
public class PushCompleteDataProcessor extends AbstractRpcProcessor<PushCompleteDataReq> {
    private static final Logger LOG = LoggerFactory.getLogger(PrepareProcessor.class);
    private final PaxosNode self;
    private LogManager<Proposal> logManager;

    public PushCompleteDataProcessor(final PaxosNode self) {
        this.self = self;
        logManager = ExtensionLoader.getExtensionLoader(LogManager.class).getJoin();
    }

    @Override
    public void handleRequest(final PushCompleteDataReq request, final RpcContext context) {
        LOG.info("receive push complete data");
        PushCompleteDataRes res = new PushCompleteDataRes();

        RoleAccessor.getLearner().loadSnap(request.getSnaps());
        logManager.getLock().writeLock().lock();
        try {
            for (Instance<Proposal> instance : request.getConfirmedInstances()) {
                Instance<Proposal> localInstance = logManager.getInstance(instance.getInstanceId());
                if (localInstance != null && localInstance.getState() == Instance.State.CONFIRMED) {
                    continue;
                }
                logManager.updateInstance(instance);
            }
            res.setSuccess(true);
        } finally {
            logManager.getLock().writeLock().unlock();
        }

        context.response(ByteBuffer.wrap(Hessian2Util.serialize(res)));
    }

    @Override
    public String service() {
        return PushCompleteDataReq.class.getSimpleName();
    }
}
