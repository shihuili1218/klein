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

import java.io.Serializable;

/**
 * Message.
 *
 * @author far.liu
 */
public class LockMessage implements Serializable {

    public static final byte LOCK = 0x01;
    public static final byte UNLOCK = 0x02;
    public static final long TTL_PERPETUITY = -1;

    private byte op;
    private String key;
    private long expire = TTL_PERPETUITY;

    public byte getOp() {
        return op;
    }

    public void setOp(final byte op) {
        this.op = op;
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public long getExpire() {
        return expire;
    }

    public void setExpire(final long expire) {
        this.expire = expire;
    }
}
