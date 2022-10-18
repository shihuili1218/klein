package com.ofcoder.klein.common.util;

import java.io.Closeable;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author far.liu
 */
public class StreamUtil {

    private static final Logger LOG = LoggerFactory.getLogger(StreamUtil.class);

    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                LOG.warn(e.getMessage(), e);
            }
        }
    }
}
