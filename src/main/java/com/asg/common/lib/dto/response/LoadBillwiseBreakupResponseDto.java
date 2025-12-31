package com.asg.common.lib.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoadBillwiseBreakupResponseDto {
    private Long mainDetRowId;
    private Long billDetRowId;
    private Long glPoid;
    private String billRefType;
    private String billRef;
    private Date billDueDate;
    private BigDecimal drAmt;
    private BigDecimal crAmt;
    private String billRemarks;
}
