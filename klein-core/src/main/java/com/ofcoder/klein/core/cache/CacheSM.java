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
package com.ofcoder.klein.core.cache;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.common.util.KleinThreadFactory;
import com.ofcoder.klein.consensus.facade.SM;

/**
 * @author 释慧利
 */
public class CacheSM implements SM {
    private static final Map<String, Object> CONTAINER = new ConcurrentHashMap<>();
    private static final Logger LOG = LoggerFactory.getLogger(CacheSM.class);
    private long checkPointInstanceId;
    private static ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(5, KleinThreadFactory.create("klein-cache-clear", true));

    private void clearExpire() {
        long now = System.currentTimeMillis();

    }

    @Override
    public <E extends Serializable> E apply(Object data) {
        LOG.info("apply data: {}", data);
        if (!(data instanceof Message)) {
            LOG.warn("apply data, UNKNOWN PARAMETER TYPE, data type is {}", data.getClass().getName());
            return null;
        }
        Message message = (Message) data;
        switch (message.getOp()) {
            case Message.PUT:
                CONTAINER.put(message.getKey(), message.getData());
                break;
            case Message.GET:
                return (E) CONTAINER.getOrDefault(message.getKey(), null);
            case Message.INVALIDATE:
                break;
            case Message.INVALIDATEALL:
                break;
            case Message.PUTIFPRESENT:
                break;
            case Message.EXIST:
                break;
            default:
                LOG.warn("apply data, UNKNOWN OPERATION, operation type is {}", message.getOp());
                break;
        }
        return null;
    }

    @Override
    public void makeImage() {

    }

    @Override
    public void loadImage() {

    }

}
