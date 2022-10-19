package com.ofcoder.klein.storage.facade;


import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.spi.SPI;
import com.ofcoder.klein.storage.facade.config.StorageProp;

/**
 * @author far.liu
 */
@SPI
public interface StorageEngine extends Lifecycle<StorageProp> {
    static void startup(String rpc, StorageProp prop) {

    }
}
