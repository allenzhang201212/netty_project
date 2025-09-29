package com.netty.consumer;

import com.netty.core.ProxyFactory;
import com.netty.core.annotation.RpcReference;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Component
public class RpcReferenceInjector implements BeanPostProcessor {
    private final ProxyFactory factory;
    public RpcReferenceInjector(ProxyFactory factory) { this.factory = factory; }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        for (Field f : bean.getClass().getDeclaredFields()) {
            RpcReference ref = f.getAnnotation(RpcReference.class);
            if (ref != null) {
                Class<?> iface = (ref.interfaceClass() != void.class) ? ref.interfaceClass() : f.getType();
                Object proxy = factory.create(iface);
                boolean old = f.canAccess(bean);
                f.setAccessible(true);
                try { f.set(bean, proxy); } catch (IllegalAccessException e) { throw new RuntimeException(e); }
                f.setAccessible(old);
            }
        }
        return bean;
    }
}