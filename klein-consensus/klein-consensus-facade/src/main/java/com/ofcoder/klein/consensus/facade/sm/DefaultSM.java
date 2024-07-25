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

package com.ofcoder.klein.consensus.facade.sm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Add Snapshot and Checkpoint in SM.
 *
 * @author 释慧利
 */
public class DefaultSM<T> implements SM {
    private final String key;
    private final T target;

    public DefaultSM(final String key, final T target) {
        this.key = key;
        this.target = target;
    }

    /**
     * 执行target中的具体方法.
     *
     * @param data proposal's data
     * @return
     */
    @Override
    public Object apply(final Object data) {
        Object[] message = (Object[]) data;
        int messageLength = message.length;
        // key
//        String key = (String) message[messageLength - 1];
        // op
        String methodName = (String) message[messageLength - 2];

        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target, Arrays.copyOfRange(message, 0, messageLength - 2));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object makeImage() {
        try {
            return this.clone();
        } catch (CloneNotSupportedException e) {
            // todo
            throw new RuntimeException(e);
        }
    }

    @Override
    public void loadImage(final Object snap) {
        // todo copy snap properties to this
    }

    @Override
    public final String getGroup() {
        return target.getClass().getName() + "_" + key;
    }

    @Override
    public void close() {

    }
}

