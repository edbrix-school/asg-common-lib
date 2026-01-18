package com.asg.common.lib.repository;

import com.asg.common.lib.dto.excel.CellFormatType;
import com.asg.common.lib.dto.excel.ExcelColumnConfig;
import com.asg.common.lib.dto.excel.ExcelSheetConfig;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

import java.sql.*;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class ExcelExportRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<ExcelSheetConfig> getExcelConfig(Long groupPoid, Long companyPoid, 
                                                  Long userPoid, String docId, 
                                                  String docKeyPoid, String parameters) {
        return jdbcTemplate.execute((Connection conn) -> {
            List<ExcelSheetConfig> sheets = new ArrayList<>();
            
            try (CallableStatement stmt = conn.prepareCall(
                    "BEGIN PROC_GLOB_DOC_EXCEL_HDR(?,?,?,?,?,?,?,?); END;")) {
                
                stmt.setLong(1, groupPoid);
                stmt.setLong(2, companyPoid);
                stmt.setLong(3, userPoid);
                stmt.setString(4, docId);
                stmt.setString(5, docKeyPoid);
                stmt.setString(6, parameters);
                stmt.registerOutParameter(7, Types.REF_CURSOR);
                stmt.registerOutParameter(8, Types.VARCHAR);
                stmt.execute();
                
                String status = stmt.getString(8);
                if (!status.contains("SUCCESS")) {
                    throw new RuntimeException("DB Error: " + status);
                }
                
                try (ResultSet rs = (ResultSet) stmt.getObject(7)) {
                    while (rs.next()) {
                        ExcelSheetConfig sheet = getSheetDetails(conn, groupPoid, companyPoid, userPoid, 
                            docId, rs.getString("SHEET_ID"), docKeyPoid, parameters);
                        sheet.setSheetName(rs.getString("EXCEL_SHEET_NAME"));
                        sheet.setExcelTemplateFile(rs.getString("EXCEL_TEMPLATE_FILE_NAME"));
                        sheets.add(sheet);
                    }
                }
            }
            return sheets;
        });
    }

    private ExcelSheetConfig getSheetDetails(Connection conn, Long groupPoid, Long companyPoid,
                                             Long userPoid, String docId, String sheetId,
                                             String docKeyPoid, String parameters) throws SQLException {
        try (CallableStatement stmt = conn.prepareCall(
                "BEGIN PROC_GLOB_DOC_EXCEL_SHEET(?,?,?,?,?,?,?,?,?,?,?,?,?,?); END;")) {
            
            stmt.setLong(1, groupPoid);
            stmt.setLong(2, companyPoid);
            stmt.setLong(3, userPoid);
            stmt.setString(4, docId);
            stmt.setString(5, sheetId);
            stmt.setString(6, docKeyPoid);
            stmt.setString(7, parameters);
            stmt.registerOutParameter(8, Types.REF_CURSOR);
            stmt.registerOutParameter(9, Types.REF_CURSOR);
            stmt.registerOutParameter(10, Types.REF_CURSOR);
            stmt.registerOutParameter(11, Types.REF_CURSOR);
            stmt.registerOutParameter(12, Types.REF_CURSOR);
            stmt.registerOutParameter(13, Types.BLOB);
            stmt.registerOutParameter(14, Types.VARCHAR);
            stmt.executeUpdate();
            
            String status = stmt.getString(14);
            if (!status.contains("SUCCESS")) {
                throw new RuntimeException("DB Error: " + status);
            }
            
            ExcelSheetConfig.ExcelSheetConfigBuilder builder = ExcelSheetConfig.builder()
                .sheetId(sheetId)
                .headerImageBlob(stmt.getBlob(13));
            
            try (ResultSet rsSum = (ResultSet) stmt.getObject(8)) {
                if (rsSum.next()) {
                    builder.showHeaderImage("Y".equals(rsSum.getString("SHOW_HEADER_IMAGE")))
                           .headerImageEndRow(rsSum.getInt("HEADER_IMAGE_END_ROW"))
                           .dtl1ShowColHdr("Y".equals(rsSum.getString("DET1_SHOW_COL_HDR")))
                           .dtl1StartRow(rsSum.getInt("DET1_START_ROW"));
                }
            }
            
            try (ResultSet rsHdr = (ResultSet) stmt.getObject(9)) {
                builder.headerData(resultSetToMap(rsHdr));
            }
            
            try (ResultSet rsMap = (ResultSet) stmt.getObject(10)) {
                builder.columnConfigMap(buildColumnMap(rsMap));
            }
            
            try (ResultSet rsDtl = (ResultSet) stmt.getObject(11)) {
                builder.detailData(resultSetToList(rsDtl));
            }
            
            try (ResultSet rsFtr = (ResultSet) stmt.getObject(12)) {
                builder.footerData(resultSetToMap(rsFtr));
            }
            
            return builder.build();
        }
    }

    private Map<String, ExcelColumnConfig> buildColumnMap(ResultSet rs) throws SQLException {
        Map<String, ExcelColumnConfig> map = new HashMap<>();
        while (rs.next()) {
            String fieldName = rs.getString("FIELD_NAME").toUpperCase();
            map.put(fieldName, ExcelColumnConfig.builder()
                .fieldName(fieldName)
                .rowRef(rs.getInt("ROW_REF"))
                .colRef(rs.getInt("COL_REF"))
                .cellFormatType(parseCellFormat(rs.getString("CELL_FORMAT_TYPE")))
                .showCaption("Y".equals(rs.getString("SHOW_CAPTION")))
                .cellFormula(rs.getString("CELL_FORMULA"))
                .build());
        }
        return map;
    }

    private Map<String, Object> resultSetToMap(ResultSet rs) throws SQLException {
        Map<String, Object> map = new HashMap<>();
        if (rs.next()) {
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                map.put(meta.getColumnName(i), rs.getObject(i));
            }
        }
        return map;
    }

    private List<Map<String, Object>> resultSetToList(ResultSet rs) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                row.put(meta.getColumnName(i), rs.getObject(i));
            }
            list.add(row);
        }
        return list;
    }

    private CellFormatType parseCellFormat(String format) {
        if (format == null) return CellFormatType.NONE;
        try {
            return CellFormatType.valueOf(format);
        } catch (IllegalArgumentException e) {
            return CellFormatType.NONE;
        }
    }
}
