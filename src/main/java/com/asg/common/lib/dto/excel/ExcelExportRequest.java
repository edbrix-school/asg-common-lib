package com.asg.common.lib.dto.excel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExcelExportRequest {
    private String docId;
    private String docKeyPoid;
    private String outputFileName;
    private String currency;
    private String reportRootPath;
    private List<ExcelSheetConfig> sheets;
}
