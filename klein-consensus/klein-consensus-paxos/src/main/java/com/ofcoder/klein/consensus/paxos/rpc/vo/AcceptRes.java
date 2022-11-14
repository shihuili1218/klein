package com.ofcoder.klein.consensus.paxos.rpc.vo;

import java.io.Serializable;

import com.ofcoder.klein.storage.facade.Instance;

/**
 * @author far.liu
 */
public class AcceptRes implements Serializable {
    private String nodeId;
    private boolean result;
    private long curProposalNo;
    private long curInstanceId;
    private Instance.State instanceState;

    public String getNodeId() {
        return nodeId;
    }

    public boolean getResult() {
        return result;
    }

    public long getCurProposalNo() {
        return curProposalNo;
    }

    public long getCurInstanceId() {
        return curInstanceId;
    }

    public Instance.State getInstanceState() {
        return instanceState;
    }

    public static final class Builder {
        private String nodeId;
        private boolean result;
        private long proposalNo;
        private long instanceId;
        private Instance.State instanceState;

        private Builder() {
        }

        public static Builder anAcceptRes() {
            return new Builder();
        }

        public Builder nodeId(String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public Builder result(boolean result) {
            this.result = result;
            return this;
        }

        public Builder curProposalNo(long proposalNo) {
            this.proposalNo = proposalNo;
            return this;
        }

        public Builder curInstanceId(long instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public Builder instanceState(Instance.State instanceState) {
            this.instanceState = instanceState;
            return this;
        }

        public AcceptRes build() {
            AcceptRes acceptRes = new AcceptRes();
            acceptRes.curInstanceId = this.instanceId;
            acceptRes.result = this.result;
            acceptRes.curProposalNo = this.proposalNo;
            acceptRes.instanceState = this.instanceState;
            acceptRes.nodeId = this.nodeId;
            return acceptRes;
        }
    }
}
