package com.agony.rpc.registry;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 服务发现（基于Zookeeper）
 */
public class ServiceDiscovery {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceDiscovery.class);

    //连接zookeeper初始化
    private CountDownLatch latch = new CountDownLatch(1);

    //服务列表（并发赋值）
    private volatile List<String> servers = new ArrayList<>();

    //zookeeper地址
    private String registryAddress;

    public ServiceDiscovery(String registryAddress) {
        this.registryAddress = registryAddress;
        ZooKeeper zooKeeper = connectZookeeper();
        if (zooKeeper != null) {
            watchNode(zooKeeper);
        }
    }

    private void watchNode(final ZooKeeper zooKeeper) {
        try {
            //发现服务（获取注册节点下的机器节点）
            List<String> nodeList = zooKeeper.getChildren(Constant.ZK_REGISTRY_PATH, watchedEvent -> {
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                    ServiceDiscovery.this.watchNode(zooKeeper);
                }
            });
            List<String> dataList = new ArrayList<>();
            nodeList.parallelStream().forEach(node -> {
                try {
                    //发现服务（获取机器节点数据）
                    byte[] data = zooKeeper.getData(Constant.ZK_REGISTRY_PATH + "/" + node, false, null);
                    dataList.add(new String(data));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            servers = dataList;
            LOGGER.debug("watch node data: {}", servers);
        } catch (Exception e) {
            LOGGER.error("watch node error", e);
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

    /**
     * 发现新节点
     */
    public String discovery() {
        String node = null;
        int size = servers.size();
        if (size > 0) {
            if (size == 1) {
                node = servers.get(0);
                LOGGER.debug("using only node: {}", node);
            } else {
                node = servers.get(ThreadLocalRandom.current().nextInt(size));
                LOGGER.debug("using random node: {}", node);
            }
        }
        return node;
    }
}
