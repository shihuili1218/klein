package com.ofcoder.klein.consensus.facade;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
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
    protected volatile Endpoint self;

    public int getVersion() {
        return version.get();
    }

    public int incrementVersion() {
        return version.incrementAndGet();
    }

    public Set<Endpoint> getAllMembers() {
        return new HashSet<>(allMembers.values());
    }


    public boolean isValid(String nodeId) {
        return allMembers.containsKey(nodeId);
    }

    public void writeOn(List<Endpoint> nodes, Endpoint self) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }
        allMembers.putAll(nodes.stream().collect(Collectors.toMap(Endpoint::getId, Function.identity())));
        this.self = self;
        version.incrementAndGet();

    }

    public void writeOff(Endpoint node) {
        allMembers.remove(node.getId());
        version.incrementAndGet();
    }

    public abstract MemberConfiguration createRef();
}
