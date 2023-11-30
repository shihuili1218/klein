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

import com.ofcoder.klein.storage.facade.Snap;

/**
 * State machine.
 *
 * @author 释慧利
 */
public interface SM {

    /**
     * last applied instance id.
     *
     * @return instance id
     */
    long lastAppliedId();

    /**
     * apply instance.
     *
     * @param instanceId instance id
     * @param data       proposal's data
     * @return apply result
     */
    Object apply(long instanceId, Object data);

    /**
     * make a snapshot.
     *
     * @return snapshot
     */
    Snap snapshot();

    /**
     * load snapshot.
     *
     * @param snap snapshot
     */
    void loadSnap(Snap snap);

    /**
     * close sm.
     */
    void close();

}
