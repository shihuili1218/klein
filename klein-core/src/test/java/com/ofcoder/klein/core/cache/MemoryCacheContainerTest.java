package com.ofcoder.klein.core.cache;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import junit.framework.TestCase;

public class MemoryCacheContainerTest extends TestCase {

    MemoryCacheContainer cache;

    public void test_MemoryMap_NoUse() {
        int initialCapacity = 3;
        LruCacheContainer.MemoryMap cache = new LruCacheContainer.MemoryMap(initialCapacity);
        cache.put("4", 4);
        cache.put("3", 4);
        cache.put("1", 4);
        cache.put("2", 4);
        Assert.assertFalse(cache.containsKey("4"));
        Assert.assertTrue(cache.containsKey("3"));
        Assert.assertTrue(cache.containsKey("1"));
        Assert.assertTrue(cache.containsKey("2"));
    }

    public void test_MemoryMap_Used() {
        int initialCapacity = 3;
        LruCacheContainer.MemoryMap cache = new LruCacheContainer.MemoryMap(initialCapacity);
        cache.put("4", 4);
        cache.put("3", 4);
        cache.put("1", 4);
        cache.get("4");
        cache.put("2", 4);
        Assert.assertFalse(cache.containsKey("3"));
        Assert.assertTrue(cache.containsKey("4"));
        Assert.assertTrue(cache.containsKey("1"));
        Assert.assertTrue(cache.containsKey("2"));
    }

    @Before
    public void setUp() {
        cache = new MemoryCacheContainer();
    }

    @Test
    public void testContainsKey_ExistingKey_Expired() {
        // Setup
        String key = "key";
        Long expire = 0L; // Expired
        Object data = new Object();
        cache._put(key, data, expire);

        // Execute
        boolean result = cache.containsKey(key);

        // Verify
        assertFalse(result);
    }

    @Test
    public void testContainsKey_NonExistingKey() {
        // Setup
        String key = "key";

        // Execute
        boolean result = cache.containsKey(key);

        // Verify
        assertFalse(result);
    }

    @Test
    public void testContainsKey_NullValue() {
        // Setup
        String key = "key";
        cache._put(key, null, 1000L);

        // Execute
        boolean result = cache.containsKey(key);

        // Verify
        assertFalse(result);
    }

    @Test
    public void testContainsKey_ExpiredAndNullValue() {
        // Setup
        String key = "key";
        cache._put(key, null, 0L);

        // Execute
        boolean result = cache.containsKey(key);

        // Verify
        assertFalse(result);
    }
}
