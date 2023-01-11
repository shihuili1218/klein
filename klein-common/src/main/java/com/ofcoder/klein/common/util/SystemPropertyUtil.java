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
package com.ofcoder.klein.common.util;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.logging.log4j.core.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A collection of utility methods to retrieve and
 * parse the values of the Java system properties.
 * Forked from <a href="https://github.com/sofastack/sofa-jraft">sofa-jraft</a>.
 */
public final class SystemPropertyUtil {

    private static final Logger LOG = LoggerFactory.getLogger(SystemPropertyUtil.class);

    private SystemPropertyUtil() {
    }

    /**
     * Returns {@code true} if and only if the system property
     * with the specified {@code key} exists.
     *
     * @param key check key
     * @return true: exist, false: not exist
     */
    public static boolean contains(final String key) {
        return get(key) != null;
    }

    /**
     * Returns the value of the Java system property with the
     * specified {@code key}, while falling back to {@code null}
     * if the property access fails.
     *
     * @param key get key
     * @return the property value or {@code null}
     */
    public static String get(final String key) {
        return get(key, null);
    }

    /**
     * Returns the value of the Java system property with the
     * specified {@code key}, while falling back to the specified
     * default value if the property access fails.
     *
     * @param key key
     * @param def default value
     * @return the property value.
     * {@code def} if there's no such property or if an access to the specified property is not allowed.
     */
    public static String get(final String key, final String def) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (key.isEmpty()) {
            throw new IllegalArgumentException("key must not be empty.");
        }

        String value = null;
        try {
            if (System.getSecurityManager() == null) {
                value = System.getProperty(key);
            } else {
                value = AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(key));
            }
        } catch (Exception e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Unable to retrieve a system property '{}'; default values will be used, {}.", key, e);
            }
        }

        if (value == null) {
            return def;
        }

        return value;
    }

    /**
     * Returns the value of the Java system property with the
     * specified {@code key}, while falling back to the specified
     * default value if the property access fails.
     *
     * @param key key
     * @param def default value, if there's no such property or if an access to the specified property is not allowed.
     * @return the property value.
     */
    public static boolean getBoolean(final String key, final boolean def) {
        String value = get(key);
        if (value == null) {
            return def;
        }

        value = value.trim().toLowerCase();
        if (value.isEmpty()) {
            return true;
        }

        if ("true".equals(value) || "yes".equals(value) || "1".equals(value)) {
            return true;
        }

        if ("false".equals(value) || "no".equals(value) || "0".equals(value)) {
            return false;
        }

        LOG.warn("Unable to parse the boolean system property '{}':{} - using the default value: {}.", key, value, def);

        return def;
    }

    /**
     * Returns the value of the Java system property with the
     * specified {@code key}, while falling back to the specified
     * default value if the property access fails.
     *
     * @param key key
     * @param def default value
     * @return the property value.
     * {@code def} if there's no such property or if an access to the specified property is not allowed.
     */
    public static int getInt(final String key, final int def) {
        String value = get(key);
        if (value == null) {
            return def;
        }

        value = value.trim().toLowerCase();
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            // ignored
        }

        LOG.warn("Unable to parse the integer system property '{}':{} - using the default value: {}.", key, value, def);

        return def;
    }

    /**
     * Returns the value of the Java system property with the
     * specified {@code key}, while falling back to the specified
     * default value if the property access fails.
     *
     * @param key key
     * @param def default value
     * @return the property value.
     * {@code def} if there's no such property or if an access to the specified property is not allowed.
     */
    public static long getLong(final String key, final long def) {
        String value = get(key);
        if (value == null) {
            return def;
        }

        value = value.trim().toLowerCase();
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            // ignored
        }

        LOG.warn("Unable to parse the long system property '{}':{} - using the default value: {}.", key, value,
                def);

        return def;
    }

    /**
     * Returns the value of the Java system property with the
     * specified {@code key}, while falling back to the specified
     * default value if the property access fails.
     *
     * @param key key
     * @param def default value
     * @return the property value.
     * {@code def} if there's no such property or if an access to the specified property is not allowed.
     */
    public static float getFloat(final String key, final float def) {
        String value = get(key);
        if (value == null) {
            return def;
        }

        value = value.trim().toLowerCase();
        try {
            return Float.parseFloat(value);
        } catch (Exception ignored) {
            // ignored
        }

        LOG.warn("Unable to parse the float system property '{}':{} - using the default value: {}.", key, value,
                def);

        return def;
    }

    /**
     * Sets the value of the Java system property with the specified {@code key}.
     *
     * @param key   key
     * @param value value
     * @return result
     */
    public static Object setProperty(final String key, final String value) {
        return System.getProperties().setProperty(key, value);
    }

}
