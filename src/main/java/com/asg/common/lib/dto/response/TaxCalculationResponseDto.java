package com.asg.common.lib.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaxCalculationResponseDto {
    private Double drAmt;
    private Double taxPercentage;
    private Double taxAmount;
    private Double totalAmount;
}
