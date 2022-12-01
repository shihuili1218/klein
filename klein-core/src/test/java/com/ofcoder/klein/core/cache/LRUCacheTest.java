package com.ofcoder.klein.core.cache;

import org.junit.Assert;

import junit.framework.TestCase;

public class LRUCacheTest extends TestCase {

    public void test_noUse() {
        int initialCapacity = 3;
        LRUCache cache = new LRUCache(initialCapacity);
        cache.put("4", 4);
        cache.put("3", 4);
        cache.put("1", 4);
        cache.put("2", 4);
        Assert.assertEquals(initialCapacity, cache.size());
        Assert.assertFalse(cache.containsKey("4"));
        Assert.assertTrue(cache.containsKey("3"));
        Assert.assertTrue(cache.containsKey("1"));
        Assert.assertTrue(cache.containsKey("2"));
    }

    public void test_used() {
        int initialCapacity = 3;
        LRUCache cache = new LRUCache(initialCapacity);
        cache.put("4", 4);
        cache.put("3", 4);
        cache.put("1", 4);
        cache.get("4");
        cache.put("2", 4);
        Assert.assertEquals(initialCapacity, cache.size());
        Assert.assertFalse(cache.containsKey("3"));
        Assert.assertTrue(cache.containsKey("4"));
        Assert.assertTrue(cache.containsKey("1"));
        Assert.assertTrue(cache.containsKey("2"));
    }

}