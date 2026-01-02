package com.asg.common.lib.dto.response;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockDetailsResponse {

    private Long stockPoid;
    private String stockCode;
    private String stockName;
    private String stockName2;
    private LovGetListDto stockDtl;
    private Long stockUnitPoid;
    private LovGetListDto unitDtl;

    private Long inputTaxPoid;
    private LovGetListDto taxDtl;
    private BigDecimal stockCost;
    private String remarks;

}
