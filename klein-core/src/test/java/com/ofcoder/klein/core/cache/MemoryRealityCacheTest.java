package com.ofcoder.klein.core.cache;

import org.junit.Assert;

import junit.framework.TestCase;

public class MemoryRealityCacheTest extends TestCase {

    public void test_MemoryMap_NoUse() {
        int initialCapacity = 3;
        LruRealityCache.MemoryMap cache = new LruRealityCache.MemoryMap(initialCapacity);
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
        LruRealityCache.MemoryMap cache = new LruRealityCache.MemoryMap(initialCapacity);
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

}