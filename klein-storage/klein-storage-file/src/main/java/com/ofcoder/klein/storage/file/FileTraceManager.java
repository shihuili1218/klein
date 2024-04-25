package com.ofcoder.klein.storage.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.io.IOUtils;

import com.ofcoder.klein.common.util.StreamUtil;
import com.ofcoder.klein.common.util.SystemPropertyUtil;
import com.ofcoder.klein.spi.Join;
import com.ofcoder.klein.storage.facade.TraceManager;
import com.ofcoder.klein.storage.facade.config.StorageProp;
import com.ofcoder.klein.storage.facade.exception.StorageException;

@Join
public class FileTraceManager implements TraceManager {
    private static String selfPath;
    private static final String BASE_PATH = SystemPropertyUtil.get("klein.data-path", SystemPropertyUtil.get("user.dir", "") + File.separator + "data");
    private static String metaPath;

    public FileTraceManager(final StorageProp op) {

        selfPath = BASE_PATH + File.separator + op.getId();
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
    public void save(final List<String> contents) {
        FileOutputStream mateOut = null;
        try {
            String name = metaPath + File.separator + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(0, 100);
            mateOut = new FileOutputStream(name);

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
