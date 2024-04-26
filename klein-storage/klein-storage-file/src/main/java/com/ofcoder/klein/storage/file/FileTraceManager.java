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
package com.ofcoder.klein.storage.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.ofcoder.klein.common.util.StreamUtil;
import com.ofcoder.klein.spi.Join;
import com.ofcoder.klein.storage.facade.TraceManager;
import com.ofcoder.klein.storage.facade.config.StorageProp;
import com.ofcoder.klein.storage.facade.exception.StorageException;

/**
 * implement for TraceManager.
 */
@Join
public class FileTraceManager implements TraceManager {
    private static String selfPath;
    private static String metaPath;

    public FileTraceManager(final StorageProp op) {
        selfPath = op.getDataPath();
        File selfFile = new File(selfPath);
        if (!selfFile.exists()) {
            boolean mkdir = selfFile.mkdirs();
            // do nothing for mkdir result
        }

        FileTraceManager.metaPath = selfPath + File.separator + "trace";
        File metaPath = new File(FileTraceManager.metaPath);
        if (!metaPath.exists()) {
            boolean mkdir = metaPath.mkdirs();
            // do nothing for mkdir result
        }
    }

    @Override
    public void save(final String name, final List<String> contents) {
        FileOutputStream mateOut = null;
        try {
            String path = metaPath + File.separator + name + System.currentTimeMillis();
            mateOut = new FileOutputStream(path);

            IOUtils.writeLines(contents, System.lineSeparator(), mateOut, StandardCharsets.UTF_8);
            mateOut.flush();
        } catch (IOException e) {
            throw new StorageException("save trace, " + e.getMessage(), e);
        } finally {
            StreamUtil.close(mateOut);
        }
    }

    @Override
    public void shutdown() {

    }
}
