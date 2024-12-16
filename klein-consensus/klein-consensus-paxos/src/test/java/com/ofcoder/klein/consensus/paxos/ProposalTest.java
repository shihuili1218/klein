package com.ofcoder.klein.consensus.paxos;

import com.ofcoder.klein.serializer.hessian2.Hessian2Util;
import junit.framework.TestCase;

public class ProposalTest extends TestCase {

    public void testGetGroup() {
        Proposal proposal = new Proposal();
        assertNull(proposal.getGroup());

        proposal = new Proposal("group1", Hessian2Util.serialize("data1"));
        assertEquals("group1", proposal.getGroup());

        proposal.setGroup("group2");
        assertEquals("group2", proposal.getGroup());
    }
}
