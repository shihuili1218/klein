package com.ofcoder.klein.consensus.paxos.rpc.vo;

/**
 * @author far.liu
 */
public class PrepareReq extends BaseReq {

    public static final class Builder {
        private String nodeId;
        private long proposalNo;
        private int memberConfigurationVersion;

        private Builder() {
        }

        public static Builder aPrepareReq() {
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

        public Builder memberConfigurationVersion(int memberConfigurationVersion) {
            this.memberConfigurationVersion = memberConfigurationVersion;
            return this;
        }

        public PrepareReq build() {
            PrepareReq prepareReq = new PrepareReq();
            prepareReq.setNodeId(nodeId);
            prepareReq.setProposalNo(proposalNo);
            prepareReq.setMemberConfigurationVersion(memberConfigurationVersion);
            return prepareReq;
        }
    }
}
