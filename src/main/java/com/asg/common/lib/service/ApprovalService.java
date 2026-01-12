package com.asg.common.lib.service;

import com.asg.common.lib.security.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalService {

    private final DataSource dataSource;

    public String getApprovalStatus(String docId, Object docKeyPoid) {
        if (docId == null || docId.isEmpty()) {
            return "ERROR : DocId is required for checking the status";
        }

        if (docKeyPoid == null) {
            return "ERROR : DocKeyPoid is required for checking the status";
        }

        String sql = "BEGIN ? := FUNC_GLOB_APPROVAL_STATUS(?,?,?,?,?); END;";

        try (Connection conn = dataSource.getConnection();
             CallableStatement statement = conn.prepareCall(sql)) {

            statement.registerOutParameter(1, Types.VARCHAR);
            statement.setLong(2, UserContext.getGroupPoid());
            statement.setLong(3, UserContext.getCompanyPoid());
            statement.setLong(4, UserContext.getUserPoid());
            statement.setString(5, docId);
            statement.setObject(6, docKeyPoid);
            statement.execute();

            String result = statement.getString(1);
            return (result == null || result.isEmpty()) 
                ? "ERROR : Approval status returned null value..." 
                : result;

        } catch (SQLException e) {
            log.error("Error getting approval status for docId: {}, docKeyPoid: {}", docId, docKeyPoid, e);
            return "ERROR : " + e.getMessage();
        }
    }
}
