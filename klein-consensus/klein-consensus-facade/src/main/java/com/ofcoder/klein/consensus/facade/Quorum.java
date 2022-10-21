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

import java.util.HashSet;
import java.util.Set;

/**
 * @author: 释慧利
 */
public abstract class Quorum {
    private Set<Node> allMembers = new HashSet<>();
    private Set<Node> grantedMembers = new HashSet<>();
    private int threshold;

    public Quorum(final Set<Node> allMembers) {
        this.allMembers = allMembers;
        this.threshold = allMembers.size() / 2 + 1;
    }

    public boolean isGrant() {
        return grantedMembers.size() >= threshold;
    }

    public boolean grant(Node node) {
        return grantedMembers.add(node);
    }
}
