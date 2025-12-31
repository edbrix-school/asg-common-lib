package com.asg.common.lib.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShowPendingBillwiseBreakupResponseDto {
    private Long glCompanyPoid;
    private String billRef;
    private Date billDueDate;
    private String remarks;
    private BigDecimal balance;
}
