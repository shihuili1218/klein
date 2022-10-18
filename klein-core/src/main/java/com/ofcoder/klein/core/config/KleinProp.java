package com.ofcoder.klein.core.config;

import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.rpc.facade.config.RpcProp;
import com.ofcoder.klein.storage.facade.config.StorageProp;

/**
 * klein:
 * storage: jvm
 * consensus: paxos
 * rpc: netty
 * max-size:
 *
 * @author: 释慧利
 */
public class KleinProp {
    private String storage = "jvm";
    private String consensus = "paxos";
    private String rpc = "netty";
    private ConsensusProp consensusProp;
    private StorageProp storageProp;
    private RpcProp rpcProp;

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public String getConsensus() {
        return consensus;
    }

    public void setConsensus(String consensus) {
        this.consensus = consensus;
    }

    public ConsensusProp getConsensusProp() {
        return consensusProp;
    }

    public void setConsensusProp(ConsensusProp consensusProp) {
        this.consensusProp = consensusProp;
    }

    public String getRpc() {
        return rpc;
    }

    public void setRpc(String rpc) {
        this.rpc = rpc;
    }

    public StorageProp getStorageProp() {
        return storageProp;
    }

    public void setStorageProp(StorageProp storageProp) {
        this.storageProp = storageProp;
    }

    public RpcProp getRpcProp() {
        return rpcProp;
    }

    public void setRpcProp(RpcProp rpcProp) {
        this.rpcProp = rpcProp;
    }

    public static KleinProp loadIfPresent() {
        return KleinPropHolder.INSTANCE;
    }

    private static class KleinPropHolder {
        private static final KleinProp INSTANCE = new KleinProp();
    }
}
