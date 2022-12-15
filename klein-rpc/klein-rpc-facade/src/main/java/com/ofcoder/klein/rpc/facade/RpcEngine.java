package com.ofcoder.klein.rpc.facade;

import com.ofcoder.klein.common.util.Requires;
import com.ofcoder.klein.rpc.facade.config.RpcProp;
import com.ofcoder.klein.spi.ExtensionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author far.liu
 */
public final class RpcEngine {
    private static final Logger LOG = LoggerFactory.getLogger(RpcEngine.class);
    private static RpcServer server;
    private static RpcClient client;

    public static void startup(String rpc, RpcProp prop) {
        LOG.info("start rpc engine");
        Requires.requireTrue(prop.getPort() > 0 && prop.getPort() < 0xFFFF, "port out of range:" + prop.getPort());

        server = ExtensionLoader.getExtensionLoader(RpcServer.class).getJoinWithGlobal(rpc);
        server.init(prop);
        client = ExtensionLoader.getExtensionLoader(RpcClient.class).getJoinWithGlobal(rpc);
        client.init(prop);
    }

    public static void registerProcessor(final RpcProcessor processor) {
        server.registerProcessor(processor);
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
