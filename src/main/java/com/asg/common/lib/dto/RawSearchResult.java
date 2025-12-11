package com.asg.common.lib.dto;

import java.util.List;
import java.util.Map;

// To get the records matching the dynamic query, the count and the displayable fields for any entity
public record RawSearchResult(
        List<Map<String, Object>> records,
        Map<String, String> displayFields,
        long totalRecords
) {}
