package com.asg.common.lib.utility;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "pagination")
public class PaginationProperties {
    private int pageNumber = 0;
    private int pageSize = 10;
    private String sortBy = "id";
    private String sortDir = "asc";
}
