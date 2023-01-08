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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.common.serialization.Hessian2Util;
import com.ofcoder.klein.consensus.facade.AbstractRpcProcessor;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.core.Master;
import com.ofcoder.klein.consensus.paxos.core.RoleAccessor;
import com.ofcoder.klein.consensus.paxos.core.sm.MemberRegistry;
import com.ofcoder.klein.consensus.paxos.rpc.vo.RedirectReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.RedirectRes;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.RpcContext;

/**
 * Redirect Request Processor.
 *
 * @author 释慧利
 */
public class RedirectProcessor extends AbstractRpcProcessor<RedirectReq> {
    private static final Logger LOG = LoggerFactory.getLogger(RedirectProcessor.class);

    private final PaxosNode self;
    private ConsensusProp prop;
    private long takeMasterTimeout;

    public RedirectProcessor(final PaxosNode self, final ConsensusProp prop) {
        this.prop = prop;
        this.self = self;
        this.takeMasterTimeout = this.prop.getRoundTimeout() * this.prop.getRetry() + this.prop.getPaxosProp().getMasterElectMaxInterval();
    }

    @Override
    public String service() {
        return RedirectReq.class.getSimpleName();
    }

    private boolean takeMaster() {
        RoleAccessor.getMaster().electingMaster();
        CountDownLatch latch = new CountDownLatch(1);
        RoleAccessor.getMaster().addHealthyListener(healthy -> {
            if (Master.ElectState.allowBoost(healthy)) {
                latch.countDown();
            }
        });
        try {
            return latch.await(takeMasterTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOG.warn(e.getMessage());
            return false;
        }
    }

    @Override
    public void handleRequest(final RedirectReq request, final RpcContext context) {
        LOG.info("receive redirect msg, redirect: {}, changeOp: {}, changeTarget: {}", request.getRedirect(), request.getChangeOp(), request.getChangeTarget());
        switch (request.getRedirect()) {
            case RedirectReq.CHANGE_MEMBER:
                Endpoint master = MemberRegistry.getInstance().getMemberConfiguration().getMaster();
                if (request.getChangeOp() == Master.REMOVE && request.getChangeTarget().contains(master)
                        && !takeMaster()) {
                    LOG.warn("remove member, taking the master failure.");
                    context.response(ByteBuffer.wrap(Hessian2Util.serialize(RedirectRes.Builder.aRedirectResp().changeResult(false).build())));
                    return;
                }
                boolean result = RoleAccessor.getMaster().changeMember(request.getChangeOp(), request.getChangeTarget());
                context.response(ByteBuffer.wrap(Hessian2Util.serialize(RedirectRes.Builder.aRedirectResp().changeResult(result).build())));
                break;
            case RedirectReq.TRANSACTION_REQUEST:
                break;
            default:
                break;
        }
    }

}
