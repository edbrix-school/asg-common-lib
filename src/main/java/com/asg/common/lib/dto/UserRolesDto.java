package com.asg.common.lib.dto;

import lombok.Data;

@Data
public class UserRolesDto {
    private Long userRolePoid;
    private String userRoleId;
    private Long groupPoid;
    private String userRoleName;
    private String userRoleName2;
    private String active;
    private Integer seqNo;
    private Long companyPoid;
    private String deleted;

}
