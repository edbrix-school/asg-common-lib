package com.asg.common.lib.client;

import com.asg.common.lib.security.util.UserContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GenericRestClient {
    private final RestTemplate restTemplate;

    public GenericRestClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public <T> T get(String url, Class<T> responseType) {
        return get(url, responseType, null);
    }

    public <T> T get(String url, Class<T> responseType, HttpHeaders customHeaders) {
        HttpHeaders headers = buildHeaders(customHeaders);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(url, HttpMethod.GET, entity, responseType).getBody();
    }

    public <T> T get(String url, ParameterizedTypeReference<T> responseType) {
        return get(url, responseType, null);
    }

    public <T> T get(String url, ParameterizedTypeReference<T> responseType, HttpHeaders customHeaders) {
        HttpHeaders headers = buildHeaders(customHeaders);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(url, HttpMethod.GET, entity, responseType).getBody();
    }

    public <T, R> R post(String url, T request, Class<R> responseType) {
        return post(url, request, responseType, null);
    }

    public <T, R> R post(String url, T request, Class<R> responseType, HttpHeaders customHeaders) {
        HttpHeaders headers = buildHeaders(customHeaders);
        HttpEntity<T> entity = new HttpEntity<>(request, headers);
        return restTemplate.exchange(url, HttpMethod.POST, entity, responseType).getBody();
    }

    public <T, R> R post(String url, T request, ParameterizedTypeReference<R> responseType) {
        return post(url, request, responseType, null);
    }

    public <T, R> R post(String url, T request, ParameterizedTypeReference<R> responseType, HttpHeaders customHeaders) {
        HttpHeaders headers = buildHeaders(customHeaders);
        HttpEntity<T> entity = new HttpEntity<>(request, headers);
        return restTemplate.exchange(url, HttpMethod.POST, entity, responseType).getBody();
    }

    public <T, R> R put(String url, T request, Class<R> responseType) {
        return put(url, request, responseType, null);
    }

    public <T, R> R put(String url, T request, Class<R> responseType, HttpHeaders customHeaders) {
        HttpHeaders headers = buildHeaders(customHeaders);
        HttpEntity<T> entity = new HttpEntity<>(request, headers);
        return restTemplate.exchange(url, HttpMethod.PUT, entity, responseType).getBody();
    }

    public void delete(String url) {
        delete(url, null);
    }

    public void delete(String url, HttpHeaders customHeaders) {
        HttpHeaders headers = buildHeaders(customHeaders);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
    }

    private HttpHeaders buildHeaders(HttpHeaders customHeaders) {
        HttpHeaders headers = new HttpHeaders();
        
        if (customHeaders != null) {
            headers.addAll(customHeaders);
        }
        
        headers.add("X-User-Name", UserContext.getUserName());
        headers.add("X-User-Poid", UserContext.getUserPoid() != null ? UserContext.getUserPoid().toString() : null);
        headers.add("X-User-Id", UserContext.getUserId());
        headers.add("X-User-Email", UserContext.getUserEmail());
        headers.add("X-User-Role", UserContext.getUserRole());
        headers.add("X-Group-Poid", UserContext.getGroupPoid() != null ? UserContext.getGroupPoid().toString() : null);

        if (!headers.containsKey("X-Company-Poid") || StringUtils.isBlank(headers.getFirst("X-Company-Poid"))) {
            headers.add("X-Company-Poid", UserContext.getCompanyPoid() != null ? UserContext.getCompanyPoid().toString() : null);
        }
        
        return headers;
    }
}
