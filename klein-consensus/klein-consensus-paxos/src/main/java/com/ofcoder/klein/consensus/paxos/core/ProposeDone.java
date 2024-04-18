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

import java.util.Map;

import com.ofcoder.klein.consensus.facade.Command;

/**
 * propose callback.
 *
 * @author 释慧利
 */
public interface ProposeDone {
    /**
     * call the method when negotiation done.
     * the method is executed no matter what proposal is reached a consensus
     *
     * @param result     negotiation result
     * @param dataChange the consensus data has changed
     */
    void negotiationDone(boolean result, boolean dataChange);

    /**
     * call the method when apply done.
     * NOTICE: The proposal that reaches consensus may not be the expected proposal
     *
     * @param result proposal to reach consensus and corresponding state transition result,
     *               the key is enter the value of the state machine
     *               the value is state machine output
     */
    default void applyDone(Map<Command, Object> result) {
        // for subclass
    }

    class FakeProposeDone implements ProposeDone {
        @Override
        public void negotiationDone(final boolean result, final boolean dataChange) {
            // do nothing.
        }
    }
}
