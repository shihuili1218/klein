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

import java.io.Serializable;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.serializer.hessian2.Hessian2Util;
import com.ofcoder.klein.consensus.facade.AbstractRpcProcessor;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.UniversalProposeProxy;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.ProposeProxy;
import com.ofcoder.klein.consensus.paxos.rpc.vo.RedirectReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.RedirectRes;
import com.ofcoder.klein.rpc.facade.RpcContext;

/**
 * Redirect Request Processor.
 *
 * @author 释慧利
 */
public class RedirectProcessor extends AbstractRpcProcessor<RedirectReq> {
    private static final Logger LOG = LoggerFactory.getLogger(RedirectProcessor.class);

    private ConsensusProp prop;
    private ProposeProxy directProposeProxy;

    public RedirectProcessor(final PaxosNode self, final ConsensusProp prop) {
        this.prop = prop;
        this.directProposeProxy = new UniversalProposeProxy(prop);
    }

    @Override
    public String service() {
        return RedirectReq.class.getSimpleName();
    }

    @Override
    public void handleRequest(final RedirectReq request, final RpcContext context) {
        LOG.info("receive redirect msg, redirect: {}", RedirectReq.fmtRedirect(request.getRedirect()));
        switch (request.getRedirect()) {
            case RedirectReq.TRANSACTION_REQUEST:
                Result<Serializable> proposeResult = directProposeProxy.propose(request.getProposal(), request.isApply());
                LOG.info("receive transfer request, apply: {}, result: {}", request.isApply(), proposeResult.getState());
                context.response(ByteBuffer.wrap(Hessian2Util.serialize(RedirectRes.Builder
                        .aRedirectResp()
                        .proposeResult(proposeResult)
                        .build()
                )));
                break;
            default:
                break;
        }
    }

}
