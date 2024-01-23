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

import com.ofcoder.klein.common.Role;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.rpc.vo.AcceptReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.AcceptRes;
import com.ofcoder.klein.consensus.paxos.rpc.vo.PrepareReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.PrepareRes;

/**
 * Acceptor Role.
 *
 * @author 释慧利
 */
public interface Acceptor extends Role<ConsensusProp> {
    /**
     * Process the Accept message from Proposer.
     *
     * @param req request content
     * @param isSelf from self
     * @return response
     */
    AcceptRes handleAcceptRequest(AcceptReq req, boolean isSelf);

    /**
     * Process the Prepare message from Proposer.
     *
     * @param req request content
     * @param isSelf from self
     * @return response
     */
    PrepareRes handlePrepareRequest(PrepareReq req, boolean isSelf);
}
