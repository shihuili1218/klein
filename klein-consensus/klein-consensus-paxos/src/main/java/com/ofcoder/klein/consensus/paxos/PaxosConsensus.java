package com.ofcoder.klein.consensus.paxos;

import java.nio.ByteBuffer;

import com.ofcoder.klein.consensus.facade.manager.Consensus;
import com.ofcoder.klein.consensus.facade.manager.Result;

/**
 * @author far.liu
 */
public class PaxosConsensus implements Consensus {
    @Override
    public Result propose(ByteBuffer data) {
        return null;
    }

    @Override
    public Result read(ByteBuffer data) {
        return null;
    }
}
