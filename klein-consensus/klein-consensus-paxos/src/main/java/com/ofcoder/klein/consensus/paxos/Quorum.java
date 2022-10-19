package com.ofcoder.klein.consensus.paxos;

import java.util.HashSet;
import java.util.Set;

import com.ofcoder.klein.consensus.facade.manager.Node;

/**
 * @author: 释慧利
 */
public class Quorum {
    private Set<Node> allMembers = new HashSet<>();
    private Set<Node> grantedMembers = new HashSet<>();
    private int threshold;

    public Quorum(final Set<Node> allMembers) {
        this.allMembers = allMembers;
        this.threshold = allMembers.size() / 2 + 1;
    }

    boolean isGrant() {
        return grantedMembers.size() >= threshold;
    }

    boolean grant(Node node) {
        return grantedMembers.add(node);
    }
}
