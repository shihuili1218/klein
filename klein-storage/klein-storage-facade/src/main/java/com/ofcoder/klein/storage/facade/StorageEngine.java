package com.ofcoder.klein.storage.facade;

import java.io.Serializable;

import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.storage.facade.config.StorageProp;

/**
 * @author far.liu
 */
public final class StorageEngine {
    private LogManager logManager;

    public void startup(String type, StorageProp prop) {
        logManager = ExtensionLoader.getExtensionLoader(LogManager.class).getJoinWithGlobal(type);
        logManager.init(prop);
    }

    public void shutdown() {
        if (logManager != null) {
            logManager.shutdown();
        }
    }

    public static <P extends Serializable> StorageEngine getInstance() {
        return StorageEngineHolder.INSTANCE;
    }

    public static class StorageEngineHolder {
        private static StorageEngine INSTANCE = new StorageEngine();
    }
}
