package com.ofcoder.klein.consensus.facade;

import java.util.HashSet;
import java.util.Set;

/**
 * @author far.liu
 */
public class MemberManager {

    private static int version;
    private static volatile Set<Node> members = new HashSet<>();

    public static int getVersion() {
        return version;
    }

    public static Set<Node> getMembers() {
        return members;
    }

    public synchronized static boolean register(Node node) {
        if (members.add(node)) {
            version++;
            return true;
        } else {
            return false;
        }
    }
}
