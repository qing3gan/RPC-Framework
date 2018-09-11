package com.agony.rpc.registry;

/**
 * 常量
 */
public interface Constant {

    //zookeeper超时时间
    int ZK_SESSION_TIMEOUT = 5000;

    //注册节点
    String ZK_REGISTRY_PATH = "/registry";

    //节点
    String ZK_DATA_PATH = ZK_REGISTRY_PATH + "/data";
}
