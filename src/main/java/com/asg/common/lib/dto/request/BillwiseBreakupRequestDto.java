package com.asg.common.lib.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillwiseBreakupRequestDto {
    private Long groupPoid;
    private Long companyPoid;
    private String docId;
    private Long transactionPoid;
    private Long mainDetRowId;
    private Long glPoid;
    private Long glCompanyPoid;
    private Long billDetRowId;
    private String billRefType;
    private String billRef;
    private LocalDate billDueDate;
    private BigDecimal drAmt;
    private BigDecimal crAmt;
    private String billRemarks;
    private Long loginUserPoid;
}
