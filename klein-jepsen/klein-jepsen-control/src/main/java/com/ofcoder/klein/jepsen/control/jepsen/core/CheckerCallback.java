package com.ofcoder.klein.jepsen.control.jepsen.core;

public interface CheckerCallback {
    /** 
     * Method which the user uses to define the behavior of the checker
     * @param test Contains information about the test
     * @param history Lists the operations performed by the test as well as its result in sequential order
     */
    public void check(Object test, Object history);
}
