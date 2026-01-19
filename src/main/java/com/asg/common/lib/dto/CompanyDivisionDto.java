package com.asg.common.lib.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CompanyDivisionDto {
    private Long companyPoid;
    private Long detRowId;
    private Long divPoid;
    private String remarks;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private String logoImageBase64;
    private String companyDivAddress;
    private String divisionName;
    private String companyDivAddressPos;
    private String actionType;
}
