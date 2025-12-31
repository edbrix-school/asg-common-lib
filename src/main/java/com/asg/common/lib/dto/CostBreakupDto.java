package com.asg.common.lib.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CostBreakupDto {
    private LocalDate transactionDate;
    private String docRef;
    private String glCode;
    private String glDescription;
    private String costGroup;
    private Long costPoid;
    private BigDecimal amt;
    private String glCompany;
}