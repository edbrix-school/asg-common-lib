package com.asg.common.lib.utility;

import com.asg.common.lib.dto.response.ApiResponseWrapper;

import java.util.List;

public class RestClientUtil {

    public static <T> T extractData(ApiResponseWrapper<T> response) {
        try {
            if (response == null || !Boolean.TRUE.equals(response.getSuccess()) ||
                    response.getResult() == null || response.getResult().getData() == null) {
                return null;
            }
            return response.getResult().getData();
        } catch (Exception e) {
            return null;
        }
    }

    public static <T> List<T> extractListData(ApiResponseWrapper<List<T>> response) {
        try {
            if (response == null || !Boolean.TRUE.equals(response.getSuccess()) ||
                    response.getResult() == null || response.getResult().getData() == null) {
                return null;
            }
            return response.getResult().getData();
        } catch (Exception e) {
            return null;
        }
    }
}
