package com.ofcoder.klein.consensus.paxos;

import java.nio.ByteBuffer;

/**
 * @author: 释慧利
 */
public class Proposal {
    private long proposalNo;
    private ByteBuffer data = ByteBuffer.wrap(new byte[0]);
    private Quorum quorum;

    public long getProposalNo() {
        return proposalNo;
    }

    public void setProposalNo(long proposalNo) {
        this.proposalNo = proposalNo;
    }

    public ByteBuffer getData() {
        return data;
    }

    public void setData(ByteBuffer data) {
        this.data = data;
    }

    public Quorum getQuorum() {
        return quorum;
    }

    public void setQuorum(Quorum quorum) {
        this.quorum = quorum;
    }

    @Override
    public String toString() {
        return "Proposal{" +
                "proposalNo=" + proposalNo +
                ", data=" + data +
                ", quorum=" + quorum +
                '}';
    }
}
