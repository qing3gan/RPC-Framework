package com.agony.rpc.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * RPC解码器（基于Netty）
 */
public class RpcDecoder extends ByteToMessageDecoder {

    //反序列化对象类型
    private Class<?> genericClass;

    public RpcDecoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    @Override
    public void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        if (byteBuf.readableBytes() < 4) {
            return;
        }
        byteBuf.markReaderIndex();
        int dataLength = byteBuf.readInt();
        if (dataLength < 0) {
            channelHandlerContext.close();
        }
        if (byteBuf.readableBytes() < dataLength) {
            byteBuf.resetReaderIndex();
        }
        //反序列化
        byte[] data = new byte[dataLength];
        byteBuf.readBytes(data);
        Object obj = SerializationUtil.deserialize(data, genericClass);
        list.add(obj);
    }
}
