# Netty + ZooKeeper Distributed RPC

A distributed RPC framework built with Spring Boot, Netty (NIO), and ZooKeeper. It provides service registration/discovery, long-lived connections with heartbeat, JSON serialization, sync/async calls with timeout & retry, simple load balancing, and dynamic proxies so consumers invoke remote interfaces like local methods.

---

## Features
- **Service Registry/Discovery** via ZooKeeper (ephemeral nodes + watchers)
- **High-performance I/O** with Netty framing (sticky/half-packet safe)
- **Long-lived connections** with heartbeat/idle detection
- **Sync & Async RPC** (future/promise), per-call **timeout** & **retry**
- **JSON serialization** (pluggable)
- **Load balancing** (round-robin, extensible)
- **Spring integration**: `@RpcService` (provider), `@RpcReference` (consumer)

## Project Structure

├─ rpc-common/        # DTOs, protocol, codecs, shared utils
├─ rpc-registry/      # ZK client, registration & discovery
├─ rpc-provider/      # Demo provider (Spring Boot)
├─ rpc-consumer/      # Demo consumer (Spring Boot)
├─ pom.xml
└─ README.md

## Prerequisites
- JDK 17+
- Maven 3.8+ (or `./mvnw`)
- ZooKeeper 3.7+ at `127.0.0.1:2181`  
  ```bash
  docker run -d --name zk -p 2181:2181 zookeeper


  
