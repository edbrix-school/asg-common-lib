package com.asg.common.lib.dto.response;

import lombok.Data;

@Data
public class ApiResponseWrapper<T> {
    private Boolean success;
    private Integer statusCode;
    private String message;
    private ResultWrapper<T> result;
    
    @Data
    public static class ResultWrapper<T> {
        private T data;
    }
}
