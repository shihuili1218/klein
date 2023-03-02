package com.ofcoder.klein.consensus.facade;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.util.RpcUtil;
import junit.framework.TestCase;

public class MemberConfigurationTest extends TestCase {

    public void testInit() {
        List<Endpoint> nodes = ImmutableList.of(RpcUtil.parseEndpoint("1:127.0.0.1:1218"),
                RpcUtil.parseEndpoint("2:127.0.0.1:1219"),
                RpcUtil.parseEndpoint("3:127.0.0.1:1220"));
        MemberConfiguration configuration = new MemberConfiguration();
        configuration.init(null, nodes);

        assertEquals(configuration.effectMembers.size(), nodes.size());
        nodes.forEach(it -> assertEquals(it, configuration.effectMembers.get(it.getId())));
    }

    public void testSeenNewConfig() {
        List<Endpoint> nodes = new ArrayList<>();
        nodes.add(RpcUtil.parseEndpoint("1:127.0.0.1:1218"));
        nodes.add(RpcUtil.parseEndpoint("2:127.0.0.1:1219"));
        nodes.add(RpcUtil.parseEndpoint("3:127.0.0.1:1220"));

        MemberConfiguration configuration = new MemberConfiguration();
        configuration.init(null, nodes);

        nodes.add(RpcUtil.parseEndpoint("4:127.0.0.1:1221"));
        configuration.seenNewConfig(new HashSet<>(nodes));

        assertEquals(configuration.effectMembers.size(), nodes.size() - 1);
        assertEquals(configuration.lastMembers.size(), nodes.size());
        nodes.forEach(it -> assertEquals(it, configuration.lastMembers.get(it.getId())));
    }

    public void testGetEndpointById() {
        List<Endpoint> nodes = new ArrayList<>();
        Endpoint effect = RpcUtil.parseEndpoint("1:127.0.0.1:1218");
        nodes.add(effect);
        nodes.add(RpcUtil.parseEndpoint("2:127.0.0.1:1219"));
        nodes.add(RpcUtil.parseEndpoint("3:127.0.0.1:1220"));

        MemberConfiguration configuration = new MemberConfiguration();
        configuration.init(null, nodes);

        Endpoint last = RpcUtil.parseEndpoint("4:127.0.0.1:1221");
        nodes.add(last);
        configuration.seenNewConfig(new HashSet<>(nodes));

        assertEquals(configuration.effectMembers.size(), nodes.size() - 1);
        assertEquals(configuration.lastMembers.size(), nodes.size());

        Endpoint emptyPoint = configuration.getEndpointById("");
        assertNull(emptyPoint);

        Endpoint effectTarget = configuration.getEndpointById("1");
        assertEquals(effectTarget, effect);

        Endpoint lastTarget = configuration.getEndpointById("4");
        assertEquals(lastTarget, last);

        Endpoint nullTarget = configuration.getEndpointById("5");
        assertNull(nullTarget);
    }


    public void testIsValid() {
        List<Endpoint> nodes = new ArrayList<>();
        Endpoint effect = RpcUtil.parseEndpoint("1:127.0.0.1:1218");
        nodes.add(effect);
        nodes.add(RpcUtil.parseEndpoint("2:127.0.0.1:1219"));
        nodes.add(RpcUtil.parseEndpoint("3:127.0.0.1:1220"));

        MemberConfiguration configuration = new MemberConfiguration();
        configuration.init(null, nodes);

        Endpoint last = RpcUtil.parseEndpoint("4:127.0.0.1:1221");
        nodes.add(last);
        configuration.seenNewConfig(new HashSet<>(nodes));

        assertEquals(configuration.effectMembers.size(), nodes.size() - 1);
        assertEquals(configuration.lastMembers.size(), nodes.size());

        assertTrue(configuration.isValid("1"));
        assertTrue(configuration.isValid("4"));
        assertFalse(configuration.isValid("-1"));
    }

    public void testGetMembersWithout() {
        List<Endpoint> nodes = new ArrayList<>();
        Endpoint effect = RpcUtil.parseEndpoint("1:127.0.0.1:1218");
        nodes.add(effect);
        nodes.add(RpcUtil.parseEndpoint("2:127.0.0.1:1219"));
        nodes.add(RpcUtil.parseEndpoint("3:127.0.0.1:1220"));

        MemberConfiguration configuration = new MemberConfiguration();
        configuration.init(null, nodes);

        Endpoint last = RpcUtil.parseEndpoint("4:127.0.0.1:1221");
        nodes.add(last);
        configuration.seenNewConfig(new HashSet<>(nodes));

        assertEquals(configuration.effectMembers.size(), nodes.size() - 1);
        assertEquals(configuration.lastMembers.size(), nodes.size());

        assertEquals(configuration.getMembersWithout("1").size(), 3);
        assertTrue(configuration.getMembersWithout("1").contains(last));
        assertFalse(configuration.getMembersWithout("1").contains(effect));
    }

    public void testGetLastMembers() {
        List<Endpoint> nodes = new ArrayList<>();
        Endpoint effect = RpcUtil.parseEndpoint("1:127.0.0.1:1218");
        nodes.add(effect);
        nodes.add(RpcUtil.parseEndpoint("2:127.0.0.1:1219"));
        nodes.add(RpcUtil.parseEndpoint("3:127.0.0.1:1220"));

        MemberConfiguration configuration = new MemberConfiguration();
        configuration.init(null, nodes);

        Endpoint last = RpcUtil.parseEndpoint("4:127.0.0.1:1221");
        nodes.add(last);
        configuration.seenNewConfig(new HashSet<>(nodes));

        assertEquals(configuration.getLastMembers().size(), nodes.size());
    }

    public void testGetEffectMembers() {
        List<Endpoint> nodes = new ArrayList<>();
        Endpoint effect = RpcUtil.parseEndpoint("1:127.0.0.1:1218");
        nodes.add(effect);
        nodes.add(RpcUtil.parseEndpoint("2:127.0.0.1:1219"));
        nodes.add(RpcUtil.parseEndpoint("3:127.0.0.1:1220"));

        MemberConfiguration configuration = new MemberConfiguration();
        configuration.init(null, nodes);

        Endpoint last = RpcUtil.parseEndpoint("4:127.0.0.1:1221");
        nodes.add(last);
        configuration.seenNewConfig(new HashSet<>(nodes));

        assertEquals(configuration.getEffectMembers().size(), nodes.size() - 1);
    }

    public void testEffectiveNewConfig() {
        List<Endpoint> nodes = new ArrayList<>();
        Endpoint effect = RpcUtil.parseEndpoint("1:127.0.0.1:1218");
        nodes.add(effect);
        nodes.add(RpcUtil.parseEndpoint("2:127.0.0.1:1219"));
        nodes.add(RpcUtil.parseEndpoint("3:127.0.0.1:1220"));

        MemberConfiguration configuration = new MemberConfiguration();
        configuration.init(null, nodes);

        Endpoint last = RpcUtil.parseEndpoint("4:127.0.0.1:1221");
        nodes.add(last);
        int version = configuration.seenNewConfig(new HashSet<>(nodes));
        configuration.effectiveNewConfig(version, new HashSet<>(nodes));

        assertEquals(configuration.getEffectMembers().size(), nodes.size());
        assertEquals(configuration.getLastMembers().size(), 0);
        assertEquals(configuration.version.get(), version + 1);
    }
}