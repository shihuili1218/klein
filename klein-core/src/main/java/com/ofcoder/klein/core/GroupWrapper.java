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
package com.ofcoder.klein.core;

import com.ofcoder.klein.serializer.hessian2.Hessian2Util;
import java.io.Serializable;

import com.ofcoder.klein.consensus.facade.Consensus;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.spi.ExtensionLoader;

/**
 * Wrapper group and sm.
 *
 * @author 释慧利
 */
public class GroupWrapper {
    private final String group;
    private final Consensus consensus;

    public GroupWrapper(final String group) {
        this.group = group;
        this.consensus = ExtensionLoader.getExtensionLoader(Consensus.class).getJoin();
    }

    /**
     * propose proposal.
     *
     * @param data Client data, type is Serializable
     *             e.g. The input value of the state machine
     * @param <D>  result type
     * @param <E>  request type
     * @return whether success
     */
    public <E extends Serializable, D extends Serializable> Result<D> propose(final E data) {
        return propose(data, false);
    }

    /**
     * propose proposal.
     *
     * @param data  Client data, type is Serializable
     *              e.g. The input value of the state machine
     * @param apply Whether you need to wait until the state machine is applied
     *              If true, wait until the state machine is applied before returning
     * @param <D>   result type
     * @param <E>   request type
     * @return whether success
     */
    public <E extends Serializable, D extends Serializable> Result<D> propose(final E data, final boolean apply) {
        return this.consensus.propose(group, Hessian2Util.serialize(data), apply);
    }

    /**
     * propose proposal.
     *
     * @param data  Client data, type is Serializable
     *              e.g. The input value of the state machine
     * @param apply Whether you need to wait until the state machine is applied
     *              If true, wait until the state machine is applied before returning
     * @param <D>   result type
     * @return whether success
     */
    private <D extends Serializable> Result<D> propose(final byte[] data, final boolean apply) {
        return this.consensus.propose(group, Hessian2Util.serialize(data), apply);
    }

    /**
     * propose proposal.
     *
     * @param data Client data, type is Serializable
     *             e.g. The input value of the state machine
     * @param <D>  result type
     * @param <E>  request type
     * @return whether success
     */
    public <E extends Serializable, D extends Serializable> Result<D> read(final E data) {
        return propose(data, true);
    }
}
