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
import com.ofcoder.klein.consensus.paxos.PaxosMemberConfiguration;

/**
 * @author 释慧利
 */
public class MasterSM extends AbstractSM {
    private static final Logger LOG = LoggerFactory.getLogger(MasterSM.class);
    public static final String GROUP = "master";

    public MasterSM() {

    }

    @Override
    protected Object apply(Object data) {
        LOG.info("MasterSM apply, {}", data.getClass().getSimpleName());
        if (data instanceof ElectionOp) {
            electMaster((ElectionOp) data);
        } else if (data instanceof ChangeMemberOp) {
            changeMember((ChangeMemberOp) data);
        } else {
            LOG.error("applying MasterSM, found unknown parameter types, data.type: {}", data.getClass().getSimpleName());
        }
        return null;
    }

    private void changeMember(ChangeMemberOp op) {
        switch (op.getOp()) {
            case ChangeMemberOp.ADD:
                MemberManager.writeOn(op.getTarget());
                break;
            case ChangeMemberOp.REMOVE:
                MemberManager.writeOff(op.getTarget());
                break;
            default:
                LOG.warn("MasterSM.changeMember, op[{}] is invalid", op.getOp());
                break;
        }
    }

    private void electMaster(ElectionOp op) {
        MemberManager.changeMaster(op.getNodeId());
    }

    @Override
    protected Object makeImage() {
        return MemberManager.createRef();
    }

    @Override
    protected void loadImage(Object snap) {
        if (!(snap instanceof PaxosMemberConfiguration)) {
            return;
        }
        PaxosMemberConfiguration snapConfiguration = (PaxosMemberConfiguration) snap;

        MemberManager.loadSnap(snapConfiguration);

        LOG.info("LOAD SNAP: {}", snapConfiguration);
    }
}
