package com.netty.core;

import com.netty.common.RpcRequest;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceInvoker {
    private final ConcurrentHashMap<String, Object> services = new ConcurrentHashMap<>();

    public void addService(Class<?> iface, Object impl) {
        services.put(iface.getName(), impl);
    }

    public Object invoke(RpcRequest req) throws Exception {
        Object impl = services.get(req.service);
        if (impl == null)
            throw new IllegalStateException("No service: " + req.service);
        Method m = impl.getClass().getMethod(req.method, req.paramTypes);
        return m.invoke(impl, req.args);
    }
}