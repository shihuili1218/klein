package com.ofcoder.klein.consensus.paxos.rpc.vo;

import java.nio.ByteBuffer;

/**
 * @author far.liu
 */
public class PrepareRes {
    private String nodeId;
    private long proposalNo;
    private ByteBuffer grantValue;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public long getProposalNo() {
        return proposalNo;
    }

    public void setProposalNo(long proposalNo) {
        this.proposalNo = proposalNo;
    }

    public ByteBuffer getGrantValue() {
        return grantValue;
    }

    public void setGrantValue(ByteBuffer grantValue) {
        this.grantValue = grantValue;
    }
}
