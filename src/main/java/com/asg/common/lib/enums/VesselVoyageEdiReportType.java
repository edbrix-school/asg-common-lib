package com.asg.common.lib.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VesselVoyageEdiReportType {
    APMT_DISCHARGE_LIST("100-291", "Discharge_list.xlsx"),
    APMT_TRANS_DISCHARGE_LIST("100-459", "Transhipment_Discharge_list.xlsx"),
    OFOQ("100-298", "OFOQFormatedXL.xlsx"),
    APMT_GENERAL("100-369", "APMTLISTGERN.xlsx"),
    YML("100-300", "OA_Booking_csv_format.csv"),
    TBL_MANIFEST_TEMPLATE("100-425", "TBLManifestTemplate.xlsx"),
    TBL_LIST("100-424", "TBLLIST.xlsx");

    private final String docId;
    private final String fileName;
}