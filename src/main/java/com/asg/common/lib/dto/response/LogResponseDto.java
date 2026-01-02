package com.asg.common.lib.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogResponseDto {
    private Timestamp logDateTime;
    private String userName;
    private Long logUserPoid;
    private String logDetails;
    private String fieldName;
    private String oldValue;
    private String newValue;
}