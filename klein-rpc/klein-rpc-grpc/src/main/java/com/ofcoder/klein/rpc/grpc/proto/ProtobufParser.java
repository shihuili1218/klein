package com.ofcoder.klein.rpc.grpc.proto;

import com.google.protobuf.Message;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: 释慧利
 */
public class ProtobufParser {
    public static final String FIXED_METHOD_NAME = "_call";

    private static final Map<String, Whole> MESSAGES = new HashMap<>();

    static {
        MESSAGES.put(HelloProto.HelloRequest.class.getName(), new Whole(HelloProto.HelloRequest.getDefaultInstance(), HelloProto.HelloReply.getDefaultInstance()));
    }

    public static Whole get(String provider) {
        return MESSAGES.get(provider);
    }


    public static class Whole {
        private Message req;
        private Message res;

        public Whole(Message req, Message res) {
            this.req = req;
            this.res = res;
        }

        public Message getReq() {
            return req;
        }

        public Message getRes() {
            return res;
        }
    }
}
