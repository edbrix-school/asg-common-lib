package com.asg.common.lib.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContainerInventoryReportType {
    COSCO("100-223", "Mvmt.xlsx"),
    PERFORMANCE("100-373", "PERFOMANCE_REPORT_FORMAT_LINE_EXPORT.xlsx"),
    PERFORMANCE_DSL("100-377", "PERFOMANCE_REPORT_FORMAT_DSL_EXPORT.xlsx"),
    DAILY_INVENTORY_DSL("100-403", "DailyInventoryXLformatDSL.xlsx"),
    DAILY_INVENTORY_TNS("100-435", "DailyInventoryXLformatTNS.xlsx"),
    PICKUP_VASCO("100-408", "PICK_UP_REPORT_vasco.xlsx");

    private final String docId;
    private final String fileName;
}