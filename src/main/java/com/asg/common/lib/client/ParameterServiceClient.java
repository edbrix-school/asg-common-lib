package com.asg.common.lib.client;

import com.asg.common.lib.dto.response.ApiResponseWrapper;
import com.asg.common.lib.utility.RestClientUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ParameterServiceClient {
    
    private final GenericRestClient restClient;
    
    @Value("${settings.service.url:http://localhost:8082/settings/api}")
    private String settingsServiceUrl;
    
    public Optional<String> findParameterValueByName(String parameterName) {
        try {
            String url = settingsServiceUrl + "/v1/global-parameters/value/" + parameterName;
            ApiResponseWrapper<String> response = restClient.get(url, new ParameterizedTypeReference<>() {});
            return Optional.ofNullable(RestClientUtil.extractData(response));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public java.math.BigDecimal getParameterValueByNameAsDecimal(String parameterType, String parameterName) {
        try {
            String url = settingsServiceUrl + "/v1/global-parameters/value-decimal/" + parameterType + "/" + parameterName;
            ApiResponseWrapper<java.math.BigDecimal> response = restClient.get(url, new ParameterizedTypeReference<>() {});
            return RestClientUtil.extractData(response);
        } catch (Exception e) {
            return null;
        }
    }

    public Integer getParameterValueByName(String parameterType, String parameterName) {
        try {
            String url = settingsServiceUrl + "/v1/global-parameters/value/" + parameterType + "/" + parameterName;
            ApiResponseWrapper<Integer> response = restClient.get(url, new ParameterizedTypeReference<>() {});
            return RestClientUtil.extractData(response);
        } catch (Exception e) {
            return null;
        }
    }
}
