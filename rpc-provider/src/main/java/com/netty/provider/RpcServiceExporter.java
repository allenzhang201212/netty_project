package com.netty.provider;

import com.netty.core.ServiceInvoker;
import com.netty.core.annotation.RpcService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class RpcServiceExporter implements BeanPostProcessor {
    private final ServiceInvoker invoker;
    public RpcServiceExporter(ServiceInvoker invoker) { this.invoker = invoker; }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();
        RpcService anno = clazz.getAnnotation(RpcService.class);
        if (anno != null) {
            if (anno.value() != void.class) {
                invoker.addService(anno.value(), bean);
            } else {
                Class<?>[] ifaces = clazz.getInterfaces();
                for (Class<?> iface : ifaces) {
                    invoker.addService(iface, bean);
                }
            }
        }
        return bean;
    }
}