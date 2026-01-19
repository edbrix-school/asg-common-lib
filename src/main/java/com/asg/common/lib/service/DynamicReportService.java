package com.asg.common.lib.service;

import com.asg.common.lib.dto.excel.ExcelFileData;
import com.asg.common.lib.repository.DynamicReportRepository;
import com.asg.common.lib.security.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicReportService {

    private final JdbcTemplate jdbcTemplate;
    private final ExcelExportService excelExportService;
    private final DynamicReportRepository dynamicReportRepository;

    public ExcelFileData exportToExcel(String docId, Map<String, Object> uiFilters, String rptName) {
        try {
            List<Map<String, Object>> dbFilters  = populateReportFilters(docId);
            Map<String, Object> parameters = mergeFinalFilters(uiFilters, dbFilters);
            String fileName = (rptName != null && !rptName.isEmpty()) ? rptName + ".xlsx" : "report.xlsx";
            return excelExportService.generateExcel(docId, null, parameters, fileName);
        } catch (Exception e) {
            log.error("Failed to export Excel for docId: {}", docId, e);
            throw new RuntimeException("Excel export failed: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> populateReportFilters(String docId) {
        return jdbcTemplate.execute((Connection conn) -> {
            List<Map<String, Object>> reportFilter = new ArrayList<>();
            try (CallableStatement stmt = conn.prepareCall(
                    "BEGIN ? := FUNC_DYNAMIC_RPT_FILTERS(?,?); END;")) {
                stmt.registerOutParameter(1, Types.REF_CURSOR);
                stmt.setLong(2, getGroupPoid());
                stmt.setString(3, docId);
                stmt.execute();
                try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                    int detRowId = 0;
                    while (rs.next()) {
                        detRowId++;
                        Map<String, Object> filterRowMap = new HashMap<>();
                        filterRowMap.put("DetRowId", detRowId);
                        filterRowMap.put("FILTER_NAME", rs.getString("FILTER_NAME"));
                        filterRowMap.put("SQL_COLUMN_NAME", rs.getString("SQL_COLUMN_NAME"));
                        filterRowMap.put("FILTER_TYPE", rs.getString("FILTER_TYPE"));
                        filterRowMap.put("DEFAULT_VALUE", getDefaultValue1Parsed(rs.getString("SQL_COLUMN_NAME"), rs.getString("DEFAULT_VALUE")));
                        filterRowMap.put("DEFAULT_VALUE2", getDefaultValue2Parsed(rs.getString("SQL_COLUMN_NAME"), rs.getString("DEFAULT_VALUE")));
                        filterRowMap.put("MANDATATORY", rs.getString("MANDATATORY"));
                        filterRowMap.put("PANEL", rs.getString("PANEL"));
                        filterRowMap.put("LOV_NAME", rs.getString("LOV_NAME"));
                        filterRowMap.put("LOV_RETURN_TYPE", rs.getString("LOV_RETURN_TYPE"));
                        filterRowMap.put("ADD_TO_WHERE", rs.getString("ADD_TO_WHERE"));
                        filterRowMap.put("REFRESH_AFTER_CHANGE", rs.getString("REFRESH_AFTER_CHANGE"));
                        filterRowMap.put("CLEAR_AFTER_CHANGE", rs.getString("CLEAR_AFTER_CHANGE"));
                        filterRowMap.put("CLEAR_FILTER_AFTER_REFRESH", rs.getString("CLEAR_FILTER_AFTER_REFRESH"));
                        reportFilter.add(filterRowMap);
                    }
                }
            }
            return reportFilter;
        });
    }

    public Map<String, Object> mergeFinalFilters(Map<String, Object> uiFilters, List<Map<String, Object>> dbFilters) {
        Map<String, Object> finalFilters = new HashMap<>(uiFilters != null ? uiFilters : new HashMap<>());
        for (Map<String, Object> filterRow : dbFilters) {
            String colName = filterRow.get("SQL_COLUMN_NAME").toString().trim();
            String filterType = filterRow.get("FILTER_TYPE").toString();
            if (!finalFilters.containsKey(colName)) {
                Object colValue = filterRow.get("DEFAULT_VALUE");
                if (colValue != null) {
                    finalFilters.put(colName, formatFilterValue(colValue, filterType));
                }
                Object colValue2 = filterRow.get("DEFAULT_VALUE2");
                if (colValue2 != null) {
                    finalFilters.put(colName + "2", formatFilterValue(colValue2, filterType));
                }
            }
        }
        return finalFilters;
    }
    
    private String formatFilterValue(Object value, String filterType) {
        if (filterType.contains("Date")) {
            try {
                java.util.Date date = (java.util.Date) value;
                return getDateFormatString(date);
            } catch (Exception e) {
                try {
                    SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy");
                    java.util.Date date = formatter.parse(value.toString());
                    return getDateFormatString(date);
                } catch (ParseException pe) {
                    return value.toString();
                }
            }
        }
        return value.toString();
    }
    
    private String getDateFormatString(java.util.Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy");
        return formatter.format(date);
    }

    private Object getDefaultValue1Parsed(String filterColName, String defValueType) {
        Object defValue1 = null;
        if (defValueType == null) return null;
        switch (defValueType) {
            case "#MONTH#":
            case "#MONTH_TODAY#":
                defValue1 = getDateTruncated(getDateCurrentMonthStart());
                break;
            case "#YEAR#":
                defValue1 = getDateTruncated(getLoginReportPeriodStart());
                break;
            case "#YEAR_TO_DATE#":
                defValue1 = getDateTruncated(getLoginReportPeriodStart());
                break;
            case "#TODAY#":
                defValue1 = getDateTruncated((Date) new java.util.Date());
                break;
            case "#LOGIN_COMPANY_POID#":
                defValue1 = new BigDecimal(1); // This would come from session
                break;
            default:
                defValue1 = defValueType;
                break;
        }
        return defValue1;
    }

    private Object getDefaultValue2Parsed(String filterColName, String defValueType) {
        Object defValue2 = null;
        if (defValueType == null) return null;
        switch (defValueType) {
            case "#MONTH#":
                defValue2 = getDateMonthEnd((Date) new java.util.Date());
                break;
            case "#MONTH_TODAY#":
                defValue2 = new java.util.Date();
                break;
            case "#YEAR#":
                defValue2 = getLoginReportPeriodEnd();
                break;
            case "#YEAR_TO_DATE#":
                defValue2 = new java.util.Date();
                break;
            case "#TODAY#":
                defValue2 = new java.util.Date();
                break;
            case "#LOGIN_COMPANY_POID#":
                defValue2 = getCompanyPoid();
                break;
            default:
                defValue2 = defValueType;
                break;
        }
        return defValue2;
    }

    private Date getDateTruncated(Date dataData) {
        try {
            if (dataData == null)
                return null;
            else {
                Calendar cal = Calendar.getInstance(); // locale-specific
                cal.setTime(dataData);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                return (Date) cal.getTime();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private Date getDateCurrentMonthStart() {
        try {
            Calendar cal = Calendar.getInstance(); // locale-specific
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return (Date) cal.getTime();
        } catch (Exception e) {
            return null;
        }
    }

    private Date getDateMonthEnd(Date date) {
        try {
            Calendar cal = Calendar.getInstance(); // locale-specific
            cal.setTime(date);
            //last day of month
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return (Date) cal.getTime();
        } catch (Exception e) {
            return null;
        }
    }

    private Date getLoginReportPeriodStart() {
        Map<String, Object> companyData = dynamicReportRepository.getLoginReportPeriod(getCompanyPoid());
        return (Date) (companyData != null ? companyData.get("REPORT_PERIOD_START") : new java.util.Date());
    }

    private Date getLoginReportPeriodEnd() {
        Map<String, Object> companyData = dynamicReportRepository.getLoginReportPeriod(getCompanyPoid());
        return (Date) (companyData != null ?  companyData.get("REPORT_PERIOD_END") : new java.util.Date());
    }

    private Long getGroupPoid() {
        try {
            Long groupPoid = UserContext.getGroupPoid();
            return groupPoid != null ? groupPoid : 1L;
        } catch (Exception e) {
            return 1L;
        }
    }

    private Long getCompanyPoid() {
        try {
            Long companyPoid = UserContext.getCompanyPoid();
            return companyPoid != null ? companyPoid : 1L;
        } catch (Exception e) {
            return 1L;
        }
    }
}