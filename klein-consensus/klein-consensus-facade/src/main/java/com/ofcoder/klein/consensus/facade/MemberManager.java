package com.ofcoder.klein.consensus.facade;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * @author far.liu
 */
public class MemberManager {
    private static int version;
    private static volatile Map<String, Endpoint> allMembers = new ConcurrentHashMap<>();
    private static volatile Set<Endpoint> membersWithoutSelf = new HashSet<>();

    public static int getVersion() {
        return version;
    }

    public static Set<Endpoint> getAllMembers() {
        return new HashSet<>(allMembers.values());
    }

    public static Set<Endpoint> getMembersWithoutSelf() {
        return membersWithoutSelf;
    }

    public static boolean isValid(String nodeId) {
        return allMembers.containsKey(nodeId);
    }

    public static void writeOn(List<Endpoint> nodes, Endpoint self) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }
        allMembers.putAll(nodes.stream().collect(Collectors.toMap(Endpoint::getId, Function.identity())));
        membersWithoutSelf.addAll(nodes.stream().filter(it -> !it.equals(self)).collect(Collectors.toSet()));
        version++;
    }

    public static void writeOff(Endpoint node) {
        allMembers.remove(node.getId());
        membersWithoutSelf.remove(node);
        version++;
    }
}
