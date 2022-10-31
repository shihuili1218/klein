package com.ofcoder.klein.consensus.facade;

import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.spi.SPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author far.liu
 */
@SPI
public class ConsensusEngine {
    private static final Logger LOG = LoggerFactory.getLogger(ConsensusEngine.class);
    private static Consensus consensus;

    public static void startup(String algorithm, ConsensusProp prop) {
        LOG.info("start consensus engine");

        consensus = ExtensionLoader.getExtensionLoader(Consensus.class).getJoin(algorithm);
        consensus.init(prop);
    }

    public static void loadSM(SM sm) {
        consensus.loadSM(sm);
    }

    public static void shutdown() {
        if (consensus != null) {
            consensus.shutdown();
        }
    }

}
