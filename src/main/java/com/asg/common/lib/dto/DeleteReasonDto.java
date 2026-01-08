package com.asg.common.lib.dto;

import lombok.Data;

import jakarta.validation.constraints.Size;

@Data
public class DeleteReasonDto {
    @Size(max = 200, message = "Delete reason must not exceed 200 characters")
    private String deleteReason;
}
