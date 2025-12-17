package com.asg.common.lib.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LovGetListDto {
    private Long poid;
    private String code;
    private String label;
    private Long value;
    private String description;
    private Integer seqNo;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String users;
}
