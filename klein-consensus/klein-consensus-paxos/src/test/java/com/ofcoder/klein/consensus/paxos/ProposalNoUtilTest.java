package com.ofcoder.klein.consensus.paxos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

public class ProposalNoUtilTest extends TestCase {
    private static final Logger LOG = LoggerFactory.getLogger(ProposalNoUtilTest.class);

    public void testMakePno() {
        for (int i = 0; i < 100; i++) {
            for (int counter = 0; counter < 50; counter++) {
                long pno = ProposalNoUtil.makePno(i, counter);
                LOG.info("pno: {}, str: {}", pno, ProposalNoUtil.pnoToString(pno));
                assertEquals(ProposalNoUtil.getCounterFromPno(pno), counter);
                assertEquals(ProposalNoUtil.getEpochFromPno(pno), i);
            }
            LOG.info("i: {}", i);
        }
    }
}