package com.agony.rpc.server;

import com.agony.rpc.common.RpcDecoder;
import com.agony.rpc.common.RpcEncoder;
import com.agony.rpc.common.RpcRequest;
import com.agony.rpc.common.RpcResponse;
import com.agony.rpc.registry.ServiceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.Map;

/**
 * RPC服务端（用于将用户系统的业务类发布为RPC服务）
 * 基于Spring Context(IOC)
 * ApplicationContextAware.setApplicationContext()：存储业务接口和业务实现类（通过自定义注解获取）
 * InitializingBean.afterPropertiesSet()：启动RPC服务端（基于Netty）
 */
public class RpcServer implements ApplicationContextAware, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);

    //RPC服务端地址
    private String serverAddress;
    //服务注册
    private ServiceRegistry serviceRegistry;
    //用于存储用户系统的业务接口和业务实现类（Spring实例化）
    private Map<String, Object> handlerMap = new HashMap<>();

    //Spring注入RPC服务端地址和Zookeeper命名服务
    public RpcServer(String serverAddress, ServiceRegistry serviceRegistry) {
        this.serverAddress = serverAddress;
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * 初始化业务handlerMap后，启动RPC服务端，绑定handler流水线pipeline
     * 1.反序列化RpcRequest获取Service
     * 2.调用Service.method()
     * 3.序列化RpcResponse返回结果
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        //Netty Server(Event-Driven-NIO)
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();
        try {
            //Bootstrap
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline()//HEAD->RpcDecoder->RpcHandler->RpcEncoder->TAIL
                                    .addLast(new RpcDecoder(RpcRequest.class))//注册RpcDecoder,IN-1
                                    .addLast(new RpcEncoder(RpcResponse.class))//注册RpcEncoder,OUT-1
                                    .addLast(new RpcHandler(handlerMap));//注册RpcHandler,IN-2
                        }
                    }).option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            //Channel
            String[] array = serverAddress.split(":");
            String host = array[0];
            int port = Integer.parseInt(array[1]);
            ChannelFuture channelFuture = serverBootstrap.bind(host, port).sync();
            LOGGER.debug("server started on port {}", port);
            //Registry
            if (serviceRegistry != null) {
                serviceRegistry.registry(serverAddress);
            }
            channelFuture.channel().closeFuture().sync();
        } finally {
            workGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    /**
     * 通过RpcService注解获取用户系统的业务接口和业务实现类，存入业务handlerMap
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        //Spring通过注解获取<beanName,bean>
        Map<String, Object> services = applicationContext.getBeansWithAnnotation(RpcService.class);
        if (MapUtils.isNotEmpty(services)) {
            services.values().parallelStream().forEach(serviceBean -> {
                //获取RpcService.value()
                String interfaceName = serviceBean.getClass().getAnnotation(RpcService.class).value().getName();
                handlerMap.put(interfaceName, serviceBean);
            });
        }
    }
}
