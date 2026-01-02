package com.asg.common.lib.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanySimpleDto {
    private Long companyPoid;
    private String companyCode;
    private String companyName;
    private String vatFilingPeriod;
}
