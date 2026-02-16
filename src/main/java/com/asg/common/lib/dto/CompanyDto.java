package com.asg.common.lib.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class CompanyDto {
    private Long groupPoid;
    private Long companyPoid;
    private String companyCode;
    private String companyName;
    private String label;
    private Long value;
    private TimeZoneDto timeZone;
    private String companyName2;
    private String contactPerson;
    private String telephone;
    private String fax;
    private String email;
    private String countryId;
    private String address;
    private LocalDate financialPeriodStart;
    private LocalDate financialPeriodEnd;
    private LocalDate reportPeriodStart;
    private LocalDate reportPeriodEnd;
    private LocalDate transPeriodStart;
    private LocalDate transPeriodEnd;
    private String active;
    private Integer seqNo;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private String deleted;
    private LocalDate provisionalClosedDate;
    private String bankDetail;
    private LovGetListDto bankDet;
    private Long bankPoid;
    private String tinNumber;
    private LocalDate vatRegistrationDate;
    private LocalDate vatLastFiledDate;
    private String accountPerson;
    private LocalDate stockPeriodStart;
    private LocalDate stockPeriodEnd;
    private String vatFilingPeriod;
    private String accountEmail;
    private String vatLastFiledBy;
    private LocalDateTime vatLastFiledCreatedDate;
    private String financialDateUpdatedBy;
    private LocalDateTime financialDateUpdatedDate;
    private String transDateUpdatedBy;
    private LocalDateTime transDateUpdatedDate;
    private String reportDateUpdatedBy;
    private LocalDateTime reportDateUpdatedDate;
    private String inventoryDateUpdatedBy;
    private LocalDateTime inventoryDateUpdatedDate;
    private String countryCode;
    private String stateName;
    private byte[] logoImage;
    private String logoImageBase64;
    private String dateFormat;
    private Long timezoneId;
    private DetailsDto currency;
    private List<CompanyDivisionDto> divisions;
    private String stateId;
    private String companyColor;
    private Long submissionPeriod;
}

