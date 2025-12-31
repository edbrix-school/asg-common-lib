package com.asg.common.lib.dto.response;

import com.asg.common.lib.dto.response.LoadBillwiseBreakupResponseDto;
import lombok.Data;

import java.util.List;

@Data
public class GlVoucherLoadBillwiseBreakupResponseDto {
    List<LoadBillwiseBreakupResponseDto> loadBillwiseBreakupResponseDtoList;
}
