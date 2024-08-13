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

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.consensus.facade.sm.AbstractSM;

/**
 * master sm.
 *
 * @author 释慧利
 */
public class MasterSM extends AbstractSM {
    public static final String GROUP = "master";
    private static final Logger LOG = LoggerFactory.getLogger(MasterSM.class);
    private final PaxosMemberConfiguration memberConfig;

    public MasterSM() {
        this.memberConfig = MemberRegistry.getInstance().getMemberConfiguration();
    }

    @Override
    public Object apply(final Object data) {
        LOG.info("MasterSM apply, {}", data.getClass().getSimpleName());
        if (data instanceof ElectionOp) {
            electMaster((ElectionOp) data);
        } else {
            throw new IllegalArgumentException("Unknown data type: " + data.getClass().getSimpleName());
        }
        return null;
    }

    private void electMaster(final ElectionOp op) {
        // do nothing.
    }

    @Override
    public Object makeImage() {
        return new Image();
    }

    @Override
    public void loadImage(final Object snap) {
        // do nothing.
    }

    /**
     * Fake image.
     */
    public static class Image implements Serializable { }
}