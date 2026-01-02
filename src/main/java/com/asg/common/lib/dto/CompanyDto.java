package com.asg.common.lib.dto;

import lombok.Getter;
import lombok.Setter;

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
    private Date financialPeriodStart;
    private Date financialPeriodEnd;
    private Date reportPeriodStart;
    private Date reportPeriodEnd;
    private Date transPeriodStart;
    private Date transPeriodEnd;
    private String active;
    private Integer seqNo;
    private String createdBy;
    private Date createdDate;
    private String lastModifiedBy;
    private Date lastModifiedDate;
    private String deleted;
    private Date provisionalClosedDate;
    private String bankDetail;
    private LovGetListDto bankDet;
    private Long bankPoid;
    private String tinNumber;
    private Date vatRegistrationDate;
    private Date vatLastFiledDate;
    private String accountPerson;
    private Date stockPeriodStart;
    private Date stockPeriodEnd;
    private String vatFilingPeriod;
    private String accountEmail;
    private String vatLastFiledBy;
    private Date vatLastFiledCreatedDate;
    private String financialDateUpdatedBy;
    private Date financialDateUpdatedDate;
    private String transDateUpdatedBy;
    private Date transDateUpdatedDate;
    private String reportDateUpdatedBy;
    private Date reportDateUpdatedDate;
    private String inventoryDateUpdatedBy;
    private Date inventoryDateUpdatedDate;
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

