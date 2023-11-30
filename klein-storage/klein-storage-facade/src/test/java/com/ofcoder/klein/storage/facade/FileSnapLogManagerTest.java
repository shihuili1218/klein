package com.ofcoder.klein.storage.facade;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.junit.Test;

import junit.framework.TestCase;

public class FileSnapLogManagerTest extends TestCase {


    @Test
    public void test() throws IOException {
        String path = Thread.currentThread().getContextClassLoader().getResource("").getPath() + "data";
        System.out.println(path);

        path = this.getClass().getResource("/").getPath();
        System.out.println(path);

        path = this.getClass().getResource("").getPath();
        System.out.println(path);

        File file = new File("");
        path = file.getCanonicalPath();
        System.out.println(path);

        URL path1 = this.getClass().getResource("");
        System.out.println(path1);

        path = System.getProperty("user.dir");
        System.out.println(path);

    }
}