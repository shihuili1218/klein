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

import java.util.concurrent.TimeUnit;

import com.ofcoder.klein.common.util.TrueTime;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.core.GroupWrapper;

/**
 * lock implement.
 *
 * @author far.liu
 */
public class KleinLockImpl implements KleinLock {
    protected GroupWrapper consensus;
    private String key;

    public KleinLockImpl(final String group) {
        this.key = group;
        this.consensus = new GroupWrapper(group);
    }

    @Override
    public boolean acquire(final long ttl, final TimeUnit unit) {
        LockMessage message = new LockMessage();
        message.setKey(key);
        message.setOp(LockMessage.LOCK);
        message.setExpire(TrueTime.currentTimeMillis() + unit.toMillis(ttl));

        Result result = consensus.propose(message, true);
        return Result.State.SUCCESS.equals(result.getState()) && (Boolean) result.getData();
    }

    @Override
    public void unlock() {
        LockMessage message = new LockMessage();
        message.setKey(key);
        message.setOp(LockMessage.UNLOCK);
        Result result = consensus.propose(message, false);
    }
}
