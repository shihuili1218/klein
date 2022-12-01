package com.ofcoder.klein.consensus.paxos.rpc.vo;

import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.storage.facade.Instance;

import java.io.Serializable;

/**
 * @author petter
 */
public class RedirectRes implements Serializable {

    private String nodeId;

    private boolean result;

    private long curProposalNo;

    private long curInstanceId;

    private Result.State state;


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

    public Result.State getState() {
        return state;
    }

    public static final class Builder {
        private String nodeId;
        private boolean result;
        private long proposalNo;
        private long instanceId;

        private Result.State state;

        private Builder() {
        }

        public static RedirectRes.Builder anRedirectRes() {
            return new RedirectRes.Builder();
        }

        public RedirectRes.Builder nodeId(String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public RedirectRes.Builder result(boolean result) {
            this.result = result;
            return this;
        }

        public RedirectRes.Builder curProposalNo(long proposalNo) {
            this.proposalNo = proposalNo;
            return this;
        }

        public RedirectRes.Builder curInstanceId(long instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public RedirectRes.Builder instanceState(Result.State state) {
            this.state = state;
            return this;
        }

        public RedirectRes build() {
            RedirectRes redirectRes = new RedirectRes();
            redirectRes.curInstanceId = this.instanceId;
            redirectRes.result = this.result;
            redirectRes.curProposalNo = this.proposalNo;
            redirectRes.state = this.state;
            redirectRes.nodeId = this.nodeId;
            return redirectRes;
        }
    }
}
