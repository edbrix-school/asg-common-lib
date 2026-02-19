package com.asg.common.lib.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogFilterRequest {
    private Integer page = 0;
    private Integer size = Integer.MAX_VALUE;
    private LocalDate startDate;
    private LocalDate endDate;
    private String searchText;
}
