package com.ofcoder.klein.consensus.paxos.rpc.vo;

import java.io.Serializable;
import java.nio.ByteBuffer;

import com.ofcoder.klein.storage.facade.Instance;

/**
 * @author far.liu
 */
public class AcceptRes implements Serializable {
    private String nodeId;
    private boolean result;
    private long proposalNo;

    public String getNodeId() {
        return nodeId;
    }

    public boolean getResult() {
        return result;
    }

    public long getProposalNo() {
        return proposalNo;
    }

    public static final class Builder {
        private String nodeId;
        private boolean result;
        private long proposalNo;

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

        public Builder proposalNo(long proposalNo) {
            this.proposalNo = proposalNo;
            return this;
        }



        public AcceptRes build() {
            AcceptRes acceptRes = new AcceptRes();
            acceptRes.nodeId = this.nodeId;
            acceptRes.proposalNo = this.proposalNo;
            acceptRes.result = this.result;
            return acceptRes;
        }
    }
}
