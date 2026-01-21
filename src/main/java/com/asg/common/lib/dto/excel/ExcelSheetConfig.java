package com.asg.common.lib.dto.excel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Blob;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExcelSheetConfig {
    private String sheetName;
    private String sheetId;
    private String excelTemplateFile;
    private Boolean showHeaderImage;
    private Integer headerImageEndRow;
    private Boolean dtl1ShowColHdr;
    private Integer dtl1StartRow;
    private Blob headerImageBlob;
    private Map<String, ExcelColumnConfig> columnConfigMap;
    private Map<String, Object> headerData;
    private List<Map<String, Object>> detailData;
    private Map<String, Object> footerData;
}
