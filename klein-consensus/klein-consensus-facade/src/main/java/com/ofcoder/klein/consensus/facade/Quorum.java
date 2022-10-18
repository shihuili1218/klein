package com.ofcoder.klein.consensus.facade;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: 释慧利
 */
public class Quorum {
    private List<Node> allMembers = new ArrayList<>();
    private List<Node> grantedMembers = new ArrayList<>();
    private long

    public Quorum(final List<Node> allMembers) {
        this.allMembers = allMembers;
    }

    boolean isGrant() {

        return false;
    }

    void grant() {

    }
}
