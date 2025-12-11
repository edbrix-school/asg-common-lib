package com.asg.common.lib.utility;

import org.springframework.data.domain.Page;

import java.util.Map;

public class PaginationUtil {

    public static <T> Map<String, Object> wrapPage(Page<T> page, Map<String, String> displayFields) {
        return Map.of(
                "content", page.getContent(),
                "displayFields", displayFields != null ? displayFields : Map.of(),
                "pageNumber", page.getNumber(),
                "pageSize", page.getSize(),
                "totalElements", page.getTotalElements(),
                "totalPages", page.getTotalPages(),
                "last", page.isLast()
        );
    }
}
