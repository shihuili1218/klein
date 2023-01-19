
package com.ofcoder.klein.jepsen.control.jepsen.core;

public interface Database {
    /**
     * This method should be used to first install a database server onto the given node, and then launch it so that it is ready to receive operations.
     */
    public Object setUpDatabase(String node);

    /**
     * Conversely to {@code setUpDatabase(Object, String)}, this method should first stop the database server, and then uninstall it.
     */
    public void teardownDatabase(String node);
}
