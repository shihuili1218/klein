/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ofcoder.klein.storage.facade;

import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.storage.facade.config.StorageProp;

/**
 * Storage Engine.
 *
 * @author far.liu
 */
public final class StorageEngine {
    private static LogManager logManager;
    private static TraceManager traceManager;

    /**
     * start up.
     *
     * @param type storage type
     * @param prop property
     */
    public static void startup(final String type, final StorageProp prop) {
        logManager = ExtensionLoader.getExtensionLoader(LogManager.class).register(type, prop);
        traceManager = ExtensionLoader.getExtensionLoader(TraceManager.class).register(type, prop);
    }

    /**
     * shutdown.
     */
    public static void shutdown() {
        if (logManager != null) {
            logManager.shutdown();
        }
        if (traceManager != null) {
            traceManager.shutdown();
        }
    }

}
