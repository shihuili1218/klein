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
package com.ofcoder.klein.common.util;

/**
 * TrueTime is used to solve time skew.
 *
 * @author 释慧利
 */
public final class TrueTime {
    private static final long HEARTBEAT_OFFSET = 0;
    private static long trueTimestamp = System.currentTimeMillis();
    private static long localTimestamp = trueTimestamp;

    /**
     * update time.
     *
     * @param trueTimestamp target time
     */
    public static void heartbeat(final long trueTimestamp) {
        long local = System.currentTimeMillis();
        if (trueTimestamp <= local) {
            return;
        }
        TrueTime.trueTimestamp = trueTimestamp + HEARTBEAT_OFFSET;
        TrueTime.localTimestamp = local;
    }

    /**
     * get current time.
     *
     * @return current time
     */
    public static long currentTimeMillis() {
        long local = System.currentTimeMillis();
        long diff = local - TrueTime.localTimestamp;
        return trueTimestamp + diff;
    }

}
