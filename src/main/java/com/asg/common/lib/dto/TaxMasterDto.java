package com.asg.common.lib.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxMasterDto {
    private Long taxPoid;
    private String taxCode;
    private String taxName;
    private String taxName2;
    private Double percentage;
    private String taxType;
    private String glType;
    private Long glLedgerPoid;
    private String taxCategory;
    private Integer seqNo;
}
