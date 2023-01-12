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
package com.ofcoder.klein.consensus.facade.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.ofcoder.klein.common.util.SystemPropertyUtil;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.util.RpcUtil;

/**
 * consensus property.
 *
 * @author far.liu
 */
public class ConsensusProp {
    private Endpoint self = new Endpoint(
            SystemPropertyUtil.get("klein.id", "1"),
            SystemPropertyUtil.get("klein.ip", "127.0.0.1"),
            SystemPropertyUtil.getInt("klein.port", 1218)
    );
    /**
     * all member, include self.
     */
    private List<Endpoint> members = parseMember(SystemPropertyUtil.get("klein.members", "1:127.0.0.1:1218"));
    /**
     * join cluster, this member is not in the cluster, and will automatically join the cluster at startup.
     */
    private boolean joinCluster = SystemPropertyUtil.getBoolean("klein.consensus.join-cluster", false);
    /**
     * timeout for single round.
     */
    private long roundTimeout = SystemPropertyUtil.getLong("klein.consensus.round-timeout", 150);
    /**
     * timeout for single change member.
     */
    private long changeMemberTimeout = SystemPropertyUtil.getLong("klein.consensus.change-member-timeout", roundTimeout * 2);
    /**
     * the number of proposals negotiated by the single round.
     */
    private int batchSize = SystemPropertyUtil.getInt("klein.consensus.batch-size", 5);
    /**
     * negotiation failed, number of retry times.
     * if set 3, then runs 1 times and retry 2 times.
     */
    private int retry = SystemPropertyUtil.getInt("klein.consensus.retry", 2);
    /**
     * calculate quorum.
     */
    private String nwr = SystemPropertyUtil.get("klein.consensus.nwr", "majority");
    private PaxosProp paxosProp = new PaxosProp();

    private List<Endpoint> parseMember(final String members) {
        List<Endpoint> endpoints = new ArrayList<>();
        if (StringUtils.isEmpty(members)) {
            return endpoints;
        }
        for (String item : StringUtils.split(members, ";")) {
            if (StringUtils.isEmpty(item)) {
                continue;
            }
            endpoints.add(RpcUtil.parseEndpoint(item));
        }
        return endpoints;
    }

    public Endpoint getSelf() {
        return self;
    }

    public void setSelf(final Endpoint self) {
        this.self = self;
    }

    public List<Endpoint> getMembers() {
        return members;
    }

    public void setMembers(final List<Endpoint> members) {
        this.members = members;
    }

    public boolean isJoinCluster() {
        return joinCluster;
    }

    public void setJoinCluster(final boolean joinCluster) {
        this.joinCluster = joinCluster;
    }

    public long getRoundTimeout() {
        return roundTimeout;
    }

    public void setRoundTimeout(final long roundTimeout) {
        this.roundTimeout = roundTimeout;
    }

    public long getChangeMemberTimeout() {
        return changeMemberTimeout;
    }

    public void setChangeMemberTimeout(final long changeMemberTimeout) {
        this.changeMemberTimeout = changeMemberTimeout;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(final int batchSize) {
        this.batchSize = batchSize;
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(final int retry) {
        this.retry = retry;
    }

    public PaxosProp getPaxosProp() {
        return paxosProp;
    }

    public void setPaxosProp(final PaxosProp paxosProp) {
        this.paxosProp = paxosProp;
    }

    public String getNwr() {
        return nwr;
    }

    public void setNwr(final String nwr) {
        this.nwr = nwr;
    }
}
