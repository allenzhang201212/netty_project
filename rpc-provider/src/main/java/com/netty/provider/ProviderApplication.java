package com.netty.provider;

import com.netty.common.HelloService;
import com.netty.core.RpcServer;
import com.netty.core.ServiceInvoker;
import com.netty.registry.zk.ZkRegistry;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(RpcProviderProps.class)
public class ProviderApplication {
    public static void main(String[] args) { SpringApplication.run(ProviderApplication.class, args); }

    @Bean
    public ZkRegistry zkRegistry(RpcProviderProps props) {
        return new ZkRegistry(props.getZk());
    }

    @Bean
    public ServiceInvoker serviceInvoker() {
        ServiceInvoker invoker = new ServiceInvoker();
        invoker.addService(HelloService.class, new HelloServiceImpl());
        return invoker;
    }

    /** 由 Spring 管理生命周期：initMethod=start, destroyMethod=stop */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public RpcServer rpcServer(RpcProviderProps props, ServiceInvoker invoker) {
        return new RpcServer(props.getPort(), invoker);
    }

    /** 启动完成后向 ZK 注册 */
    @Bean
    public ApplicationRunner runner(RpcProviderProps props, ZkRegistry registry) {
        return args -> {
            registry.register(HelloService.class.getName(), props.getHost(), props.getPort());
            System.out.println("[Provider] started at " + props.getHost() + ":" + props.getPort()
                    + ", zk=" + props.getZk());
        };
    }
}