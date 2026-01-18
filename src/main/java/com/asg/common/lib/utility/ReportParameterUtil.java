package com.asg.common.lib.utility;

import com.asg.common.lib.security.util.UserContext;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class ReportParameterUtil {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MMM-yyyy");

    public static String prepareRptParameters(Map<String, Object> filters) {
        return prepareRptParameters(filters, null, null);
    }

    public static String prepareRptParameters(Map<String, Object> filters, String docId, String docKeyPoid) {
        StringBuilder rptParams = new StringBuilder();

        // Add filter parameters
        if (filters != null && !filters.isEmpty()) {
            for (Map.Entry<String, Object> entry : filters.entrySet()) {
                String key = entry.getKey();
                Object value = formatValue(entry.getValue());
                
                if (!rptParams.isEmpty()) {
                    rptParams.append(";");
                }
                rptParams.append(key).append("=").append(value);
            }
        }

        // Add standard parameters
        addStandardParameters(rptParams, docId, docKeyPoid);

        return rptParams.toString();
    }

    private static Object formatValue(Object value) {
        if (value == null) {
            return "NULL";
        } else if (value instanceof Date) {
            return DATE_FORMAT.format((Date) value);
        }
        return value;
    }

    private static void addStandardParameters(StringBuilder rptParams, String docId, String docKeyPoid) {
        appendParam(rptParams, "DOC_ID", docId != null ? docId : "dummy");
        appendParam(rptParams, "DOC_KEY_POID", docKeyPoid != null ? docKeyPoid : "1");
        appendParam(rptParams, "SUBREPORT_DIR", getReportsPath());
        appendParam(rptParams, "CompanyName", UserContext.getCompanyName());
        appendParam(rptParams, "LOGIN_COMP_NAME", UserContext.getCompanyName());
    }

    private static void appendParam(StringBuilder rptParams, String key, String value) {
        if (!rptParams.isEmpty()) {
            rptParams.append(";");
        }
        rptParams.append(key).append("=").append(value);
    }

    private static String getReportsPath() {
        return "/reports/";
    }
}