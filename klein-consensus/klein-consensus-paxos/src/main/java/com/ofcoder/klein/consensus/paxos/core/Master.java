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
package com.ofcoder.klein.consensus.paxos.core;

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.rpc.vo.Ping;
import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * @author 释慧利
 */
public interface Master extends Lifecycle<ConsensusProp> {
    // 成员变更后，需要协商一个NOOP提案，
    void addMember(Endpoint endpoint);

    void removeMember(Endpoint endpoint);

    // todo master要拥有最完整的数据，晋升后需要立即推进未执行状态转移的instance，以保证成员变更的正确性
    void electingMaster();

    public boolean onReceiveHeartbeat(final Ping request, boolean isSelf);

    void onChangeMaster(final String newMaster);
}
