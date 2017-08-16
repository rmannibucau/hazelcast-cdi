package com.github.rmannibucau.hazelcast.cdi.internal;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.BeanManager;

class ProvidedInstanceBean<T> extends AbstractBean<T> {
    private Value<T> instance;

    public ProvidedInstanceBean(final Value<T> object, final BeanManager beanManager, final Annotated type) {
        super(beanManager, type, type.getBaseType());
        instance = object;
    }

    @Override
    public T create(final CreationalContext<T> tCreationalContext) {
        return instance.get();
    }

    @Override
    public void destroy(final T instance, final CreationalContext<T> tCreationalContext) {
        // nothing
    }

    public Value<T> getValue() {
        return instance;
    }

    public interface Value<T> {
        T get();
        void init();
    }

    public static class ValueImpl<T> implements Value<T> {
        private final T t;

        public ValueImpl(final T t) {
            this.t = t;
        }

        @Override
        public T get() {
            return t;
        }

        @Override
        public void init() {
            // no-op
        }
    }
}
