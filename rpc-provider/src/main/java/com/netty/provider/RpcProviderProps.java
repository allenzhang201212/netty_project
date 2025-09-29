package com.netty.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rpc")
public class RpcProviderProps {
    private String zk = "127.0.0.1:2181";
    private String host = "127.0.0.1";
    private int port = 19090;

    public String getZk() { return zk; }
    public void setZk(String zk) { this.zk = zk; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
}