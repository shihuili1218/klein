package com.ofcoder.klein.storage.facade;

import java.io.Serializable;

import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.storage.facade.config.StorageProp;

/**
 * @author far.liu
 */
public final class StorageEngine<P extends Serializable> {
    private LogManager<P> logManager;

    public void startup(String type, StorageProp prop) {
        logManager = ExtensionLoader.getExtensionLoader(LogManager.class).getJoin(type);
        logManager.init(prop);
    }

    public void shutdown() {
        if (logManager != null) {
            logManager.shutdown();
        }

    }

    public LogManager<P> getLogManager() {
        return logManager;
    }

    public static <P extends Serializable> StorageEngine<P> getInstance() {
        return StorageEngineHolder.INSTANCE;
    }

    public static class StorageEngineHolder {
        private static StorageEngine INSTANCE = new StorageEngine();
    }
}
