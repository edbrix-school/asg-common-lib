package com.asg.common.lib.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Data
public class BillwiseBreakupDto {
    private LocalDate transactionDate;
    private String docRef;
    private String glCode;
    private String glDescription;
    private String billRefType;
    private String billRef;
    private Date billDueDate;
    private String remarks;
    private BigDecimal drAmt;
    private BigDecimal crAmt;
    private String glCompany;
}