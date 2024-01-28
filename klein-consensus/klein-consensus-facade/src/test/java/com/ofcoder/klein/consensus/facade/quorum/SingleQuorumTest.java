package com.ofcoder.klein.consensus.facade.quorum;

import java.util.HashSet;
import java.util.Set;

import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.util.RpcUtil;
import junit.framework.TestCase;

public class SingleQuorumTest extends TestCase {

    public void testIsGranted() {
        Set<Endpoint> members = new HashSet<>();
        members.add(RpcUtil.parseEndpoint("1:127.0.0.1:1218:false"));
        members.add(RpcUtil.parseEndpoint("2:127.0.0.1:1219:false"));
        members.add(RpcUtil.parseEndpoint("3:127.0.0.1:1220:false"));

        SingleQuorum passQuorum = new SingleQuorum(members, 2);
        passQuorum.grant(RpcUtil.parseEndpoint("1:127.0.0.1:1218:false"));
        assertEquals(passQuorum.isGranted(), Quorum.GrantResult.GRANTING);

        passQuorum.grant(RpcUtil.parseEndpoint("4:127.0.0.1:1221:false"));
        assertEquals(passQuorum.isGranted(), Quorum.GrantResult.GRANTING);

        passQuorum.grant(RpcUtil.parseEndpoint("2:127.0.0.1:1219:false"));
        assertEquals(passQuorum.isGranted(), Quorum.GrantResult.PASS);

        SingleQuorum refuseQuorum = new SingleQuorum(members, 2);
        refuseQuorum.refuse(RpcUtil.parseEndpoint("1:127.0.0.1:1218:false"));
        assertEquals(refuseQuorum.isGranted(), Quorum.GrantResult.GRANTING);

        refuseQuorum.refuse(RpcUtil.parseEndpoint("1:127.0.0.1:1218:false"));
        assertEquals(refuseQuorum.isGranted(), Quorum.GrantResult.GRANTING);

        refuseQuorum.refuse(RpcUtil.parseEndpoint("2:127.0.0.1:1219:false"));
        assertEquals(refuseQuorum.isGranted(), Quorum.GrantResult.REFUSE);

    }

    public void testGrant() {
    }

    public void testRefuse() {
    }
}