package com.ofcoder.klein.consensus.facade;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * @author far.liu
 */
public abstract class MemberConfiguration implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(MemberConfiguration.class);
    protected AtomicInteger version = new AtomicInteger(0);
    protected volatile Map<String, Endpoint> allMembers = new ConcurrentHashMap<>();

    public AtomicInteger getVersion() {
        return version;
    }

    public void setVersion(AtomicInteger version) {
        this.version = version;
    }

    public void setAllMembers(Map<String, Endpoint> allMembers) {
        this.allMembers = allMembers;
    }

    public Map<String, Endpoint> getAllMembers() {
        return allMembers;
    }


    @Override
    public String toString() {
        return "MemberConfiguration{" +
                "version=" + version +
                ", allMembers=" + allMembers +
                '}';
    }
}
