package com.ofcoder.klein.consensus.facade;

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.common.exception.StartupException;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.spi.SPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author far.liu
 */
@SPI
public interface ConsensusEngine extends Lifecycle<ConsensusProp> {
    Logger LOG = LoggerFactory.getLogger(ConsensusEngine.class);

    static void startup(String consensus, ConsensusProp prop) {

        LOG.debug("start consensus engine");
        ConsensusEngine bootstrap = ExtensionLoader.getExtensionLoader(ConsensusEngine.class).getJoin(consensus);
        bootstrap.init(prop);
    }

}
