package com.ofcoder.klein.consensus.facade;

import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.util.RpcUtil;
import org.apache.commons.collections4.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author far.liu
 */
public class MemberManager {
    private static int version;
    private static volatile Set<Endpoint> members = new HashSet<>();

    public static int getVersion() {
        return version;
    }

    public static Set<Endpoint> getMembers() {
        return members;
    }

    public static void register(Endpoint node) {
        if (members.add(node)) {
            version++;
        }
    }

    public static void register(List<String> nodes) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }
        for (String node : nodes) {
            register(RpcUtil.parseEndpoint(node));
        }
    }
}
