package com.ofcoder.klein.consensus.facade;

import java.io.Serializable;

public interface Command extends Serializable {
    Object getData();
}
