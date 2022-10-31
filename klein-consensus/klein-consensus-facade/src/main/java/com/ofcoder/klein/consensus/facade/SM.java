package com.ofcoder.klein.consensus.facade;

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.storage.facade.Snap;
import com.ofcoder.klein.storage.facade.StorageEngine;

/**
 * @author 释慧利
 */
public interface SM extends Lifecycle {

    Object apply(long instanceId, Object data);

    Snap snapshot();

    void loadSnap(Snap snap);

}
