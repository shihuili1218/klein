package com.ofcoder.klein.rpc.facade.util;

import com.ofcoder.klein.rpc.facade.Endpoint;
import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author: 释慧利
 */
public class RpcUtil {
    public static final String IP_ANY = "0.0.0.0";

    public static String getLocalIp() {
        try {
            InetAddress address = InetAddress.getLocalHost();
            return address.getHostAddress();
        } catch (UnknownHostException e) {
            return IP_ANY;
        }
    }

    public static Endpoint parseEndpoint(final String s) {
        if (StringUtils.isEmpty(s)) {
            throw new IllegalArgumentException("parse Endpoint, but address is empty.");
        }
        final String[] tmps = StringUtils.split(s, ":");
        if (tmps.length != 2) {
            throw new IllegalArgumentException(String.format("parse Endpoint, but address: %s is invalid, e.g. ip:port", s));
        }
        try {
            final int port = Integer.parseInt(tmps[1]);
            return new Endpoint(tmps[0], port);
        } catch (final Exception e) {
            throw new IllegalArgumentException(String.format("parse Endpoint, address: %s, error: %s.", s, e.getMessage()), e);
        }
    }


}
