package com.ofcoder.klein.consensus.facade.sm;

import com.ofcoder.klein.storage.facade.Snap;

/**
 * @author 释慧利
 */
public interface SM {

    Object apply(long instanceId, Object data);

    Snap snapshot();

    void loadSnap(Snap snap);

}
