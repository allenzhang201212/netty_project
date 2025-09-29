package com.netty.consumer;

import com.netty.core.*;
import com.netty.registry.zk.ZkRegistry;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(RpcConsumerProps.class)
public class ConsumerApplication {
    public static void main(String[] args) { SpringApplication.run(ConsumerApplication.class, args); }

    @Bean public ZkRegistry registry(RpcConsumerProps props) { return new ZkRegistry(props.getZk()); }
    @Bean public PooledRpcClient client() { return new PooledRpcClient(); }

    @Bean
    public LoadBalancer loadBalancer(RpcConsumerProps props) {
        return "random".equalsIgnoreCase(props.getLb()) ? new RandomLoadBalancer() : new RoundRobinLoadBalancer();
    }

    @Bean
    public ProxyFactory proxyFactory(PooledRpcClient client, ZkRegistry registry, LoadBalancer lb, RpcConsumerProps props) {
        return new ProxyFactory(client, registry, lb, props.getTimeoutMs(), props.getRetries());
    }

    @Bean
    public ApplicationRunner runner(HelloClient client) {
        return args -> {
            String r = client.call("Netty via @RpcReference");
            System.out.println("[Consumer] result = " + r);
        };
    }
}