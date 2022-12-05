package com.ofcoder.klein.storage.facade.config;

import com.ofcoder.klein.common.util.SystemPropertyUtil;

/**
 * @author far.liu
 */
public class StorageProp {
    private String id = SystemPropertyUtil.get("klein.id", "1");

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
