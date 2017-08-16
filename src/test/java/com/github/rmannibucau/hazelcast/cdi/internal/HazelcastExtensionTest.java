package com.github.rmannibucau.hazelcast.cdi.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

import org.apache.meecrowave.junit.MeecrowaveRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.github.rmannibucau.hazelcast.cdi.api.HzMap;
import com.github.rmannibucau.hazelcast.cdi.api.HzInstance;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

public class HazelcastExtensionTest {

    @ClassRule
    public static final TestRule CONTAINER = new MeecrowaveRule();

    @Test
    public void ensureInjectionsExist() {
        final Injected injected = CDI.current().select(Injected.class).get();
        assertNotNull(injected.getTheMap());

        final Set<HazelcastInstance> allHazelcastInstances = Hazelcast.getAllHazelcastInstances();
        assertEquals(1, allHazelcastInstances.size());
        final HazelcastInstance instance = allHazelcastInstances.iterator().next();
        final IMap<String, String> imap = instance.getMap("the-map");
        imap.put("test", "value");
        assertEquals("value", injected.getTheMap().get("test"));

        assertEquals(instance, injected.getInstance());
    }

    @ApplicationScoped
    public static class Injected {

        @Inject
        @HzMap("the-map")
        private Map<String, String> theMap;

        @Inject
        @HzInstance
        private HazelcastInstance instance;

        public HazelcastInstance getInstance() {
            return instance;
        }

        public Map<String, String> getTheMap() {
            return theMap;
        }
    }
}
