package com.asg.common.lib.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleDto {
    private Long userRolePoid;
    private Long groupPoid;
    private String userRoleId;
    private String userRoleName;
    private String userRoleName2;
    private String active;
    private Integer seqNo;
    private Long companyPoid;
    private String deleted;
}
