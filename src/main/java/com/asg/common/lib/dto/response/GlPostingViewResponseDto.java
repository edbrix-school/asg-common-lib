package com.asg.common.lib.dto.response;

import com.asg.common.lib.dto.BillwiseBreakupDto;
import com.asg.common.lib.dto.CostBreakupDto;
import com.asg.common.lib.dto.LedgerEntryDto;
import com.asg.common.lib.dto.VatBreakupDto;
import lombok.Data;

import java.util.List;

@Data
public class GlPostingViewResponseDto {
    private List<LedgerEntryDto> ledgerEntries;
    private List<BillwiseBreakupDto> billwiseBreakup;
    private List<CostBreakupDto> costBreakup;
    private List<VatBreakupDto> vatBreakup;
}