package com.asg.common.lib.repository;

import org.springframework.beans.factory.annotation.Autowired;
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

    public List<Map<String, Object>> getLogData(Long groupPoid, Long companyPoid,
                                                String docId, Long docKeyPoid, String logType) {

        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             CallableStatement stmt =
                     connection.prepareCall("{call PROC_GLOB_LOG_LOADLIST(?, ?, ?, ?, ?, ?)}")) {

            // Set all 6 parameters correctly
            stmt.setLong(1, groupPoid == null ? 0 : groupPoid);        // P_GROUP_POID
            stmt.setLong(2, companyPoid == null ? 0 : companyPoid);    // P_COMPANY_POID
            stmt.setString(3, docId);                                  // P_DOC_ID
            stmt.setLong(4, docKeyPoid);                               // P_DOC_KEY_POID
            stmt.setString(5, logType);                                // P_LOG_TYPE
            stmt.registerOutParameter(6, Types.REF_CURSOR);            // OUTDATA

            stmt.execute();

            try (ResultSet rs = (ResultSet) stmt.getObject(6)) {
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

        } catch (SQLException e) {
            throw new RuntimeException("Error calling log procedure: " + e.getMessage(), e);
        }

        return results;
    }
}
