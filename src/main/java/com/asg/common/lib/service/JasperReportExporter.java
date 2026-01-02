package com.asg.common.lib.service;

import net.sf.jasperreports.engine.*;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;
public class JasperReportExporter {
    private JasperReportExporter() {
        // private constructor to prevent instantiation
    }

    public static byte[] fillReportToPdf(JasperReport report, Map<String, Object> params, DataSource dataSource) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            params.put("REPORT_CLASS_LOADER", JasperReportExporter.class.getClassLoader());
            JasperPrint jasperPrint = JasperFillManager.fillReport(report, params, conn);
            return JasperExportManager.exportReportToPdf(jasperPrint);
        }
    }
}
