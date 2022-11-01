package com.ofcoder.klein.consensus.facade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.spi.ExtensionLoader;

/**
 * @author far.liu
 */
public class ConsensusEngine {
    private static final Logger LOG = LoggerFactory.getLogger(ConsensusEngine.class);
    private static Consensus consensus;

    public static void startup(String algorithm, ConsensusProp prop) {
        LOG.info("start consensus engine");
        consensus = ExtensionLoader.getExtensionLoader(Consensus.class).getJoin(algorithm);
        consensus.init(prop);
    }

    public static void shutdown() {
        if (consensus != null) {
            consensus.shutdown();
        }
    }

}
