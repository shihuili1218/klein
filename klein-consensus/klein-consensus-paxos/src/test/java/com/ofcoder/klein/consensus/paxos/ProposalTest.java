package com.ofcoder.klein.consensus.paxos;

import junit.framework.TestCase;

public class ProposalTest extends TestCase {

    public void testGetGroup() {
        Proposal proposal = new Proposal();
        assertNull(proposal.getGroup());

        proposal = new Proposal("group1", "data1");
        assertEquals("group1", proposal.getGroup());

        proposal.setGroup("group2");
        assertEquals("group2", proposal.getGroup());
    }
}
