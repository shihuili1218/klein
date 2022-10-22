package com.ofcoder.klein.consensus.paxos.rpc.vo;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author far.liu
 */
public class PrepareReq implements Serializable {
    private String nodeId;
    private long instanceId;
    private long proposalNo;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(long instanceId) {
        this.instanceId = instanceId;
    }

    public long getProposalNo() {
        return proposalNo;
    }

    public void setProposalNo(long proposalNo) {
        this.proposalNo = proposalNo;
    }

    @Override
    public String toString() {
        return "PrepareReq{" +
                "nodeId='" + nodeId + '\'' +
                ", instanceId=" + instanceId +
                ", proposalNo=" + proposalNo +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrepareReq that = (PrepareReq) o;
        return instanceId == that.instanceId && proposalNo == that.proposalNo && Objects.equals(nodeId, that.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, instanceId, proposalNo);
    }

    public static final class Builder {
        private String nodeId;
        private long instanceId;
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

        public Builder instanceId(long instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public Builder proposalNo(long proposalNo) {
            this.proposalNo = proposalNo;
            return this;
        }

        public PrepareReq build() {
            PrepareReq prepareReq = new PrepareReq();
            prepareReq.instanceId = this.instanceId;
            prepareReq.proposalNo = this.proposalNo;
            prepareReq.nodeId = this.nodeId;
            return prepareReq;
        }
    }
}
