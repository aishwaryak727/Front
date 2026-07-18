package com.teleconnect.billing_service.dto.response;

public class DataResponse<T> {

    private int statusCode;
    private T data;

    public DataResponse() {}

    public DataResponse(int statusCode, T data) {
        this.statusCode = statusCode;
        this.data = data;
    }

    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
