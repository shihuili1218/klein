package com.ofcoder.klein.consensus.paxos.core;

import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * Master Info.
 */
public class MasterState {
    private Endpoint master;
    private Master.ElectState electState;
    /**
     * Whether I am a Master. true if I am master.
     */
    private boolean isSelf;

    public MasterState(Endpoint master, Master.ElectState electState, boolean isSelf) {
        this.master = master;
        this.electState = electState;
        this.isSelf = isSelf;
    }

    public Endpoint getMaster() {
        return master;
    }

    public Master.ElectState getElectState() {
        return electState;
    }

    public boolean isSelf() {
        return isSelf;
    }
}
