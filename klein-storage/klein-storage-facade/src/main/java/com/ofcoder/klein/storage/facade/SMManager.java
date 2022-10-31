package com.ofcoder.klein.storage.facade;

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.spi.SPI;
import com.ofcoder.klein.storage.facade.config.StorageProp;

/**
 * @author 释慧利
 */
@SPI

public interface SMManager extends Lifecycle<StorageProp> {

    void saveSnap(Snap snap);

    Snap getLastSnap();

}
