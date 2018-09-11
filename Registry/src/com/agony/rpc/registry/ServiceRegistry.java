package com.agony.rpc.registry;

import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * 服务注册（基于Zookeeper）
 */
public class ServiceRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRegistry.class);

    //连接zookeeper初始化
    private CountDownLatch latch = new CountDownLatch(1);

    //zookeeper地址（Spring注入）
    private String registryAddress;

    public ServiceRegistry(String registryAddress) {
        this.registryAddress = registryAddress;
    }

    private void createNode(ZooKeeper zooKeeper, String data) {
        try {
            //创建注册节点
            byte[] bytes = data.getBytes();
            if (zooKeeper.exists(Constant.ZK_REGISTRY_PATH, null) == null) {
                zooKeeper.create(Constant.ZK_REGISTRY_PATH, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            //注册服务（注册节点下创建机器节点）
            String path = zooKeeper.create(Constant.ZK_DATA_PATH, bytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            LOGGER.debug("create zookeeper node ({} => {})", path, data);
        } catch (Exception e) {
            LOGGER.error("create zookeeper node error", e);
        }
    }

    private ZooKeeper connectZookeeper() {
        ZooKeeper zooKeeper = null;
        try {
            zooKeeper = new ZooKeeper(registryAddress, Constant.ZK_SESSION_TIMEOUT, watchedEvent -> {
                if (watchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    latch.countDown();
                }
            });
            latch.await();
        } catch (Exception e) {
            LOGGER.error("connect zookeeper error", e);
        }
        return zooKeeper;
    }

    public void registry(String data) {
        if (data != null) {
            ZooKeeper zooKeeper = connectZookeeper();
            if (zooKeeper != null) {
                createNode(zooKeeper, data);
            }
        }
    }
}
