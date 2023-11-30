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

import java.io.Serializable;
import java.util.List;

/**
 * Instance information.
 *
 * @author 释慧利
 */
public class Instance<D extends Serializable> implements Serializable {

    private long instanceId;
    private long proposalNo;
    private List<D> grantedValue;
    private String checksum;
    private State state = State.PREPARED;

    /**
     * get instance id.
     *
     * @return instance id
     */
    public long getInstanceId() {
        return instanceId;
    }

    /**
     * set instance id.
     *
     * @param instanceId instance id
     */
    public void setInstanceId(final long instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * get instance’s proposalNo.
     *
     * @return proposalNo
     */
    public long getProposalNo() {
        return proposalNo;
    }

    /**
     * set instance’s proposalNo.
     *
     * @param proposalNo proposalNo
     */
    public void setProposalNo(final long proposalNo) {
        this.proposalNo = proposalNo;
    }

    /**
     * get granted value.
     *
     * @return granted value
     */
    public List<D> getGrantedValue() {
        return grantedValue;
    }

    /**
     * set granted value.
     *
     * @param grantedValue granted value
     */
    public void setGrantedValue(final List<D> grantedValue) {
        this.grantedValue = grantedValue;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(final String checksum) {
        this.checksum = checksum;
    }

    /**
     * get instance's state.
     *
     * @return instance's state.
     */
    public State getState() {
        return state;
    }

    /**
     * set instance's state.
     *
     * @param state instance's state.
     */
    public void setState(final State state) {
        this.state = state;
    }

    public enum State {
        PREPARED, ACCEPTED, CONFIRMED;
    }

    public static final class Builder<B extends Serializable> {
        private long instanceId;
        private long proposalNo;
        private List<B> grantedValue;
        private String checksum;
        private State state;

        private Builder() {
        }

        /**
         * anInstance.
         *
         * @param <B> Instance data type
         * @return Builder
         */
        public static <B extends Serializable> Builder<B> anInstance() {
            return new Builder<>();
        }

        /**
         * instanceId.
         *
         * @param instanceId instanceId
         * @return Builder
         */
        public Builder<B> instanceId(final long instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        /**
         * proposalNo.
         *
         * @param proposalNo proposalNo
         * @return Builder
         */
        public Builder<B> proposalNo(final long proposalNo) {
            this.proposalNo = proposalNo;
            return this;
        }

        /**
         * grantedValue.
         *
         * @param grantedValue grantedValue
         * @return Builder
         */
        public Builder<B> grantedValue(final List<B> grantedValue) {
            this.grantedValue = grantedValue;
            return this;
        }

        /**
         * checksum.
         *
         * @param checksum checksum
         * @return Builder
         */
        public Builder<B> checksum(final String checksum) {
            this.checksum = checksum;
            return this;
        }

        /**
         * state.
         *
         * @param state state
         * @return Builder
         */
        public Builder<B> state(final State state) {
            this.state = state;
            return this;
        }

        /**
         * build.
         *
         * @return Instance
         */
        public Instance<B> build() {
            Instance<B> instance = new Instance<>();
            instance.setInstanceId(instanceId);
            instance.setProposalNo(proposalNo);
            instance.setGrantedValue(grantedValue);
            instance.setState(state);
            instance.setChecksum(checksum);
            return instance;
        }

    }

}
