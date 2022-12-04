package com.ofcoder.klein.core.cache;

import org.junit.Assert;

import junit.framework.TestCase;

public class LRUMapTest extends TestCase {

    public void test_noUse() {
        int initialCapacity = 3;
        CacheSM.LRUMap cache = new CacheSM.LRUMap(initialCapacity);
        cache.put("4", 4);
        cache.put("3", 4);
        cache.put("1", 4);
        cache.put("2", 4);
        Assert.assertFalse(cache.exist("4"));
        Assert.assertTrue(cache.exist("3"));
        Assert.assertTrue(cache.exist("1"));
        Assert.assertTrue(cache.exist("2"));
    }

    public void test_used() {
        int initialCapacity = 3;
        CacheSM.LRUMap cache = new CacheSM.LRUMap(initialCapacity);
        cache.put("4", 4);
        cache.put("3", 4);
        cache.put("1", 4);
        cache.get("4");
        cache.put("2", 4);
        Assert.assertFalse(cache.exist("3"));
        Assert.assertTrue(cache.exist("4"));
        Assert.assertTrue(cache.exist("1"));
        Assert.assertTrue(cache.exist("2"));
    }

}