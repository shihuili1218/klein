package com.ofcoder.klein.common.util.pojo;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.common.util.RepeatedTimerTest;

import junit.framework.TestCase;

/**
 * @author far.liu
 */
public class PojoUtilsTest extends TestCase {
    private static final Logger LOG = LoggerFactory.getLogger(PojoUtilsTest.class);

    public void testGeneralize() {
        Map<String, List<Object>> map = new HashMap<String, List<Object>>();

        List<Object> list = new ArrayList<Object>();
        list.add(new Person());

        map.put("k", list);

        Object generalize = PojoUtils.generalize(map);
        LOG.info("generalize: {}, {}",generalize.getClass(), generalize);
        Object realize = PojoUtils.realize(generalize, Map.class);
        assertEquals(map, realize);
        assertTrue(map.get("k") instanceof List);
        assertTrue(map.get("k").get(0) instanceof Person);
    }
}