package com.ofcoder.klein.consensus.paxos.core;

import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * Master Info.
 */
public class MasterState {
    private final Endpoint master;
    private final Master.ElectState electState;
    /**
     * Whether I am a Master. true if I am master.
     */
    private final boolean isSelf;

    public MasterState(final Endpoint master, final Master.ElectState electState, final boolean isSelf) {
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

    @Override
    public String toString() {
        return "MasterState{" +
                "master=" + master +
                ", electState=" + electState +
                ", isSelf=" + isSelf +
                '}';
    }
}
