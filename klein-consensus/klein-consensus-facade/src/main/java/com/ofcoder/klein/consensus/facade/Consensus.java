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
package com.ofcoder.klein.consensus.facade;

import java.io.Serializable;

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.spi.SPI;

/**
 * Consensus.
 *
 * @author 释慧利
 */
@SPI
public interface Consensus extends Cluster, Lifecycle<ConsensusProp> {

    /**
     * propose proposal.
     *
     * @param group group name
     * @param data  Client data, type is Serializable
     *              e.g. The input value of the state machine
     * @param apply Whether you need to wait until the state machine is applied
     *              If true, wait until the state machine is applied before returning
     * @param <D>   result type
     * @param <E>   request type
     * @return whether success
     */
    <E extends Serializable, D extends Serializable> Result<D> propose(String group, E data, boolean apply);

    /**
     * propose proposal.
     *
     * @param <E>   request type
     * @param group group name
     * @param data  Client data, type is Serializable
     *              e.g. The input value of the state machine
     * @return whether success
     * @see Consensus#propose(String, Serializable, boolean)
     */
    default <E extends Serializable> Result propose(final String group, final E data) {
        return propose(group, data, false);
    }

    /**
     * W + R ＞ N.
     * read from local.sm and only check self.lastApplyInstance if W = 1
     * read from W and only check self.lastApplyInstance if W ＞ 1
     *
     * @param group group name
     * @param data  message
     * @param <D>   result type
     * @param <E>   request type
     * @return whether success
     */
    default <E extends Serializable, D extends Serializable> Result<D> read(String group, E data) {
        return propose(group, data, true);
    }

    /**
     * set consensus listener.
     *
     * @param listener LifecycleListener
     */
    void setListener(LifecycleListener listener);

    interface LifecycleListener {

        /**
         * initialized, ready to initiate a proposal.
         */
        void prepared();
    }
}
