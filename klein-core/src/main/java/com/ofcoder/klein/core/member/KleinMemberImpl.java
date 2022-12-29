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
package com.ofcoder.klein.core.member;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.consensus.facade.Consensus;
import com.ofcoder.klein.consensus.paxos.core.sm.ChangeMemberOp;
import com.ofcoder.klein.consensus.paxos.core.sm.MemberManager;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.spi.ExtensionLoader;

/**
 * @author 释慧利
 */
public class KleinMemberImpl implements KleinMember {
    private static final Logger LOG = LoggerFactory.getLogger(KleinMemberImpl.class);
    private Consensus consensus;

    public KleinMemberImpl() {
        this.consensus = ExtensionLoader.getExtensionLoader(Consensus.class).getJoin();
    }

    @Override
    public void addMember(final Endpoint endpoint) {
        if (MemberManager.createRef().isValid(endpoint.getId())) {
            return;
        }
        changeMember(ChangeMemberOp.ADD, endpoint);
    }

    @Override
    public void removeMember(final Endpoint endpoint) {
        if (!MemberManager.createRef().isValid(endpoint.getId())) {
            return;
        }
        changeMember(ChangeMemberOp.REMOVE, endpoint);
    }

    private void changeMember(final byte op, final Endpoint endpoint) {
        LOG.info("start add member.");


    }
}
