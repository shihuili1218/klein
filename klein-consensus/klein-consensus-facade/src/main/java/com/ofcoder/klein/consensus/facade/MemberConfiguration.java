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
import org.apache.commons.lang3.StringUtils;
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

    public int getVersion() {
        return version.get();
    }

    public Set<Endpoint> getAllMembers() {
        return new HashSet<>(allMembers.values());
    }

    public Set<Endpoint> getMembersWithout(String selfId) {
        return getAllMembers().stream().filter(it -> !StringUtils.equals(selfId, it.getId()))
                .collect(Collectors.toSet());
    }

    public boolean isValid(String nodeId) {
        return allMembers.containsKey(nodeId);
    }

    public Endpoint getEndpointById(String id) {
        if (StringUtils.isEmpty(id)) {
            return null;
        }
        return allMembers.getOrDefault(id, null);
    }

    protected void writeOn(Endpoint node) {
        allMembers.put(node.getId(), node);
        version.incrementAndGet();
    }

    protected void writeOff(Endpoint node) {
        allMembers.remove(node.getId());
        version.incrementAndGet();
    }

    protected void init(List<Endpoint> nodes) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }
        this.allMembers.putAll(nodes.stream().collect(Collectors.toMap(Endpoint::getId, Function.identity())));
        this.version.incrementAndGet();
    }

    @Override
    public String toString() {
        return "MemberConfiguration{" +
                "version=" + version +
                ", allMembers=" + allMembers +
                '}';
    }
}
