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

import com.ofcoder.klein.consensus.facade.Result;

/**
 * @author 释慧利
 */
public interface ProposeDone {
    /**
     * call the method when negotiation done
     * @param result negotiation result
     */
    void negotiationDone(Result.State result);

    /**
     * This method may not be called because the agreed proposal is uncontrollable.
     * @param input  Enter the value of the state machine
     * @param output Value of state machine output
     */
    default void applyDone(Object input, Object output) {
        // for subclass
    }
}
