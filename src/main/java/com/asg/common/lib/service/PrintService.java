package com.asg.common.lib.service;

import com.asg.common.lib.security.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Slf4j
@Service
public class PrintService {

    private static final Map<String, JasperReport> CACHE = new ConcurrentHashMap<>();
    private static final String BASE_JASPER_PATH = "/jasper/";
    private final DataSource dataSource;

    public Map<String, Object> buildBaseParams(Long transactionPoid, String documentId) throws JRException {
        Map<String, Object> p = new HashMap<>();
        p.put("DOC_KEY_POID", transactionPoid);
        p.put("DOC_ID", documentId);
        p.put("SUBREPORT_DIR", "");
        p.put("DATE_TIME", new java.util.Date());
        p.put("LOGIN_COMP_POID", UserContext.getCompanyPoid());
        p.put("LOGIN_DIV_POID", getDivisionPoid(UserContext.getCompanyPoid()));
        p.put("LOGIN_GROUP_POID", UserContext.getGroupPoid());
        p.put("LOGIN_USER_POID", UserContext.getUserPoid());
        p.put("SUB_HEADER", load("Templates/DocHeaderSubReport.jrxml"));
        p.put("SUB_FOOTER", load("Templates/DocFooterSubReport.jrxml"));
        p.put("SUB_FOOTER_ISO", load("Templates/DocFooterSubReport-ISO.jrxml"));
        return p;
    }

    public JasperReport load(String relativePath) throws JRException {
        return CACHE.computeIfAbsent(relativePath, path -> {
            try {
                String filePath = BASE_JASPER_PATH + path;
                InputStream is = PrintService.class.getResourceAsStream(filePath);
                if (is == null) {
                    throw new RuntimeException("JRXML not found: " + filePath);
                }
                return JasperCompileManager.compileReport(is);
            } catch (JRException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public byte[] fillReportToPdf(JasperReport report, Map<String, Object> params, DataSource dataSource) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            params.put("REPORT_CLASS_LOADER", PrintService.class.getClassLoader());
            JasperPrint jasperPrint = JasperFillManager.fillReport(report, params, conn);
            JRPdfExporter exporter = new JRPdfExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
            SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
            if (params.containsKey("REPORT_TITLE")) {
                configuration.setMetadataTitle(params.get("REPORT_TITLE").toString());
            }
            exporter.setConfiguration(configuration);
            return JasperExportManager.exportReportToPdf(jasperPrint);
        }
    }

    private String getDivisionPoid(Long companyPoid) {
        if (companyPoid == null) return "581";
        try (Connection conn = dataSource.getConnection()) {
            var stmt = conn.prepareStatement("SELECT DIV_POID FROM GLOBAL_COMPANY_MASTER_DIV_DTL WHERE COMPANY_POID = ? AND ROWNUM = 1");
            stmt.setLong(1, companyPoid);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch division POID for company {}", companyPoid, e);
        }
        return "581";
    }
}
