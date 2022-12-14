package com.ofcoder.klein.consensus.paxos.rpc.vo;

import java.io.Serializable;
import java.util.List;

import com.ofcoder.klein.consensus.paxos.Proposal;
import com.ofcoder.klein.storage.facade.Instance;

/**
 * @author far.liu
 */
public class PrepareRes implements Serializable {
    private String nodeId;
    private boolean result;
    private long curProposalNo;
    private long curInstanceId;
    private List<Instance<Proposal>> instances;
    private NodeState nodeState;

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

    public List<Instance<Proposal>> getInstances() {
        return instances;
    }

    public NodeState getNodeState() {
        return nodeState;
    }

    public static final class Builder {
        private String nodeId;
        private boolean result;
        private long curProposalNo;
        private long curInstanceId;
        private List<Instance<Proposal>> instances;
        private NodeState nodeState;

        private Builder() {
        }

        public static Builder aPrepareRes() {
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

        public Builder curProposalNo(long curProposalNo) {
            this.curProposalNo = curProposalNo;
            return this;
        }

        public Builder curInstanceId(long curInstanceId) {
            this.curInstanceId = curInstanceId;
            return this;
        }

        public Builder instances(List<Instance<Proposal>> instances) {
            this.instances = instances;
            return this;
        }

        public Builder nodeState(NodeState masterState) {
            this.nodeState = masterState;
            return this;
        }

        public PrepareRes build() {
            PrepareRes prepareRes = new PrepareRes();
            prepareRes.result = this.result;
            prepareRes.curInstanceId = this.curInstanceId;
            prepareRes.instances = this.instances;
            prepareRes.curProposalNo = this.curProposalNo;
            prepareRes.nodeId = this.nodeId;
            prepareRes.nodeState = this.nodeState;
            return prepareRes;
        }
    }
}
