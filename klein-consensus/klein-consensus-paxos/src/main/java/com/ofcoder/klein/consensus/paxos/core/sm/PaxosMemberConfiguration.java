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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.ofcoder.klein.consensus.facade.MemberConfiguration;
import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * paxos member configuration.
 *
 * @author 释慧利
 */
public class PaxosMemberConfiguration extends MemberConfiguration {
    public static final String RESET_MASTER_ID = "BYE";
    private static final Logger LOG = LoggerFactory.getLogger(PaxosMemberConfiguration.class);
    private transient volatile Endpoint master;
    private final transient List<HealthyListener> listeners = new ArrayList<>();
    private transient ElectState masterState = ElectState.ELECTING;

    public boolean isSelf() {
        Endpoint endpoint = getMaster();
        return endpoint != null && endpoint.equals(self);
    }

    public Endpoint getMaster() {
        return this.master;
    }

    /**
     * change master.
     *
     * @param nodeId new master id
     */
    public void changeMaster(final String nodeId) {
        if (StringUtils.equals(RESET_MASTER_ID, nodeId)) {
            this.master = null;
            this.masterState = ElectState.ELECTING;
            this.listeners.forEach(it -> it.change(this.masterState));
        } else if (isValid(nodeId)) {
            this.master = getEndpointById(nodeId);
            this.masterState = isSelf() ? ElectState.LEADING : ElectState.FOLLOWING;
            this.listeners.forEach(it -> it.change(this.masterState));
            LOG.info("node-{} was promoted to master, version: {}", nodeId, this.version.get());
        }
    }

    /**
     * only in BOOSTING state can boost instance.
     *
     * @return if true, can boost instance
     */
    public boolean allowBoost() {
        return ElectState.allowBoost(getMasterState());
    }

    /**
     * In FOLLOWER/BOOSTING state can propose.
     *
     * @return if true, can propose
     */
    public boolean allowPropose() {
        return ElectState.allowPropose(getMasterState());
    }

    @Override
    public void init(final Endpoint self, final List<Endpoint> nodes) {
        super.init(self, nodes);
    }

    /**
     * load snapshot.
     *
     * @param snap snapshot
     */
    protected void loadSnap(final PaxosMemberConfiguration snap) {
        this.version = new AtomicInteger(snap.version.get());
        this.effectMembers.clear();
        this.effectMembers.putAll(snap.effectMembers);
        this.lastMembers.clear();
        this.lastMembers.putAll(snap.lastMembers);
    }

    /**
     * Create an object with the same data.
     *
     * @return new object
     */
    public PaxosMemberConfiguration createRef() {
        PaxosMemberConfiguration target = new PaxosMemberConfiguration();
        target.effectMembers.putAll(effectMembers);
        target.lastMembers.putAll(lastMembers);
        target.listeners.addAll(listeners);
        target.masterState = masterState;
        if (master != null) {
            target.master = new Endpoint(master.getId(), master.getIp(), master.getPort(), master.isOutsider());
        }
        target.version = new AtomicInteger(version.get());
        return target;
    }

    @Override
    public String toString() {
        return "PaxosMemberConfiguration{"
                + "master=" + master
                + ", masterState=" + masterState
                + ", version=" + version
                + ", effectMembers=" + effectMembers
                + ", lastMembers=" + lastMembers
                + ", self=" + self
                + "} " + super.toString();
    }

    /**
     * Added master health listener.
     *
     * @param listener listener
     */
    public void addHealthyListener(final HealthyListener listener) {
        listener.change(getMasterState());
        listeners.add(listener);
    }

    /**
     * get elect state.
     *
     * @return elect state
     */
    public ElectState getMasterState() {
        return masterState;
    }

    public interface HealthyListener {

        /**
         * on state change.
         *
         * @param healthy state
         */
        void change(ElectState healthy);
    }

    public enum ElectState {
        DISABLE(-2), //todo
        ELECTING(-1),
        FOLLOWING(0),
        LEADING(1);
        public static final List<ElectState> BOOSTING_STATE = ImmutableList.of(LEADING);
        public static final List<ElectState> PROPOSE_STATE = ImmutableList.of(FOLLOWING, LEADING);
        private int state;

        ElectState(final int state) {
            this.state = state;
        }

        private static boolean allowBoost(final ElectState state) {
            return BOOSTING_STATE.contains(state);
        }

        private static boolean allowPropose(final ElectState state) {
            return PROPOSE_STATE.contains(state);
        }

        public int getState() {
            return state;
        }
    }
}
