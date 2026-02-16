package com.asg.common.lib.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogResponseDto {
    private LocalDateTime logDateTime;
    private String userName;
    private Long logUserPoid;
    private String logDetails;
    private String fieldName;
    private String oldValue;
    private String newValue;
}