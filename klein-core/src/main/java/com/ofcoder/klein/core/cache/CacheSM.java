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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.ofcoder.klein.consensus.facade.sm.AbstractSM;

/**
 * @author 释慧利
 */
public class CacheSM extends AbstractSM {
    public static final String GROUP = "cache";

    private static final LRUMap CONTAINER = new LRUMap(3);
    private static final Logger LOG = LoggerFactory.getLogger(CacheSM.class);

    @Override
    public Object apply(Object data) {
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
                return CONTAINER.get(message.getKey());
            case Message.INVALIDATE:
                CONTAINER.remove(message.getKey());
                break;
            case Message.INVALIDATEALL:
                CONTAINER.clear();
                break;
            case Message.PUTIFPRESENT:
                return CONTAINER.putIfAbsent(message.getKey(), message.getData());
            case Message.EXIST:
                return CONTAINER.exist(message.getKey());
            default:
                LOG.warn("apply data, UNKNOWN OPERATION, operation type is {}", message.getOp());
                break;
        }
        return null;
    }

    @Override
    public Object makeImage() {
        return CONTAINER.makeImage();
    }

    @Override
    public void loadImage(Object snap) {
        CONTAINER.clear();
        CONTAINER.loadImage(snap);
    }

}
