package com.ofcoder.klein.consensus.facade;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import com.ofcoder.klein.rpc.facade.Endpoint;

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

    public static void writeOn(Endpoint node) {
        if (members.add(node)) {
            version++;
        }
    }

    public static void writeOn(List<Endpoint> nodes) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }
        if (members.addAll(nodes)) {
            version++;
        }
    }

    public static void writeOff(Endpoint node){
        if (members.remove(node)) {
            version++;
        }
    }
}
