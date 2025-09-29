package com.netty.consumer;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rpc")
public class RpcConsumerProps {
    private String zk = "127.0.0.1:2181";
    private long timeoutMs = 3000;
    private int retries = 2;
    private String lb = "rr";

    public String getZk() { return zk; }
    public void setZk(String zk) { this.zk = zk; }
    public long getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
    public int getRetries() { return retries; }
    public void setRetries(int retries) { this.retries = retries; }
    public String getLb() { return lb; }
    public void setLb(String lb) { this.lb = lb; }

}
