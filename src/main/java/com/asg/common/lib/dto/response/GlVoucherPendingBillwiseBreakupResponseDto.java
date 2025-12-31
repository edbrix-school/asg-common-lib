package com.asg.common.lib.dto.response;

import com.asg.common.lib.dto.response.ShowPendingBillwiseBreakupResponseDto;
import lombok.Data;

import java.util.List;
@Data
public class GlVoucherPendingBillwiseBreakupResponseDto {
    List<ShowPendingBillwiseBreakupResponseDto> showPendingBillwiseBreakupResponseDtoList;
}
