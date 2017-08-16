package com.github.rmannibucau.hazelcast.cdi.internal;

import static java.util.Collections.emptySet;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Named;

abstract class AbstractBean<T> implements Bean<T> {

    protected static final AnnotationLiteral<Any> ANY_LITTERAL = new AnnotationLiteral<Any>() {
    };

    protected static final AnnotationLiteral<Default> DEFAULT_LITTERAL = new AnnotationLiteral<Default>() {
    };

    protected Class<?> beanClass;

    protected Set<InjectionPoint> injectionPoints = null;

    protected Set<Annotation> qualifiers;

    protected Set<Class<? extends Annotation>> stereotypes;

    protected Set<Type> types;

    protected Class<? extends Annotation> scope;

    protected String name;

    protected boolean alternative;

    AbstractBean(final BeanManager beanManager, final Annotated type, final Type clazz) {
        beanClass = realClass(clazz);
        alternative = false;
        qualifiers = new HashSet<Annotation>();
        stereotypes = new HashSet<Class<? extends Annotation>>();
        types = new HashSet<Type>();

        for (Annotation annotation : type.getAnnotations()) {
            if (beanManager.isQualifier(annotation.annotationType())) {
                qualifiers.add(annotation);
            } else if (beanManager.isScope(annotation.annotationType())) {
                scope = annotation.annotationType();
            } else if (beanManager.isStereotype(annotation.annotationType())) {
                stereotypes.add(annotation.annotationType());
            }

            if (annotation instanceof Named) {
                name = ((Named) annotation).value();
            }

            if (annotation instanceof Alternative) {
                alternative = true;
            }
        }

        if (scope == null) {
            scope = Dependent.class;
        }

        if (beanClass != null) {
            for (Class<?> c = beanClass; c != Object.class && c != null; c = c.getSuperclass()) {
                types.add(c);
            }
            types.addAll(Arrays.asList(beanClass.getInterfaces()));
        }

        if (!clazz.equals(beanClass)) {
            types.add(clazz);
        }

        if (qualifiers.isEmpty()) {
            qualifiers.add(DEFAULT_LITTERAL);
        }
        qualifiers.add(ANY_LITTERAL);

        // can be overridden in children
        injectionPoints = emptySet();
    }

    private static Class<?> realClass(final Type type) {
        final Class<?> raw;
        if (type instanceof ParameterizedType) {
            raw = (Class<?>) ((ParameterizedType) type).getRawType();
        } else if (type instanceof Class) {
            raw = (Class<?>) type;
        } else if (type instanceof GenericArrayType) {
            raw = realClass(((GenericArrayType) type).getGenericComponentType());
        } else {
            raw = null;
        }
        return raw;
    }

    @Override
    public Set<Type> getTypes() {
        return types;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return scope;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return injectionPoints;
    }

    @Override
    public Class<?> getBeanClass() {
        return beanClass;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return stereotypes;
    }

    @Override
    public boolean isAlternative() {
        return alternative;
    }
}
