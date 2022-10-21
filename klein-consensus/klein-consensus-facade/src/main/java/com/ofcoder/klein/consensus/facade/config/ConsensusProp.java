package com.ofcoder.klein.consensus.facade.config;

import java.util.ArrayList;
import java.util.List;

import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * @author far.liu
 */
public class ConsensusProp {
    private String id = "1";
    /**
     * all member
     * member info include: id, ip, port.
     */
    private List<Endpoint> members = new ArrayList<>();
    /**
     * timeout for single round.
     */
    private long roundTimeout = 1000;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
