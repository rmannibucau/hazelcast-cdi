package com.github.rmannibucau.hazelcast.cdi.internal;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

import com.github.rmannibucau.hazelcast.cdi.api.HzIdGenerator;
import com.github.rmannibucau.hazelcast.cdi.api.HzInstance;
import com.github.rmannibucau.hazelcast.cdi.api.HzList;
import com.github.rmannibucau.hazelcast.cdi.api.HzLock;
import com.github.rmannibucau.hazelcast.cdi.api.HzMap;
import com.github.rmannibucau.hazelcast.cdi.api.HzMultiMap;
import com.github.rmannibucau.hazelcast.cdi.api.HzQueue;
import com.github.rmannibucau.hazelcast.cdi.api.HzSemaphore;
import com.github.rmannibucau.hazelcast.cdi.api.HzSet;
import com.github.rmannibucau.hazelcast.cdi.api.HzTopic;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IList;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ISemaphore;
import com.hazelcast.core.ISet;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.IdGenerator;
import com.hazelcast.core.MultiMap;

public class HazelcastExtension implements Extension {

    private static final String DEFAULT_INSTANCE_NAME = HazelcastExtension.class.getName();

    private final Set<ProvidedInstanceBean<?>> beans = new HashSet<>();

    private final Collection<HazelcastInstance> instances = new CopyOnWriteArrayList<>();

    void afterDeploymentValidation(@Observes final AfterDeploymentValidation event) {
        // now CDI context exists just initialize factories. This allow to use CDI with MapStores.
        for (final ProvidedInstanceBean<?> bean : beans) {
            bean.getValue().init();
        }
        beans.clear();
    }

    void afterBeanDiscovery(@Observes final AfterBeanDiscovery event) {
        for (final Bean<?> bean : beans) {
            try {
                event.addBean(bean);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    <X> void processAnnotatedType(@Observes final ProcessAnnotatedType<X> event, final BeanManager beanManager) {
        final AnnotatedType<X> baseClass = event.getAnnotatedType();
        for (final AnnotatedField<?> field : baseClass.getFields()) {
            if (field.isAnnotationPresent(HzMap.class)) {
                final HzMap config = field.getAnnotation(HzMap.class);
                final String name = value(config.value(), field.getDeclaringType().getJavaClass().getName());

                beans.add(new ProvidedInstanceBean<>(new HazelcastValue<IMap<?, ?>>(config.instance()) {

                    @Override
                    protected IMap<?, ?> createInstance() {
                        return instance().getMap(name);
                    }
                }, beanManager, field));
            } else if (field.isAnnotationPresent(HzList.class)) {
                final HzList config = field.getAnnotation(HzList.class);
                final String name = value(config.value(), field.getDeclaringType().getJavaClass().getName());
                beans.add(new ProvidedInstanceBean<>(new HazelcastValue<IList<?>>(config.instance()) {

                    @Override
                    protected IList<?> createInstance() {
                        return instance().getList(name);
                    }
                }, beanManager, field));
            } else if (field.isAnnotationPresent(HzSet.class)) {
                final HzSet config = field.getAnnotation(HzSet.class);
                final String name = value(config.value(), field.getDeclaringType().getJavaClass().getName());
                beans.add(new ProvidedInstanceBean<>(new HazelcastValue<ISet<?>>(config.instance()) {

                    @Override
                    protected ISet<?> createInstance() {
                        return instance().getSet(name);
                    }
                }, beanManager, field));
            } else if (field.isAnnotationPresent(HzMultiMap.class)) {
                final HzMultiMap config = field.getAnnotation(HzMultiMap.class);
                final String name = value(config.value(), field.getDeclaringType().getJavaClass().getName());
                beans.add(new ProvidedInstanceBean<>(new HazelcastValue<MultiMap<?, ?>>(config.instance()) {

                    @Override
                    protected MultiMap<?, ?> createInstance() {
                        return instance().getMultiMap(name);
                    }
                }, beanManager, field));
            } else if (field.isAnnotationPresent(HzLock.class)) {
                final HzLock config = field.getAnnotation(HzLock.class);
                final String name = value(config.value(), field.getDeclaringType().getJavaClass().getName());
                beans.add(new ProvidedInstanceBean<>(new HazelcastValue<ILock>(config.instance()) {

                    @Override
                    protected ILock createInstance() {
                        return instance().getLock(name);
                    }
                }, beanManager, field));
            } else if (field.isAnnotationPresent(HzQueue.class)) {
                final HzQueue config = field.getAnnotation(HzQueue.class);
                final String name = value(config.value(), field.getDeclaringType().getJavaClass().getName());
                beans.add(new ProvidedInstanceBean<>(new HazelcastValue<IQueue<?>>(config.instance()) {

                    @Override
                    protected IQueue<?> createInstance() {
                        return instance().getQueue(name);
                    }
                }, beanManager, field));
            } else if (field.isAnnotationPresent(HzSemaphore.class)) {
                final HzSemaphore config = field.getAnnotation(HzSemaphore.class);
                final String name = value(config.value(), field.getDeclaringType().getJavaClass().getName());
                beans.add(new ProvidedInstanceBean<>(new HazelcastValue<ISemaphore>(config.instance()) {

                    @Override
                    protected ISemaphore createInstance() {
                        return instance().getSemaphore(name);
                    }
                }, beanManager, field));
            } else if (field.isAnnotationPresent(HzTopic.class)) {
                final HzTopic config = field.getAnnotation(HzTopic.class);
                final String name = value(config.value(), field.getDeclaringType().getJavaClass().getName());
                beans.add(new ProvidedInstanceBean<>(new HazelcastValue<ITopic<?>>(config.instance()) {

                    @Override
                    protected ITopic<?> createInstance() {
                        return instance().getTopic(name);
                    }
                }, beanManager, field));
            } else if (field.isAnnotationPresent(HzInstance.class)) {
                final HzInstance config = field.getAnnotation(HzInstance.class);
                beans.add(new ProvidedInstanceBean<>(new HazelcastValue<com.hazelcast.core.HazelcastInstance>(config.value()) {

                    @Override
                    protected com.hazelcast.core.HazelcastInstance createInstance() {
                        return instance();
                    }
                }, beanManager, field));
            } else if (field.isAnnotationPresent(HzIdGenerator.class)) {
                final HzIdGenerator config = field.getAnnotation(HzIdGenerator.class);
                final String name = value(config.value(), field.getDeclaringType().getJavaClass().getName());
                beans.add(new ProvidedInstanceBean<>(new HazelcastValue<IdGenerator>(config.instance()) {

                    @Override
                    protected IdGenerator createInstance() {
                        return instance().getIdGenerator(name);
                    }
                }, beanManager, field));
            } // else: add other hazelcast components
        }
    }

    void shutdown(@Observes final BeforeShutdown shutdown) {
        for (final com.hazelcast.core.HazelcastInstance i : instances) {
            i.getLifecycleService().shutdown();
        }
        instances.clear();
    }

    private static String value(final String value, final String defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    private abstract class HazelcastValue<T> implements ProvidedInstanceBean.Value<T> {

        private final String instanceName;

        protected T value;

        private HazelcastValue(final String instanceName) {
            this.instanceName = instanceName;
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public void init() {
            value = createInstance();
        }

        protected abstract T createInstance();

        protected com.hazelcast.core.HazelcastInstance instance() {
            final String name = value(instanceName, DEFAULT_INSTANCE_NAME);
            com.hazelcast.core.HazelcastInstance instance = Hazelcast.getHazelcastInstanceByName(name);
            if (instance == null) {
                instance = Hazelcast.newHazelcastInstance(new XmlConfigBuilder().build().setInstanceName(name));
                instances.add(instance);
            }
            return instance;
        }
    }
}
