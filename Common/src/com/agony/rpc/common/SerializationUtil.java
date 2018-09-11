package com.agony.rpc.common;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtobufIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * protocol buffer -> protocol stuff
 * 序列化工具
 */
public class SerializationUtil {

    //缓存protocol
    private static Map<Class<?>, Schema<?>> cachedSchema = new ConcurrentHashMap<>();

    //对象initiate
    private static Objenesis objenesis = new ObjenesisStd();

    private SerializationUtil() {
    }

    private static <T> Schema<T> getSchema(Class<T> clz) {
        Schema<T> schema = (Schema<T>) cachedSchema.get(clz);
        if (schema == null) {
            schema = RuntimeSchema.createFrom(clz);
            if (schema != null) {
                cachedSchema.put(clz, schema);
            }
        }
        return schema;
    }


    /**
     * 序列化（对象 -> 字节数组）
     */
    public static <T> byte[] serialize(T obj) {
        Class<T> clz = (Class<T>) obj.getClass();
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            Schema<T> schema = getSchema(clz);
            return ProtobufIOUtil.toByteArray(obj, schema, buffer);//序列化
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            buffer.clear();
        }
        return null;
    }

    /**
     * 反序列化（字节数组 -> 对象）
     */
    public static <T> T deserialize(byte[] data, Class<T> clz) {
        try {
            T message = objenesis.newInstance(clz);
            Schema<T> schema = getSchema(clz);
            ProtobufIOUtil.mergeFrom(data, message, schema);//反序列化
            return message;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
