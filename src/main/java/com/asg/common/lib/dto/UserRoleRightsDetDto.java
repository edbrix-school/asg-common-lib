package com.asg.common.lib.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class UserRoleRightsDetDto {

    private Long userRolePoid;
    private String userRoleId;
    private String userRoleName;
    private String userRoleName2;
    private Integer seqNo;
    private String active;
    private List<ModuleDto> modules;
    private List<UserInRoleDto> users;
    
    // Audit fields
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;

}
