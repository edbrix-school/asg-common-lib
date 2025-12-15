package com.asg.common.lib.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiffObject {
    private String fieldName;
    private String oldValue;
    private String newValue;
}