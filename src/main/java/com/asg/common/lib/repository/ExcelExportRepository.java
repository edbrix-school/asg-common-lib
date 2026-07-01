package com.asg.common.lib.repository;

import com.asg.common.lib.dto.excel.CellFormatType;
import com.asg.common.lib.dto.excel.ExcelColumnConfig;
import com.asg.common.lib.dto.excel.ExcelSheetConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;

import java.sql.*;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class ExcelExportRepository {

    private final JdbcTemplate jdbcTemplate;

    @Value("${db.connection:oracle}")
    private String dbConnection;

    private boolean isPostgres() {
        return "postgres".equalsIgnoreCase(dbConnection);
    }

    /**
     * Converts Oracle-style bracket list notation to PostgreSQL parenthesis notation.
     * e.g. COMPANY_POID=[999]  →  COMPANY_POID=(999)
     *      STATUS=[A,B,C]      →  STATUS=(A,B,C)
     */
    private String normalizeParametersForPostgres(String parameters) {
        if (parameters == null) return null;
        return parameters.replaceAll("=\\[([^\\]]*)]" , "=($1)");
    }

    public List<ExcelSheetConfig> getExcelConfig(Long groupPoid, Long companyPoid,
                                                  Long userPoid, String docId,
                                                  String docKeyPoid, String parameters) {
        return jdbcTemplate.execute((Connection conn) -> {
            if (isPostgres()) {
                return getExcelConfigPostgres(conn, groupPoid, companyPoid, userPoid, docId, docKeyPoid, parameters);
            } else {
                return getExcelConfigOracle(conn, groupPoid, companyPoid, userPoid, docId, docKeyPoid, parameters);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Oracle implementation
    // -------------------------------------------------------------------------

    private List<ExcelSheetConfig> getExcelConfigOracle(Connection conn, Long groupPoid, Long companyPoid,
                                                         Long userPoid, String docId,
                                                         String docKeyPoid, String parameters) throws SQLException {
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
                    ExcelSheetConfig sheet = getSheetDetailsOracle(conn, groupPoid, companyPoid, userPoid,
                            docId, rs.getString("SHEET_ID"), docKeyPoid, parameters);
                    sheet.setSheetName(rs.getString("EXCEL_SHEET_NAME"));
                    sheet.setExcelTemplateFile(rs.getString("EXCEL_TEMPLATE_FILE_NAME"));
                    sheets.add(sheet);
                }
            }
        }
        return sheets;
    }

    private ExcelSheetConfig getSheetDetailsOracle(Connection conn, Long groupPoid, Long companyPoid,
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

            return buildSheetConfig(stmt, sheetId);
        }
    }

    // -------------------------------------------------------------------------
    // PostgreSQL implementation
    // Postgres procedures with INOUT params must be called via plain
    // "CALL proc(...)" using prepareStatement + executeQuery.
    // The driver returns INOUT values as columns in a single result row.
    // -------------------------------------------------------------------------

    private List<ExcelSheetConfig> getExcelConfigPostgres(Connection conn, Long groupPoid, Long companyPoid,
                                                           Long userPoid, String docId,
                                                           String docKeyPoid, String parameters) throws SQLException {
        boolean prevAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            List<ExcelSheetConfig> sheets = new ArrayList<>();

            // Cast NULLs explicitly so Postgres resolves the correct overload:
            // proc_glob_doc_excel_hdr(numeric, text, numeric, text, numeric, text, refcursor, text)
            // p_doc_key_poid is always null so passed as NULL::numeric directly
            String sql = "CALL proc_glob_doc_excel_hdr(?::numeric, ?::text, ?::numeric, ?::text, NULL::numeric, ?::text, NULL::refcursor, NULL::text)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, groupPoid);
                stmt.setString(2, companyPoid != null ? companyPoid.toString() : null);
                stmt.setLong(3, userPoid);
                stmt.setString(4, docId);
                stmt.setString(5, normalizeParametersForPostgres(parameters));

                try (ResultSet rs = stmt.executeQuery()) {
                    // First row contains the INOUT values: outdata_summary cursor, p_status
                    if (rs.next()) {
                        String status = rs.getString("p_status");
                        if (status == null || !status.contains("SUCCESS")) {
                            throw new RuntimeException("DB Error: " + status);
                        }
                        // outdata_summary is a cursor name — open it
                        String cursorName = rs.getString("outdata_summary");
                        try (Statement fetchStmt = conn.createStatement()) {
                            try (ResultSet sheetRs = fetchStmt.executeQuery("FETCH ALL FROM \"" + cursorName + "\"")) {
                                while (sheetRs.next()) {
                                    ExcelSheetConfig sheet = getSheetDetailsPostgres(conn, groupPoid, companyPoid,
                                            userPoid, docId, sheetRs.getString("SHEET_ID"), docKeyPoid, parameters);
                                    sheet.setSheetName(sheetRs.getString("EXCEL_SHEET_NAME"));
                                    sheet.setExcelTemplateFile(sheetRs.getString("EXCEL_TEMPLATE_FILE_NAME"));
                                    sheets.add(sheet);
                                }
                            }
                        }
                    }
                }
            }
            conn.commit();
            return sheets;
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(prevAutoCommit);
        }
    }

    private ExcelSheetConfig getSheetDetailsPostgres(Connection conn, Long groupPoid, Long companyPoid,
                                                      Long userPoid, String docId, String sheetId,
                                                      String docKeyPoid, String parameters) throws SQLException {
        // Cast NULLs explicitly for all INOUT params so Postgres resolves the correct overload:
        // proc_glob_doc_excel_sheet(numeric, numeric[], numeric, text, numeric, numeric, text, refcursor, refcursor, refcursor, refcursor, refcursor, bytea, text)
        // p_doc_key_poid is always null so passed as NULL::numeric directly
        String sql = "CALL proc_glob_doc_excel_sheet(?::numeric, ?::numeric, ?::numeric, ?::text, ?::numeric, NULL::numeric, ?::text, NULL::refcursor, NULL::refcursor, NULL::refcursor, NULL::refcursor, NULL::refcursor, NULL::bytea, NULL::text)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, groupPoid);
            stmt.setLong(2, companyPoid);
            stmt.setLong(3, userPoid);
            stmt.setString(4, docId);
            stmt.setLong(5, Long.parseLong(sheetId));
            stmt.setString(6, normalizeParametersForPostgres(parameters));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("p_status");
                    if (status == null || !status.contains("SUCCESS")) {
                        throw new RuntimeException("DB Error: " + status);
                    }
                    return buildSheetConfigFromCursors(conn, rs, sheetId);
                }
            }
        }
        throw new RuntimeException("No result returned from proc_glob_doc_excel_sheet for sheetId: " + sheetId);
    }

    // -------------------------------------------------------------------------
    // Shared result-set mapping
    // -------------------------------------------------------------------------

    // Oracle: reads OUT cursors by parameter index from CallableStatement
    private static final int IDX_SUMMARY = 8;
    private static final int IDX_HDR     = 9;
    private static final int IDX_MAP     = 10;
    private static final int IDX_DTL     = 11;
    private static final int IDX_FTR     = 12;
    private static final int IDX_IMAGE   = 13;

    private ExcelSheetConfig buildSheetConfig(CallableStatement stmt, String sheetId) throws SQLException {
        ExcelSheetConfig.ExcelSheetConfigBuilder builder = ExcelSheetConfig.builder()
                .sheetId(sheetId)
                .headerImageBlob(stmt.getBlob(IDX_IMAGE));

        try (ResultSet rsSum = (ResultSet) stmt.getObject(IDX_SUMMARY)) {
            if (rsSum.next()) {
                builder.showHeaderImage("Y".equals(rsSum.getString("SHOW_HEADER_IMAGE")))
                       .headerImageEndRow(rsSum.getInt("HEADER_IMAGE_END_ROW"))
                       .dtl1ShowColHdr("Y".equals(rsSum.getString("DET1_SHOW_COL_HDR")))
                       .dtl1StartRow(rsSum.getInt("DET1_START_ROW"));
            }
        }

        try (ResultSet rsHdr = (ResultSet) stmt.getObject(IDX_HDR)) {
            builder.headerData(resultSetToMap(rsHdr));
        }

        try (ResultSet rsMap = (ResultSet) stmt.getObject(IDX_MAP)) {
            builder.columnConfigMap(buildColumnMap(rsMap));
        }

        try (ResultSet rsDtl = (ResultSet) stmt.getObject(IDX_DTL)) {
            builder.detailData(resultSetToList(rsDtl));
        }

        try (ResultSet rsFtr = (ResultSet) stmt.getObject(IDX_FTR)) {
            builder.footerData(resultSetToMap(rsFtr));
        }

        return builder.build();
    }

    // Postgres: INOUT refcursor params come back as cursor names in the result row.
    // p_hdr_image comes back as bytea (byte[]) — wrap in SerialBlob for Blob compatibility.
    private ExcelSheetConfig buildSheetConfigFromCursors(Connection conn, ResultSet rs, String sheetId) throws SQLException {
        Blob imageBlob = null;
        byte[] imageBytes = rs.getBytes("p_hdr_image");
        if (imageBytes != null && imageBytes.length > 0) {
            imageBlob = new javax.sql.rowset.serial.SerialBlob(imageBytes);
        }

        ExcelSheetConfig.ExcelSheetConfigBuilder builder = ExcelSheetConfig.builder()
                .sheetId(sheetId)
                .headerImageBlob(imageBlob);

        try (Statement s = conn.createStatement()) {
            try (ResultSet rsSum = s.executeQuery("FETCH ALL FROM \"" + rs.getString("outdata_summary") + "\"")) {
                if (rsSum.next()) {
                    builder.showHeaderImage("Y".equalsIgnoreCase(getStringSafe(rsSum, "show_header_image", "SHOW_HEADER_IMAGE")))
                           .headerImageEndRow(getIntSafe(rsSum, "header_image_end_row", "HEADER_IMAGE_END_ROW"))
                           .dtl1ShowColHdr("Y".equalsIgnoreCase(getStringSafe(rsSum, "det1_show_col_hdr", "DET1_SHOW_COL_HDR")))
                           .dtl1StartRow(getIntSafe(rsSum, "det1_start_row", "DET1_START_ROW"));
                }
            }

            try (ResultSet rsHdr = s.executeQuery("FETCH ALL FROM \"" + rs.getString("outdata1_hdr") + "\"")) {
                builder.headerData(resultSetToMap(rsHdr));
            }

            try (ResultSet rsMap = s.executeQuery("FETCH ALL FROM \"" + rs.getString("outdata2_map") + "\"")) {
                builder.columnConfigMap(buildColumnMap(rsMap));
            }

            try (ResultSet rsDtl = s.executeQuery("FETCH ALL FROM \"" + rs.getString("outdata3_dtl1") + "\"")) {
                builder.detailData(resultSetToList(rsDtl));
            }

            try (ResultSet rsFtr = s.executeQuery("FETCH ALL FROM \"" + rs.getString("outdata4_ftr") + "\"")) {
                builder.footerData(resultSetToMap(rsFtr));
            }
        }

        return builder.build();
    }

    private Map<String, ExcelColumnConfig> buildColumnMap(ResultSet rs) throws SQLException {
        Map<String, ExcelColumnConfig> map = new HashMap<>();
        while (rs.next()) {
            // Uppercase the key so lookups are case-insensitive across Oracle and Postgres
            String fieldName = rs.getString("field_name") != null
                    ? rs.getString("field_name").toUpperCase()
                    : rs.getString("FIELD_NAME").toUpperCase();
            map.put(fieldName, ExcelColumnConfig.builder()
                .fieldName(fieldName)
                .rowRef(getIntSafe(rs, "row_ref", "ROW_REF"))
                .colRef(getIntSafe(rs, "col_ref", "COL_REF"))
                .cellFormatType(parseCellFormat(getStringSafe(rs, "cell_format_type", "CELL_FORMAT_TYPE")))
                .showCaption("Y".equalsIgnoreCase(getStringSafe(rs, "show_caption", "SHOW_CAPTION")))
                .cellFormula(getStringSafe(rs, "cell_formula", "CELL_FORMULA"))
                .build());
        }
        return map;
    }

    private Map<String, Object> resultSetToMap(ResultSet rs) throws SQLException {
        Map<String, Object> map = new HashMap<>();
        if (rs.next()) {
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                // Uppercase all keys so writeCellData lookups work for both Oracle and Postgres
                map.put(meta.getColumnName(i).toUpperCase(), rs.getObject(i));
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
                // Uppercase all keys so writeDetailCell lookups work for both Oracle and Postgres
                row.put(meta.getColumnName(i).toUpperCase(), rs.getObject(i));
            }
            list.add(row);
        }
        return list;
    }

    // Safe column accessors: Postgres returns lowercase names, Oracle returns uppercase
    private String getStringSafe(ResultSet rs, String pgCol, String oraCol) {
        try { return rs.getString(pgCol); } catch (SQLException e) {
            try { return rs.getString(oraCol); } catch (SQLException ex) { return null; }
        }
    }

    private int getIntSafe(ResultSet rs, String pgCol, String oraCol) {
        try { return rs.getInt(pgCol); } catch (SQLException e) {
            try { return rs.getInt(oraCol); } catch (SQLException ex) { return 0; }
        }
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
