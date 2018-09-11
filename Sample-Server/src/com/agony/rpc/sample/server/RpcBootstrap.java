package com.agony.rpc.sample.server;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * RPC服务端启动入口（通过启动SpringContext加载RpcServer并注入RpcService）
 */
public class RpcBootstrap {

    public static void main(String[] args) {
        new ClassPathXmlApplicationContext("spring.xml");
    }
}
