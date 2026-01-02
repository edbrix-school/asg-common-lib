package com.asg.common.lib.service;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JasperReportLoader {
    private static final Map<String, JasperReport> CACHE = new ConcurrentHashMap<>();
    private static final String BASE_JASPER_PATH = "/jasper/";

    public static JasperReport load(String relativePath) throws JRException {
        return CACHE.computeIfAbsent(relativePath, path -> {
            try {
                String filePath = BASE_JASPER_PATH + path;
                InputStream is = JasperReportLoader.class.getResourceAsStream(filePath);
                if (is == null) {
                    throw new RuntimeException("JRXML not found: " + filePath);
                }
                return JasperCompileManager.compileReport(is);
            } catch (JRException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static JasperReport loadSubreport(String path) throws JRException {
        InputStream is = JasperReportLoader.class.getResourceAsStream(path);

        if (is == null) {
            throw new JRException("Subreport not found at: " + path);
        }

        return JasperCompileManager.compileReport(is);
    }

}
