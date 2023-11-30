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

/**
 * generate proposal no by config.version and local.counter.
 *
 * @author 释慧利
 */
public class ProposalNoUtil {

    public static long getEpochFromPno(final long pno) {
        return pno >> 32L;
    }

    public static long getCounterFromPno(final long pno) {
        return pno & 0xffffffffL;
    }

    /**
     * generate proposal no by config.version and local.counter.
     *
     * @param epoch   config.version
     * @param counter local.counter
     * @return proposal no
     */
    public static long makePno(final long epoch, final long counter) {
        return (epoch << 32L) | (counter & 0xffffffffL);
    }

    /**
     * pno convert to Hex.
     *
     * @param pno pno
     * @return hex string
     */
    public static String pnoToString(final long pno) {
        return Long.toHexString(pno);
    }

}
