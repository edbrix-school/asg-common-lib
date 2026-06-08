package com.asg.common.lib.service;

import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.JRPrintServiceExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePrintServiceExporterConfiguration;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import org.springframework.stereotype.Service;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.JobName;
import javax.print.PrintServiceLookup;
import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.Arrays;
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
    private final LoggingService loggingService;

    public Map<String, Object> buildBaseParams(Long transactionPoid, String documentId) throws JRException {
        Map<String, Object> p = new HashMap<>();
        p.put("DOC_KEY_POID", transactionPoid);
        p.put("DOC_ID", documentId);
        p.put("SUBREPORT_DIR", "");
        p.put("DATE_TIME", new java.util.Date());
        p.put("REPORT_DATE", Timestamp.valueOf(DateUtil.getCurrentDateTimeInUserTimeZone()));
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
            exporter.exportReport();
            if (null != loggingService
                    && params.containsKey("DOC_KEY_POID") && params.get("DOC_KEY_POID") != null
                    && params.containsKey("DOC_ID") && params.get("DOC_ID") != null) {
                loggingService.createLogSummaryEntry(LogDetailsEnum.PREVIEWED_OR_PRINTED_OR_DOWNLOADED,
                        params.get("DOC_ID").toString(),
                        params.get("DOC_KEY_POID").toString());
            }
            return outputStream.toByteArray();
        }
    }

    /**
     * Print a JasperReport silently to a named OS/network printer.
     * This does not return a PDF to the caller.
     */
    public void printReportToPrinter(JasperReport report,
                                     Map<String, Object> params,
                                     DataSource dataSource,
                                     String printerName,
                                     int copies) throws Exception {
        if (printerName == null || printerName.isBlank()) {
            throw new IllegalArgumentException("printerName is required");
        }
        int effectiveCopies = Math.max(1, copies);

        javax.print.PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        log.info("the printer has fetched and total number of printers available are " + services.length);
        Arrays.stream(services).forEach(val -> log.info("the printer name was " + val.getName()));
        javax.print.PrintService selected = null;
        if (services != null) {
            for (javax.print.PrintService s : services) {
                if (s != null && printerName.equalsIgnoreCase(s.getName())) {
                    selected = s;
                    break;
                }
            }
        }
        if (selected == null) {
            throw new RuntimeException("Printer not found: " + printerName);
        }

        try (Connection conn = dataSource.getConnection()) {
            params.put("REPORT_CLASS_LOADER", PrintService.class.getClassLoader());
            JasperPrint jasperPrint = JasperFillManager.fillReport(report, params, conn);

            JRPrintServiceExporter exporter = new JRPrintServiceExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));

            PrintRequestAttributeSet printRequestAttributes = new HashPrintRequestAttributeSet();
            printRequestAttributes.add(new Copies(effectiveCopies));
            String jobName = params.get("DOC_ID").toString() + "_" + params.get("DOC_KEY_POID").toString();
            printRequestAttributes.add(new JobName(jobName, null));

            SimplePrintServiceExporterConfiguration configuration = new SimplePrintServiceExporterConfiguration();
            configuration.setPrintService(selected);
            configuration.setPrintRequestAttributeSet(printRequestAttributes);
            configuration.setDisplayPageDialog(false);
            configuration.setDisplayPrintDialog(false);
            exporter.setConfiguration(configuration);

            exporter.exportReport();

            if (null != loggingService
                    && params.containsKey("DOC_KEY_POID") && params.get("DOC_KEY_POID") != null
                    && params.containsKey("DOC_ID") && params.get("DOC_ID") != null) {
                loggingService.createLogSummaryEntry(LogDetailsEnum.PREVIEWED_OR_PRINTED_OR_DOWNLOADED,
                        params.get("DOC_ID").toString(),
                        params.get("DOC_KEY_POID").toString());
            }
        }
    }

    private String getDivisionPoid(Long companyPoid) {
        if (companyPoid == null) return "581";
        try (Connection conn = dataSource.getConnection()) {
            var stmt = conn.prepareStatement("SELECT DIV_POID FROM GLOBAL_COMPANY_MASTER_DIV_DTL WHERE COMPANY_POID = ? FETCH FIRST 1 ROW ONLY;");
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
