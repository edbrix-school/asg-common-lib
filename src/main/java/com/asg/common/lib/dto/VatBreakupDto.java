package com.asg.common.lib.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class VatBreakupDto {
    private LocalDate transactionDate;
    private String docRef;
    private String glCode;
    private String glDescription;
    private String taxName;
    private BigDecimal taxBaseAmount;
    private BigDecimal taxAmount;
    private BigDecimal amt;
}