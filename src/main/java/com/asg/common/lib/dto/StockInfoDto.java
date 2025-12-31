package com.asg.common.lib.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockInfoDto {
    private Long stockPoid;
    private String stockCode;
    private String stockName;
    private String stockName2;
    private Long categoryPoid;
    private Long stockUnitPoid;
    private BigDecimal stockCost;
    private String currencyCode;
    private Long taxPoid;
    private String serviceItem;
    private String categoryCode;
}
