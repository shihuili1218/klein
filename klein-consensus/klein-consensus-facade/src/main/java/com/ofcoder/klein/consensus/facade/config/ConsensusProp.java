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

import com.google.common.collect.Lists;
import com.ofcoder.klein.common.util.SystemPropertyUtil;
import com.ofcoder.klein.consensus.facade.exception.SnapshotException;
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
            SystemPropertyUtil.getInt("klein.port", 1218),
            SystemPropertyUtil.getBoolean("klein.outsider", false)
    );
    /**
     * all member, include self.
     */
    private List<Endpoint> members = parseMember(SystemPropertyUtil.get("klein.members", "1:127.0.0.1:1218:false"));
    /**
     * The node is not included in the cluster.
     * When it starts, it will actively join the cluster, and when it shuts down, it will actively exit the cluster.
     */
    private boolean elastic = SystemPropertyUtil.getBoolean("klein.consensus.elastic", false);
    /**
     * timeout for single round.
     */
    private long roundTimeout = SystemPropertyUtil.getLong("klein.consensus.round-timeout", 500);
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

    /**
     * 快照配置.
     * default: 1 minute 1w req || 5 minutes 10 req || 30 minutes 1 req.
     */
    private List<SnapshotStrategy> snapshotStrategy = parseSnapStrategy(SystemPropertyUtil.get("klein.snapshot.generation-policy", "60 10000 300 10 1800 1"));

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

    private List<SnapshotStrategy> parseSnapStrategy(final String prop) {
        String[] props = prop.split(" ");
        if (props.length % 2 != 0) {
            throw new SnapshotException("klein.snapshot.generation-policy config must appear in pairs.");
        }

        List<SnapshotStrategy> snapshotStrategies = Lists.newArrayList();
        for (int i = 0; i < props.length;) {
            SnapshotStrategy snapshotStrategy = new SnapshotStrategy(Integer.parseInt(props[i++]), Integer.parseInt(props[i++]));
            snapshotStrategies.add(snapshotStrategy);
        }
        return snapshotStrategies;
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

    public boolean isElastic() {
        return elastic;
    }

    public void setElastic(final boolean elastic) {
        this.elastic = elastic;
    }

    public long getRoundTimeout() {
        return roundTimeout;
    }

    public void setRoundTimeout(final long roundTimeout) {
        this.roundTimeout = roundTimeout;
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

    public void setSnapshotStrategy(final List<SnapshotStrategy> snapshotStrategy) {
        this.snapshotStrategy = snapshotStrategy;
    }

    public List<SnapshotStrategy> getSnapshotStrategy() {
        return snapshotStrategy;
    }
}
