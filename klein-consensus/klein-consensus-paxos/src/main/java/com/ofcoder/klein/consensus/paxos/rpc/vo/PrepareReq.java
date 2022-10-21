package com.ofcoder.klein.consensus.paxos.rpc.vo;

/**
 * @author far.liu
 */
public class PrepareReq {
    private String nodeId;
    private long index;
    private long proposalNo;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public long getProposalNo() {
        return proposalNo;
    }

    public void setProposalNo(long proposalNo) {
        this.proposalNo = proposalNo;
    }

    public static final class Builder {
        private String nodeId;
        private long index;
        private long proposalNo;

        private Builder() {
        }

        public static Builder aPrepareReq() {
            return new Builder();
        }

        public Builder nodeId(String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public Builder index(long index) {
            this.index = index;
            return this;
        }

        public Builder proposalNo(long proposalNo) {
            this.proposalNo = proposalNo;
            return this;
        }

        public PrepareReq build() {
            PrepareReq prepareReq = new PrepareReq();
            prepareReq.setNodeId(nodeId);
            prepareReq.setIndex(index);
            prepareReq.setProposalNo(proposalNo);
            return prepareReq;
        }
    }
}
