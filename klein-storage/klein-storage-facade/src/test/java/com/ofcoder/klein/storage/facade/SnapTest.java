package com.ofcoder.klein.storage.facade;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SnapTest {

    @Test
    public void testGetCheckpoint() {
        Snap snap = new Snap();
        snap.setCheckpoint(100L);
        assertEquals(100L, snap.getCheckpoint());
    }
}
