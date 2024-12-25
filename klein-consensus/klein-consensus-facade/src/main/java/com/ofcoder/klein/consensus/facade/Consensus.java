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

import com.ofcoder.klein.consensus.facade.sm.SM;
import com.ofcoder.klein.spi.SPI;
import java.io.Serializable;

/**
 * Consensus.
 *
 * @author 释慧利
 */
@SPI
public interface Consensus extends Cluster {

    /**
     * load state machine.
     *
     * @param group group name
     * @param sm    state machine
     */
    void loadSM(String group, SM sm);

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
    <E extends Serializable, D extends Serializable> Result<D> propose(String group, byte[] data, boolean apply);

    /**
     * Obtain the consensus reached instanceId.
     * todo: propose apply=true, 需要调用readIndex判断自己是否执行了对应的instanceId，再返回本地数据
     *
     * @param group group name
     * @return instance id
     */
    Result<Long> readIndex(String group);

    /**
     * preheating.
     */
    void preheating();

    /**
     * Bean shutdown.
     */
    void shutdown();
}
