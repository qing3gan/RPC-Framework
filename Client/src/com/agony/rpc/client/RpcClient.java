package com.agony.rpc.client;

import com.agony.rpc.common.RpcDecoder;
import com.agony.rpc.common.RpcEncoder;
import com.agony.rpc.common.RpcRequest;
import com.agony.rpc.common.RpcResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC客户端（用于发送RPC请求）
 */
public class RpcClient extends SimpleChannelInboundHandler<RpcResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcClient.class);
    //RPC服务端地址
    private String host;
    private int port;
    //RPC响应
    private RpcResponse rpcResponse;
    //MainThread与IOThread锁
    private final Object obj = new Object();

    public RpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * RPC客户端发送PRC请求
     * 1.发送PRC请求
     * 2.等待RPC响应
     */
    public RpcResponse send(RpcRequest rpcRequest) throws Exception {
        //Netty Client
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        try {
            //Bootstrap
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline()//HEAD->RpcEncoder->RpcDecoder->SimpleInbound->TAIL
                                    .addLast(new RpcEncoder(RpcRequest.class))//注册RpcEncoder,OUT-1
                                    .addLast(new RpcDecoder(RpcResponse.class))//注册RpcDecoder,IN-1
                                    .addLast(RpcClient.this);//注册SimpleInbound,IN-2

                        }
                    });
            //Channel
            ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
            channelFuture.channel().writeAndFlush(rpcRequest).sync();
            //MainThread
            synchronized (obj) {
                obj.wait();
            }
            if (rpcResponse != null) {
                channelFuture.channel().closeFuture().sync();
            }
            return rpcResponse;
        } finally {
            eventLoopGroup.shutdownGracefully();
        }
    }

    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcResponse rpcResponse) throws Exception {
        //获取RpcResponse
        this.rpcResponse = rpcResponse;
        //IOThread
        synchronized (obj) {
            obj.notifyAll();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("client caught exception", cause);
        ctx.close();
    }
}
