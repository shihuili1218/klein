package com.ofcoder.klein.consensus.facade.config;

import java.util.List;

import com.google.common.collect.Lists;
import com.ofcoder.klein.common.util.SystemPropertyUtil;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.util.RpcUtil;

/**
 * @author far.liu
 */
public class ConsensusProp {
    private Endpoint self = new Endpoint("1", RpcUtil.getLocalIp(), 1218);
    /**
     * all member, include self.
     */
    private List<Endpoint> members = Lists.newArrayList(self);
    /**
     * timeout for single round.
     */
    private long roundTimeout = SystemPropertyUtil.getLong("klein.consensus.round-timeout", 1000);
    /**
     * the number of proposals negotiated by the single round.
     */
    private int batchSize = SystemPropertyUtil.getInt("klein.consensus.batch-size", 3);
    /**
     * negotiation failed, number of retry times.
     * if set 2, then runs 3 times
     */
    private int retry = SystemPropertyUtil.getInt("klein.consensus.retry", 2);

    public Endpoint getSelf() {
        return self;
    }

    public void setSelf(Endpoint self) {
        this.self = self;
    }

    public List<Endpoint> getMembers() {
        return members;
    }

    public void setMembers(List<Endpoint> members) {
        this.members = members;
    }

    public long getRoundTimeout() {
        return roundTimeout;
    }

    public void setRoundTimeout(long roundTimeout) {
        this.roundTimeout = roundTimeout;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }
}
