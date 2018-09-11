package com.agony.rpc.server;

import com.agony.rpc.common.RpcRequest;
import com.agony.rpc.common.RpcResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 业务调用
 */
public class RpcHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcHandler.class);

    //业务集合
    private final Map<String, Object> handlerMap;

    public RpcHandler(Map<String, Object> handlerMap) {
        this.handlerMap = handlerMap;
    }

    /**
     * 接受RpcRequest，调用具体业务，返回RpcResponse
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcRequest rpcRequest) throws Exception {
        RpcResponse rpcResponse = new RpcResponse();
        rpcResponse.setRequsetId(rpcRequest.getRequestId());
        try {
            Object result = handle(rpcRequest);
            rpcResponse.setResult(result);
        } catch (Exception e) {
            rpcResponse.setError(e);
        }
        //RpcRequest -> RpcHandler -> RpcResponse
        channelHandlerContext.writeAndFlush(rpcResponse).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 调用具体业务（反射）
     */
    private Object handle(RpcRequest rpcRequest) throws Exception {
        //业务类名
        String className = rpcRequest.getClassName();
        //业务实例
        Object serviceBean = handlerMap.get(className);
        //业务方法调用参数
        String methodName = rpcRequest.getMethodName();
        Class<?>[] parameterType = rpcRequest.getParameterType();
        Object[] params = rpcRequest.getParamters();
        //业务调用
        Class<?> serviceClass = Class.forName(className);
        Method serviceMethod = serviceClass.getMethod(methodName, parameterType);
        return serviceMethod.invoke(serviceBean, params);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("server caught exception", cause);
        ctx.close();
    }
}
