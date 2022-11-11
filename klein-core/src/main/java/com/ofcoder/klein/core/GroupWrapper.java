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
package com.ofcoder.klein.core;

import java.io.Serializable;

import com.ofcoder.klein.consensus.facade.Consensus;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.consensus.facade.sm.SM;
import com.ofcoder.klein.spi.ExtensionLoader;

/**
 * @author 释慧利
 */
public class GroupWrapper {
    private final String group;
    private final SM sm;
    private Consensus consensus;

    public GroupWrapper(final String group, final SM sm) {
        this.group = group;
        this.sm = sm;
        this.consensus = ExtensionLoader.getExtensionLoader(Consensus.class).getJoin();
        this.consensus.loadSM(this.group, this.sm);
    }

    public <E extends Serializable, D extends Serializable> Result<D> propose(E data) {
        return propose(data, false);
    }

    public <E extends Serializable, D extends Serializable> Result<D> propose(E data, boolean apply) {
        return this.consensus.propose(group, data, apply);
    }

    public <E extends Serializable, D extends Serializable> Result<D> read(E data) {
        return this.consensus.read(group, data);
    }
}
