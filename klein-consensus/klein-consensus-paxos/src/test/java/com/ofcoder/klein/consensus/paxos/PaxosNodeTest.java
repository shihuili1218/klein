package com.ofcoder.klein.consensus.paxos;

import com.google.common.collect.Lists;
import com.ofcoder.klein.rpc.facade.Endpoint;

import junit.framework.TestCase;

/**
 * @author far.liu
 */
public class PaxosNodeTest extends TestCase {

    public void testGenerateNextProposalNo() {
        PaxosMemberConfiguration configuration = new PaxosMemberConfiguration();
        configuration.writeOn(
                Lists.newArrayList(new Endpoint("1", "127.0.0.1", 1218), new Endpoint("2", "127.0.0.1", 1219), new Endpoint("3", "127.0.0.1", 1220))
                , new Endpoint("3", "127.0.0.1", 1220)
        );
        PaxosNode node = PaxosNode.Builder.aPaxosNode()
                .curProposalNo(0)
                .self(new Endpoint("3", "127.0.0.1", 1220))
                .memberConfiguration(configuration).build();
        assertEquals(node.generateNextProposalNo(), 3);
    }
}