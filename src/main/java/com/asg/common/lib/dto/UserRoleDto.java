package com.asg.common.lib.dto;

import lombok.Builder;

import java.sql.Date;
import java.time.LocalDate;

@Builder
public record UserRoleDto(Long userRolePoId, String userRoleId, String userRoleName, LocalDate expiryDate, String deleted,
                          String active, String actionType, Long detRowId) {
}

