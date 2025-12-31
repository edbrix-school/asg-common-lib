package com.asg.common.lib.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserRoleRightsDto {
    private Long userRolePoid;
    private Long detRowId;
    private String docId;
    private String rights;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    
}
