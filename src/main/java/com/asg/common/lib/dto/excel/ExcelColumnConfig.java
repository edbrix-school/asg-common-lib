package com.asg.common.lib.dto.excel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExcelColumnConfig {
    private String fieldName;
    private Integer rowRef;
    private Integer colRef;
    private CellFormatType cellFormatType;
    private Boolean showCaption;
    private String cellFormula;
}
