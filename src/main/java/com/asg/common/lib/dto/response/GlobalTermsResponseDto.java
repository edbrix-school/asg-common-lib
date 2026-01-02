package com.asg.common.lib.dto.response;

import com.asg.common.lib.dto.GlobalTermsDto;
import lombok.Data;

import java.util.List;

@Data
public class GlobalTermsResponseDto {
    private List<GlobalTermsDto> termsList;
}
