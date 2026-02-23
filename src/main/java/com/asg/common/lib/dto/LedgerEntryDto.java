package com.asg.common.lib.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class LedgerEntryDto {
    private LocalDate transactionDate;
    private String docRef;
    private String narration;
    private String companyCode;
    private String glAcType;
    private String glCode;
    private String glDescription;
    private BigDecimal drAmt;
    private BigDecimal crAmt;
    private String postedBy;
    private LocalDateTime postedDate;
}