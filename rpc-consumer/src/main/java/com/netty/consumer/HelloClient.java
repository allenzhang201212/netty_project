package com.netty.consumer;

import com.netty.common.HelloService;
import com.netty.core.annotation.RpcReference;
import org.springframework.stereotype.Component;

@Component
public class HelloClient {
    @RpcReference
    private HelloService helloService;

    public String call(String name) {
        return helloService.hello(name);
    }
}