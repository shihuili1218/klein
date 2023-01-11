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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * Quorum.
 *
 * @author 释慧利
 */
public class SingleQuorum implements Quorum {
    private final Set<Endpoint> allMembers;
    private final Set<Endpoint> grantedMembers = Collections.synchronizedSet(new HashSet<>());
    private final Set<Endpoint> failedMembers = Collections.synchronizedSet(new HashSet<>());
    private final int successThreshold;
    private final int failureThreshold;

    public SingleQuorum(final Set<Endpoint> allMembers) {
        this.allMembers = new HashSet<>(allMembers);
        int n = allMembers.size();
        this.successThreshold = n / 2 + 1;
        this.failureThreshold = n - successThreshold;
    }

    @Override
    public GrantResult isGranted() {
        if (grantedMembers.size() >= successThreshold) {
            return GrantResult.PASS;
        } else if (failedMembers.size() > failureThreshold) {
            return GrantResult.REFUSE;
        } else {
            return GrantResult.GRANTING;
        }
    }

    @Override
    public boolean grant(final Endpoint node) {
        if (!allMembers.contains(node)) {
            return false;
        }
        return grantedMembers.add(node);
    }

    @Override
    public boolean refuse(final Endpoint node) {
        if (!allMembers.contains(node)) {
            return false;
        }
        return failedMembers.add(node);
    }

}
