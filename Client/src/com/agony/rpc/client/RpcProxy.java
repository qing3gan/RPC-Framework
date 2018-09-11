package com.agony.rpc.client;

import com.agony.rpc.common.RpcRequest;
import com.agony.rpc.common.RpcResponse;
import com.agony.rpc.registry.ServiceDiscovery;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

/**
 * RPC代理（用于创建RPC服务代理）
 */
public class RpcProxy {

    //服务端地址
    private String serverAddress;
    //服务端发现
    private ServiceDiscovery serviceDiscovery;

    public RpcProxy(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public RpcProxy(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    public <T> T create(Class<?> interfaceClass) {
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass}, new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        //代理对象
                        RpcRequest rpcRequest = new RpcRequest();
                        rpcRequest.setRequestId(UUID.randomUUID().toString());
                        rpcRequest.setClassName(method.getDeclaringClass().getName());
                        rpcRequest.setMethodName(method.getName());
                        rpcRequest.setParameterType(method.getParameterTypes());
                        rpcRequest.setParamters(args);
                        if (serviceDiscovery != null) {
                            serverAddress = serviceDiscovery.discovery();
                        }
                        String[] array = serverAddress.split(":");
                        String host = array[0];
                        int port = Integer.valueOf(array[1]);
                        //被代理对象
                        RpcClient rpcClient = new RpcClient(host, port);
                        RpcResponse rpcResponse = rpcClient.send(rpcRequest);
                        if (rpcResponse.isError()) {
                            throw rpcResponse.getError();
                        } else {
                            return rpcResponse.getResult();
                        }
                    }
                });
    }
}
