package com.ofcoder.klein.consensus.paxos.rpc.vo;

import com.ofcoder.klein.consensus.paxos.Proposal;

import java.util.List;

/**
 * @author petter
 */
public class RedirectReq extends BaseReq{

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

        public static RedirectReq.Builder anRedirectReq() {
            return new RedirectReq.Builder();
        }

        public RedirectReq.Builder instanceId(long instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public RedirectReq.Builder data(List<Proposal> data) {
            this.data = data;
            return this;
        }

        public RedirectReq.Builder nodeId(String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public RedirectReq.Builder proposalNo(long proposalNo) {
            this.proposalNo = proposalNo;
            return this;
        }

        public RedirectReq.Builder memberConfigurationVersion(int memberConfigurationVersion) {
            this.memberConfigurationVersion = memberConfigurationVersion;
            return this;
        }

        public RedirectReq build() {
            RedirectReq redirectReq = new RedirectReq();
            redirectReq.setNodeId(nodeId);
            redirectReq.setProposalNo(proposalNo);
            redirectReq.setMemberConfigurationVersion(memberConfigurationVersion);
            redirectReq.instanceId = this.instanceId;
            redirectReq.data = this.data;
            return redirectReq;
        }
    }
}
