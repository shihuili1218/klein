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
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.consensus.facade.SM;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.member.Acceptor;
import com.ofcoder.klein.consensus.paxos.member.Learner;
import com.ofcoder.klein.consensus.paxos.member.Proposer;
import com.ofcoder.klein.spi.Join;

import java.nio.ByteBuffer;

/**
 * @author far.liu
 */
@Join
public class PaxosConsensus implements Consensus {
    private PaxosNode self;
    private Proposer proposer;
    private Acceptor acceptor;
    private Learner learner;

    @Override
    public Result propose(ByteBuffer data) {
        return null;
    }

    @Override
    public Result read(ByteBuffer data) {
        return null;
    }

    @Override
    public void loadSM(SM sm) {

    }

    @Override
    public void init(ConsensusProp op) {

    }

    @Override
    public void shutdown() {
        if (proposer != null) {
            proposer.shutdown();
        }
        if (acceptor != null) {
            acceptor.shutdown();
        }
        if (learner != null) {
            learner.shutdown();
        }
    }
}
