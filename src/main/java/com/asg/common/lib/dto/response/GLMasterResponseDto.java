package com.asg.common.lib.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GLMasterResponseDto {
    private Long glPoid;
    private String glCode;
    private String description;
    private String description2;
    private String type;
    private Long subOf;
    private String accountType;
    private String controlAcType;
    private String costGroup;
    private Boolean interCompany;
    private Long interCompanyId;
    private String remarks;
    private Integer seqNo;
    private Boolean active;
    private Boolean isKeyFavorite;
    private Boolean billWise;
    private Boolean prepaymentLedger;
    private String createdBy;
    private LocalDateTime createdDate;
}