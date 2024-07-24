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

import java.util.List;

import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * Cluster info.
 *
 * @author 释慧利
 */
public interface Cluster {

    MemberConfiguration getMemberConfig();

    /**
     * Change member by Join-Consensus.
     * e.g. old version = 0, new version = 1
     * 1. Accept phase → send new config to old quorum, enter Join-Consensus
     * 2. Copy instance(version = 0, confirmed) to new quorum TODO
     * 3. Confirm phase → new config take effect
     *
     * @param add    endpoint to the cluster.
     * @param remove endpoint to the cluster.
     */
    boolean changeMember(List<Endpoint> add, List<Endpoint> remove);

}
