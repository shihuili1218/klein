package com.ofcoder.klein.consensus.paxos;

import com.google.common.collect.Lists;
import com.ofcoder.klein.consensus.facade.nwr.Nwr;
import com.ofcoder.klein.consensus.paxos.core.sm.MemberRegistry;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.spi.ExtensionLoader;
import junit.framework.TestCase;

/**
 * @author far.liu
 */
public class PaxosNodeTest extends TestCase {

    public void testGenerateNextProposalNo() {

        ExtensionLoader.getExtensionLoader(Nwr.class).getJoin("majority");
        MemberRegistry.getInstance().init(
                Lists.newArrayList(new Endpoint("1", "127.0.0.1", 1218), new Endpoint("2", "127.0.0.1", 1219), new Endpoint("3", "127.0.0.1", 1220))
        );
        PaxosNode node = PaxosNode.Builder.aPaxosNode()
                .curProposalNo(0)
                .self(new Endpoint("3", "127.0.0.1", 1220))
                .build();

        long pno = node.generateNextProposalNo();
        assertEquals(ProposalNoUtil.getCounterFromPno(pno), 3);
        assertEquals(ProposalNoUtil.getEpochFromPno(pno), MemberRegistry.getInstance().getMemberConfiguration().getVersion());

        pno = node.generateNextProposalNo();
        assertEquals(ProposalNoUtil.getCounterFromPno(pno), 6);
        assertEquals(ProposalNoUtil.getEpochFromPno(pno), MemberRegistry.getInstance().getMemberConfiguration().getVersion());
    }

}