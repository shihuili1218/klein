package com.ofcoder.klein.consensus.paxos.rpc;

/**
 * @author far.liu
 */
public enum MessageType {
    PREPARE(0x01),
    ACCEPT(0x02),
    CONFIRM(0x03);

    private MessageType(int code) {

    }
}
