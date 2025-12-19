package com.asg.common.lib.service;

import com.asg.common.lib.dto.DiffObject;
import com.asg.common.lib.dto.LogResponseDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.repository.LoggingRepository;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.DiffUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class LoggingService {

    @Autowired
    private LoggingRepository loggingRepository;

    @Autowired
    private DataSource dataSource;

    // ----------------------------------------------------------
    // READ SUMMARY LOGS
    // ----------------------------------------------------------
    public List<LogResponseDto> getLogSummary(String docId, Long docKeyPoid) {
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();

        return loggingRepository.getLogData(groupPoid, companyPoid, docId, docKeyPoid, "Summary")
                .stream().map(this::mapToLogResponseDto).toList();
    }

    // ----------------------------------------------------------
    // READ DETAIL LOGS
    // ----------------------------------------------------------
    public List<LogResponseDto> getDetailedLogs(String docId, Long docKeyPoid) {
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();

        return loggingRepository.getLogData(groupPoid, companyPoid, docId, docKeyPoid, "Details")
                .stream().map(this::mapToLogResponseDto).toList();
    }

    // ----------------------------------------------------------
    // INSERT SUMMARY LOG (PROC_UPDATE_LOG_SUMMARY)
    // ----------------------------------------------------------
    public void createLogSummaryEntry(LogDetailsEnum logType, String docId, String docKeyPoid) {

        Long userPoid = UserContext.getUserPoid();
        if (userPoid == null)
            throw new ValidationException("User not authenticated");

        if (docId == null)
            docId = UserContext.getDocumentId();

        // Build meaningful log text
        String logDetails = logType.getDescription() + " - DOC:" + docId + " KEY:" + docKeyPoid;

        try (Connection con = dataSource.getConnection();
             CallableStatement stmt = con.prepareCall("{call PROC_UPDATE_LOG_SUMMARY(?, ?, ?, ?, ?)}")) {

            stmt.setLong(1, userPoid);                                 // P_USER_POID
            stmt.setTimestamp(2, Timestamp.from(Instant.now()));      // P_LOGDATETIME
            stmt.setString(3, logDetails);                             // P_LOGDETAILS
            stmt.setString(4, docId);                                  // P_LOG_DOC_ID
            stmt.setString(5, docKeyPoid);                             // P_LOG_DOC_KEY_POID

            stmt.execute();

        } catch (SQLException e) {
            throw new RuntimeException("Error calling PROC_UPDATE_LOG_SUMMARY", e);
        }
    }

    // ----------------------------------------------------------
    // INSERT DETAIL LOG (PROC_UPDATE_LOG_DETAILS)
    // ----------------------------------------------------------
    public void createLogDetailsEntry(String docId, String docKeyPoid, String fieldName, String oldValue, String newValue, String logDetails, String logTable) {

        Long userPoid = UserContext.getUserPoid();
        if (userPoid == null)
            throw new ValidationException("User not authenticated");

        if (docId == null)
            docId = UserContext.getDocumentId();

        // Avoid NULLs — PL/SQL VARCHAR2 cannot accept null consistently in your system
        fieldName = fieldName == null ? "" : fieldName;
        oldValue = oldValue == null ? "" : oldValue;
        newValue = newValue == null ? "" : newValue;
        logDetails = logDetails == null ? "" : logDetails;
        logTable = logTable == null ? "" : logTable;

        try (Connection con = dataSource.getConnection();
             CallableStatement stmt = con.prepareCall(
                     "{call PROC_UPDATE_LOG_DETAILS(?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {

            stmt.setLong(1, userPoid);                                 // P_USER_POID
            stmt.setTimestamp(2, Timestamp.from(Instant.now()));      // P_LOGDATETIME
            stmt.setString(3, logDetails);                             // P_LOGDETAILS
            stmt.setString(4, docId);                                  // P_LOG_DOC_ID
            stmt.setString(5, docKeyPoid);                             // P_LOG_DOC_KEY_POID
            stmt.setString(6, fieldName);                              // P_FIELD_NAME
            stmt.setString(7, oldValue);                               // P_OLD_VALUE
            stmt.setString(8, newValue);                               // P_NEW_VALUE
            stmt.setString(9, logTable);                               // P_LOG_TABLE

            stmt.execute();

        } catch (SQLException e) {
            throw new RuntimeException("Error calling PROC_UPDATE_LOG_DETAILS", e);
        }
    }

    // ----------------------------------------------------------
    // MAP RESULT SET TO DTO
    // ----------------------------------------------------------
    private LogResponseDto mapToLogResponseDto(Map<String, Object> row) {
        return new LogResponseDto(
                (Timestamp) row.get("logDateTime"),
                (String) row.get("userName"),
                (Long) row.get("logUserPoid"),
                (String) row.get("logDetails"),
                (String) row.get("fieldName"),
                (String) row.get("oldValue"),
                (String) row.get("newValue")
        );
    }
    public <T> void logChanges(T oldObj, T newObj, Class<T> clazz, String documentId, String docKeyPoid, LogDetailsEnum logType, String keyIdLabel) {

        // 1) summary
        createLogSummaryEntry(logType, documentId, docKeyPoid);

        T oldCopy = null;
        if (oldObj != null) {
            try {
                oldCopy = clazz.getDeclaredConstructor().newInstance();
                BeanUtils.copyProperties(oldObj, oldCopy);
            } catch (Exception e) {
                throw new RuntimeException("Unable to copy old object for logging", e);
            }
        }

        // prefix
        String logDetail = String.format("KeyId = %s:%s", keyIdLabel, docKeyPoid);

        // 2) diff list
        List<DiffObject> diffs = DiffUtil.createDiffList(oldObj, newObj, clazz);

        // table
        String tableName = clazz.getAnnotation(jakarta.persistence.Table.class).name();

        // 3) detail logs
        for (DiffObject diff : diffs) {
            createLogDetailsEntry(documentId, docKeyPoid, diff.getFieldName(), diff.getOldValue(), diff.getNewValue(), logDetail, tableName
            );
        }
    }
    public void logSimpleFieldChange(Class<?> entityClass, String docId, String docKeyPoid,
                                     String fieldName, String oldVal, String newVal, String detailPrefix) {

        String tableName = entityClass.getAnnotation(jakarta.persistence.Table.class).name();
        createLogDetailsEntry(docId, docKeyPoid, fieldName, oldVal, newVal, detailPrefix, tableName);
    }

}
