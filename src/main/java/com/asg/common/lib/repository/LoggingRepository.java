package com.asg.common.lib.repository;

import com.asg.common.lib.dto.request.LogFilterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class LoggingRepository {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> getLogData(Long groupPoid, Long companyPoid,
                                                String docId, Long docKeyPoid, String logType) {

        List<Map<String, Object>> results = new ArrayList<>();

        // pgjdbc's CallableStatement.registerOutParameter() only binds a REF_CURSOR correctly
        // when it's the first parameter; OUTDATA here is 6th of 6, so it was silently dropped
        // from the call actually sent to Postgres. Calling as a plain CALL via
        // PreparedStatement.executeQuery() sidesteps that restriction — Postgres returns the
        // cursor's name as an ordinary one-row ResultSet, then FETCH ALL reads the real rows.
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            String cursorName;
            try (PreparedStatement ps = connection.prepareStatement(
                    "CALL PROC_GLOB_LOG_LOADLIST(?, ?, ?, ?, ?, NULL::refcursor)")) {
                ps.setLong(1, groupPoid == null ? 0 : groupPoid);        // P_GROUP_POID
                ps.setLong(2, companyPoid == null ? 0 : companyPoid);    // P_COMPANY_POID
                ps.setString(3, docId);                                  // P_DOC_ID
                ps.setLong(4, docKeyPoid);                               // P_DOC_KEY_POID
                ps.setString(5, logType);                                // P_LOG_TYPE

                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    cursorName = rs.getString(1);
                }
            }

            try (Statement fetchStmt = connection.createStatement();
                 ResultSet rs = fetchStmt.executeQuery("FETCH ALL FROM \"" + cursorName + "\"")) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("logDateTime", rs.getTimestamp("LOG_DATETIME"));
                    row.put("userName", rs.getString("USER_NAME"));
                    row.put("logUserPoid", rs.getLong("LOG_USER_POID"));
                    row.put("logDetails", rs.getString("LOG_DETAILS"));
                    row.put("fieldName", rs.getString("FIELD_NAME"));
                    row.put("oldValue", rs.getString("OLD_VALUE"));
                    row.put("newValue", rs.getString("NEW_VALUE"));
                    results.add(row);
                }
            }
            connection.commit();

        } catch (SQLException e) {
            throw new RuntimeException("Error calling log procedure: " + e.getMessage(), e);
        }

        return results;
    }

    public Map<String, Object> getLogDataWithPagination(Long groupPoid, Long companyPoid,
                                                         String docId, Long docKeyPoid,
                                                         String logType, LogFilterRequest filter) {
        String table = "Summary".equals(logType) ? "GLOBAL_LOG_SUMMARY" : "GLOBAL_LOG_DETAILS";
        
        StringBuilder sql = new StringBuilder(
            "SELECT GL.LOG_DATETIME, GU.USER_NAME, GL.LOG_USER_POID, GL.LOG_DETAILS, " +
            ("Summary".equals(logType) ? "' ' FIELD_NAME, ' ' OLD_VALUE, ' ' NEW_VALUE" : 
                                          "GL.FIELD_NAME, GL.OLD_VALUE, GL.NEW_VALUE") +
            " FROM " + table + " GL " +
            "LEFT JOIN GLOBAL_USERS GU ON GL.LOG_USER_POID = GU.USER_POID " +
            "WHERE GL.LOG_DOC_ID = ? AND GL.LOG_DOC_KEY_POID = ?"
        );
        
        List<Object> params = new ArrayList<>();
        params.add(docId);
        params.add(String.valueOf(docKeyPoid));
        
        if (filter.getStartDate() != null) {
            sql.append(" AND GL.LOG_DATETIME::date >= ?");
            params.add(java.sql.Date.valueOf(filter.getStartDate()));
        }
        if (filter.getEndDate() != null) {
            sql.append(" AND GL.LOG_DATETIME::date <= ?");
            params.add(java.sql.Date.valueOf(filter.getEndDate()));
        }
        if (filter.getSearchText() != null && !filter.getSearchText().isEmpty()) {
            sql.append(" AND (UPPER(GU.USER_NAME) LIKE ? OR UPPER(GL.LOG_DETAILS) LIKE ?");
            if ("Details".equals(logType)) {
                sql.append(" OR UPPER(GL.FIELD_NAME) LIKE ? OR UPPER(GL.OLD_VALUE) LIKE ? OR UPPER(GL.NEW_VALUE) LIKE ?");
            }
            sql.append(")");
            String searchPattern = "%" + filter.getSearchText().toUpperCase() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
            if ("Details".equals(logType)) {
                params.add(searchPattern);
                params.add(searchPattern);
                params.add(searchPattern);
            }
        }
        
        String countSql = "SELECT COUNT(*) FROM (" + sql + ")";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        
        sql.append(" ORDER BY GL.LOG_DATETIME DESC");
        sql.append(" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add(filter.getPage() * filter.getSize());
        params.add(filter.getSize());
        
        List<Map<String, Object>> content = jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("logDateTime", rs.getTimestamp("LOG_DATETIME"));
            row.put("userName", rs.getString("USER_NAME"));
            row.put("logUserPoid", rs.getLong("LOG_USER_POID"));
            row.put("logDetails", rs.getString("LOG_DETAILS"));
            row.put("fieldName", rs.getString("FIELD_NAME"));
            row.put("oldValue", rs.getString("OLD_VALUE"));
            row.put("newValue", rs.getString("NEW_VALUE"));
            return row;
        });
        
        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("totalElements", total);
        result.put("page", filter.getPage());
        result.put("size", filter.getSize());
        result.put("totalPages", (int) Math.ceil((double) total / filter.getSize()));
        
        return result;
    }
}
