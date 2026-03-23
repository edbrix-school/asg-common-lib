package com.asg.common.lib.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
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
