package com.ofcoder.klein.consensus.paxos.rpc.vo;

import java.util.List;

import com.ofcoder.klein.consensus.paxos.Proposal;

/**
 * @author far.liu
 */
public class AcceptReq extends BaseReq {
    private long instanceId;
    private List<Proposal> data;

    public long getInstanceId() {
        return instanceId;
    }

    public List<Proposal> getData() {
        return data;
    }

    public static final class Builder {
        private long instanceId;
        private List<Proposal> data;
        private String nodeId;
        private long proposalNo;
        private int memberConfigurationVersion;

        private Builder() {
        }

        public static Builder anAcceptReq() {
            return new Builder();
        }

        public Builder instanceId(long instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public Builder data(List<Proposal> data) {
            this.data = data;
            return this;
        }

        public Builder nodeId(String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public Builder proposalNo(long proposalNo) {
            this.proposalNo = proposalNo;
            return this;
        }

        public Builder memberConfigurationVersion(int memberConfigurationVersion) {
            this.memberConfigurationVersion = memberConfigurationVersion;
            return this;
        }

        public AcceptReq build() {
            AcceptReq acceptReq = new AcceptReq();
            acceptReq.setNodeId(nodeId);
            acceptReq.setProposalNo(proposalNo);
            acceptReq.setMemberConfigurationVersion(memberConfigurationVersion);
            acceptReq.instanceId = this.instanceId;
            acceptReq.data = this.data;
            return acceptReq;
        }
    }
}
