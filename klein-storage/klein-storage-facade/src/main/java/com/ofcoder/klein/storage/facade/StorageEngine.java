package com.ofcoder.klein.storage.facade;

import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.storage.facade.config.StorageProp;

/**
 * @author far.liu
 */
public class StorageEngine {
    private static LogManager logManager;
    private static SMManager smManager;

    public static void startup(String type, StorageProp prop) {
        logManager = ExtensionLoader.getExtensionLoader(LogManager.class).getJoin(type);
        smManager = ExtensionLoader.getExtensionLoader(SMManager.class).getJoin(type);

        logManager.init(prop);
        smManager.init(prop);
    }

    public static void shutdown() {
        if (logManager != null) {
            logManager.shutdown();
        }
        if (smManager != null) {
            smManager.shutdown();
        }
    }

    public static LogManager getLogManager() {
        return logManager;
    }

    public static SMManager getSmManager() {
        return smManager;
    }
}
