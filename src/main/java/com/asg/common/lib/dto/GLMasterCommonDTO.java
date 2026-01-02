package com.asg.common.lib.dto;

import lombok.Data;

@Data
public class GLMasterCommonDTO {
    private String glCode;
    private String glDescription;
    private String costGroup;
    private String billwise;
    private String prepaymentLedger;
    private String interCompanyAc;
    private String controlAcNature;
    private String glAcType;
}
