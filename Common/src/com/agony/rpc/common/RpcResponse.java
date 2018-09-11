package com.agony.rpc.common;

/**
 * RPC响应（封装RPC调用结果）
 */
public class RpcResponse {

    private String requsetId;
    private Throwable error;
    private Object result;

    public String getRequsetId() {
        return requsetId;
    }

    public void setRequsetId(String requsetId) {
        this.requsetId = requsetId;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public boolean isError() {
        return error != null;
    }
}
