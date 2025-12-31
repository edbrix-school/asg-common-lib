package com.asg.common.lib.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GLMasterDto {
    private Long glPoid;
    private String glCode;
    private String glDescription;
    private String glDescription2;
    private String glType;
    private String controlAcNature;
    private String costGroup;
    private String billwise;
    private String prepaymentLedger;
    private String interCompanyAc;
    private String glAcType;
}
