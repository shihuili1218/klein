package com.ofcoder.klein.rpc.facade;

import com.ofcoder.klein.common.util.Requires;
import com.ofcoder.klein.rpc.facade.config.RpcProp;
import com.ofcoder.klein.spi.ExtensionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author far.liu
 */
public class RpcEngine {
    private static final Logger LOG = LoggerFactory.getLogger(RpcEngine.class);
    private static RpcServer server;
    private static RpcClient client;

    public static void startup(String rpc, RpcProp prop) {
        LOG.info("start rpc engine");
        Requires.requireTrue(prop.getPort() > 0 && prop.getPort() < 0xFFFF, "port out of range:" + prop.getPort());

        server = ExtensionLoader.getExtensionLoader(RpcServer.class).getJoin(rpc);
        server.init(prop);
        client = ExtensionLoader.getExtensionLoader(RpcClient.class).getJoin(rpc);
        client.init(prop);
    }

    public static void registerProcessor(final RpcProcessor processor) {
        server.registerProcessor(processor);
    }

    // fixme use spi for get client?
    public static RpcClient getClient() {
        return client;
    }

    public static void shutdown() {
        if (server != null) {
            server.shutdown();
        }
        if (client != null) {
            client.shutdown();
        }
    }
}
