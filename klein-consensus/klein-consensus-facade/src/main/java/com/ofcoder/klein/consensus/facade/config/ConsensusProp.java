package com.ofcoder.klein.consensus.facade.config;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;
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
    private long roundTimeout = 10000;

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
}
