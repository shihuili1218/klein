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
package com.ofcoder.klein.storage.facade;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * @author 释慧利
 */
public class Instance {

    private long instanceId;
    private List<Object> grantedValue;
    private State state = State.PREPARED;

    public long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(long instanceId) {
        this.instanceId = instanceId;
    }

    public List<Object> getGrantedValue() {
        return grantedValue;
    }

    public void setGrantedValue(List<Object> grantedValue) {
        this.grantedValue = grantedValue;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public static enum State{
        PREPARED, ACCEPTED, CONFIRMED;
    }

    public static final class Builder {
        private long instanceId;
        private List<Object> grantedValue;
        private State state;

        private Builder() {
        }

        public static Builder anInstance() {
            return new Builder();
        }

        public Builder instanceId(long instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public Builder grantedValue(List<Object> grantedValue) {
            this.grantedValue = grantedValue;
            return this;
        }

        public Builder state(State state) {
            this.state = state;
            return this;
        }

        public Instance build() {
            Instance instance = new Instance();
            instance.setInstanceId(instanceId);
            instance.setGrantedValue(grantedValue);
            instance.setState(state);
            return instance;
        }
    }
}
