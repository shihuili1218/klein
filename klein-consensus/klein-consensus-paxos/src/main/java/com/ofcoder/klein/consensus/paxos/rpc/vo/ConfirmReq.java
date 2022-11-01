package com.ofcoder.klein.consensus.paxos.rpc.vo;

import java.io.Serializable;
import java.util.List;

import com.ofcoder.klein.consensus.paxos.Proposal;

/**
 * @author far.liu
 */
public class ConfirmReq implements Serializable {
    private String nodeId;
    private long proposalNo;
    private long instanceId;
    private List<Proposal> data;

    public long getProposalNo() {
        return proposalNo;
    }

    public void setProposalNo(long proposalNo) {
        this.proposalNo = proposalNo;
    }

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

    public List<Proposal> getData() {
        return data;
    }

    public void setData(List<Proposal> data) {
        this.data = data;
    }

    public static final class Builder {
        private String nodeId;
        private long proposalNo;
        private long instanceId;
        private List<Proposal> data;

        private Builder() {
        }

        public static Builder aConfirmReq() {
            return new Builder();
        }

        public Builder nodeId(String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public Builder proposalNo(long proposalNo) {
            this.proposalNo = proposalNo;
            return this;
        }

        public Builder instanceId(long instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public Builder data(List<Proposal> data) {
            this.data = data;
            return this;
        }

        public ConfirmReq build() {
            ConfirmReq confirmReq = new ConfirmReq();
            confirmReq.setNodeId(nodeId);
            confirmReq.setProposalNo(proposalNo);
            confirmReq.setInstanceId(instanceId);
            confirmReq.setData(data);
            return confirmReq;
        }
    }
}
