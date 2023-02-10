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
package com.ofcoder.klein.consensus.facade.quorum;

import java.util.Set;

import com.ofcoder.klein.common.OnlyForTest;
import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * Join Consensus Quorum.
 *
 * @author 释慧利
 */
public final class JoinConsensusQuorum implements Quorum {
    private final Quorum oldQuorum;
    private final Quorum newQuorum;

    public JoinConsensusQuorum(final Set<Endpoint> effectMembers, final Set<Endpoint> lastMembers, final int effectThreshold, final int lastThreshold) {
        oldQuorum = new SingleQuorum(effectMembers, effectThreshold);
        newQuorum = new SingleQuorum(lastMembers, lastThreshold);
    }

    @Override
    public boolean refuse(final Endpoint node) {
        oldQuorum.refuse(node);
        newQuorum.refuse(node);
        return true;
    }

    @Override
    public boolean grant(final Endpoint node) {
        oldQuorum.grant(node);
        newQuorum.grant(node);
        return true;
    }

    @Override
    public GrantResult isGranted() {
        if (oldQuorum.isGranted() == GrantResult.REFUSE || newQuorum.isGranted() == GrantResult.REFUSE) {
            return GrantResult.REFUSE;
        }
        if (oldQuorum.isGranted() == GrantResult.PASS && newQuorum.isGranted() == GrantResult.PASS) {
            return GrantResult.PASS;
        }
        return GrantResult.GRANTING;
    }

    @OnlyForTest
    public Quorum getOldQuorum() {
        return oldQuorum;
    }

    @OnlyForTest
    public Quorum getNewQuorum() {
        return newQuorum;
    }
}
