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
package com.ofcoder.klein.core.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.common.util.TrueTime;
import com.ofcoder.klein.consensus.facade.sm.AbstractSM;
import com.ofcoder.klein.core.cache.CacheSM;

/**
 * Lock State Machine.
 */
public class LockSM extends AbstractSM {
    public static final String GROUP = "cache";
    private static final byte UNLOCK = 0x00;
    private static final byte LOCKED = 0x01;

    private static final Logger LOG = LoggerFactory.getLogger(CacheSM.class);
    private Byte lock = UNLOCK;
    private long expire = 0;

    @Override
    protected Object apply(final Object data) {
        if (!(data instanceof LockMessage)) {
            LOG.warn("apply data, UNKNOWN PARAMETER TYPE, data type is {}", data.getClass().getName());
            return null;
        }
        LockMessage message = (LockMessage) data;
        switch (message.getOp()) {
            case LockMessage.LOCK:
                if (lock == UNLOCK || (expire != LockMessage.TTL_PERPETUITY && expire < TrueTime.currentTimeMillis())) {
                    lock = LOCKED;
                    expire = message.getExpire();
                    return true;
                } else {
                    return false;
                }
            case LockMessage.UNLOCK:
                lock = UNLOCK;
                expire = 0;
                break;
            default:
                break;
        }
        return null;
    }

    @Override
    protected Object makeImage() {
        return lock;
    }

    @Override
    protected void loadImage(final Object snap) {
        if (!(snap instanceof Byte)) {
            return;
        }
        lock = (Byte) snap;
    }
}
