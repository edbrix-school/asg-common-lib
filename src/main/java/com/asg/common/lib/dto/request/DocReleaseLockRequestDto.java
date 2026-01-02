package com.asg.common.lib.dto.request;

import lombok.Data;

@Data
public class DocReleaseLockRequestDto {
    private Long loginGroupPoid;
    private Long loginCompanyPoid;
    private String loginUserPoid;
    private String docId;
    private Long docPoidValue;
    private String userId;
}
