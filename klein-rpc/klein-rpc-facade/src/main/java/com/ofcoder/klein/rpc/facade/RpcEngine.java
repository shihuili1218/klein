package com.ofcoder.klein.rpc.facade;

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.common.util.Requires;
import com.ofcoder.klein.rpc.facade.config.RpcProp;
import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.spi.SPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author far.liu
 */
@SPI
public interface RpcEngine extends Lifecycle<RpcProp> {
    Logger LOG = LoggerFactory.getLogger(RpcEngine.class);
    static void startup(String rpc, RpcProp prop) {
        LOG.debug("start rpc engine");
        Requires.requireTrue(prop.getPort() > 0 && prop.getPort() < 0xFFFF, "port out of range:" + prop.getPort());
        RpcEngine bootstrap = ExtensionLoader.getExtensionLoader(RpcEngine.class).getJoin(rpc);
        bootstrap.init(prop);
    }
}
