package com.ofcoder.klein.consensus.facade;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.common.exception.StartupException;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.spi.ExtensionLoader;

/**
 * @author far.liu
 */
public class ConsensusEngine {

    private static final Logger LOG = LoggerFactory.getLogger(ConsensusEngine.class);
    private static volatile AtomicBoolean started = new AtomicBoolean(false);

    public static void startup(String consensus, ConsensusProp prop) {
        if (started.get()) {
            throw new StartupException("consensus engine has started.");
        }
        if (!started.compareAndSet(false, true)) {
            LOG.warn("consensus engine is starting.");
            return ;
        }
        LOG.debug("start consensus engine");
        ConsensusBootstrap bootstrap = ExtensionLoader.getExtensionLoader(ConsensusBootstrap.class).getJoin(consensus);
        bootstrap.startup(prop);
    }

}
