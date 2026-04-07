package com.asg.common.lib.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String users;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String controlAcNature;
}
