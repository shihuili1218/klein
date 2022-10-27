package com.ofcoder.klein.consensus.paxos.rpc.vo;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.List;

import com.ofcoder.klein.storage.facade.Instance;

/**
 * @author far.liu
 */
public class PrepareRes implements Serializable {
    private String nodeId;
    private boolean result;
    private long proposalNo;
    private List<Instance> instances;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public boolean getResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public long getProposalNo() {
        return proposalNo;
    }

    public void setProposalNo(long proposalNo) {
        this.proposalNo = proposalNo;
    }

    public List<Instance> getInstances() {
        return instances;
    }

    public void setInstances(List<Instance> instances) {
        this.instances = instances;
    }

    public static final class Builder {
        private String nodeId;
        private boolean result;
        private long proposalNo;
        private List<Instance> instances;

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

        public Builder proposalNo(long proposalNo) {
            this.proposalNo = proposalNo;
            return this;
        }

        public Builder instances(List<Instance> instances) {
            this.instances = instances;
            return this;
        }

        public PrepareRes build() {
            PrepareRes prepareRes = new PrepareRes();
            prepareRes.setNodeId(nodeId);
            prepareRes.setResult(result);
            prepareRes.setProposalNo(proposalNo);
            prepareRes.instances = this.instances;
            return prepareRes;
        }
    }
}
