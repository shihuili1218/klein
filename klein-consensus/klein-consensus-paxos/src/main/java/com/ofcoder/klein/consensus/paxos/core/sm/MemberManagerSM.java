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
package com.ofcoder.klein.consensus.paxos.core.sm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.consensus.facade.sm.AbstractSM;

/**
 * Member Manager SM.
 *
 * @author 释慧利
 */
public class MemberManagerSM extends AbstractSM {
    public static final String GROUP = "member_manager";
    private static final Logger LOG = LoggerFactory.getLogger(MemberManagerSM.class);
    private final PaxosMemberConfiguration memberConfig;

    public MemberManagerSM() {
        this.memberConfig = MemberRegistry.getInstance().getMemberConfiguration();
    }

    @Override
    public Object apply(final Object data) {
        LOG.debug("MemberManagerSM apply, {}", data.getClass().getSimpleName());
        if (data instanceof ChangeMemberOp) {
            ChangeMemberOp op = (ChangeMemberOp) data;
            if (op.getPhase() == ChangeMemberOp.FIRST_PHASE) {
                memberConfig.seenNewConfig(op.getNewConfig());
            } else if (op.getPhase() == ChangeMemberOp.SECOND_PHASE) {
                memberConfig.commitNewConfig(op.getNewConfig());
            }
            // else ignore.
        } else {
            LOG.error("MemberManagerSM, found unknown parameter types, data.type: {}", data.getClass().getSimpleName());
        }
        return null;
    }

    @Override
    public Object makeImage() {
        return MemberRegistry.getInstance().getMemberConfiguration().createRef();
    }

    @Override
    public void loadImage(final Object snap) {
        LOG.info("LOAD SNAP: {}", snap);
        if (!(snap instanceof PaxosMemberConfiguration)) {
            return;
        }
        MemberRegistry.getInstance().loadSnap((PaxosMemberConfiguration) snap);
    }
}
