package com.ofcoder.klein.storage.facade;

import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.storage.facade.config.StorageProp;

/**
 * @author far.liu
 */
public class StorageEngine {
    private static LogManager logManager;

    public static void startup(String type, StorageProp prop) {
        logManager = ExtensionLoader.getExtensionLoader(LogManager.class).getJoin(type);

        logManager.init(prop);
    }

    public static void shutdown() {
        if (logManager != null) {
            logManager.shutdown();
        }

    }

    public static LogManager getLogManager() {
        return logManager;
    }

}
