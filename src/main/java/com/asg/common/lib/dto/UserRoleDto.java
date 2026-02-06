package com.asg.common.lib.dto;

import lombok.Builder;

import java.sql.Date;

@Builder
public record UserRoleDto(Long userRolePoId, String userRoleId, String userRoleName, Date expiryDate, String deleted,
                          String active, String actionType,Long detRowId) {
}

