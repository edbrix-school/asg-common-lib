package com.asg.common.lib.service;

import com.asg.common.lib.dto.DiffObject;
import com.asg.common.lib.dto.request.LogFilterRequest;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.dto.response.LogResponseDto;
import com.asg.common.lib.dto.response.PagedLogResponse;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.repository.LoggingRepository;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.DateUtil;
import com.asg.common.lib.utility.DiffUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
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
    // READ SUMMARY LOGS WITH PAGINATION AND FILTERS
    // ----------------------------------------------------------
    public PagedLogResponse getLogSummaryPaged(String docId, Long docKeyPoid, LogFilterRequest filter) {
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();

        Map<String, Object> result = loggingRepository.getLogDataWithPagination(
                groupPoid, companyPoid, docId, docKeyPoid, "Summary", filter);

        return mapToPagedResponse(result);
    }

    // ----------------------------------------------------------
    // READ DETAIL LOGS WITH PAGINATION AND FILTERS
    // ----------------------------------------------------------
    public PagedLogResponse getDetailedLogsPaged(String docId, Long docKeyPoid, LogFilterRequest filter) {
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();

        Map<String, Object> result = loggingRepository.getLogDataWithPagination(
                groupPoid, companyPoid, docId, docKeyPoid, "Details", filter);

        return mapToPagedResponse(result);
    }

    // ----------------------------------------------------------
    // INSERT SUMMARY LOG (PROC_UPDATE_LOG_SUMMARY)
    // ----------------------------------------------------------
    public void createLogSummaryEntry(LogDetailsEnum logType, String docId, String docKeyPoid) {

        if (LogDetailsEnum.VIEWED.equals(logType) && BooleanUtils.isFalse(UserContext.isLogEnabled()))
            return;

        // Build meaningful log text
        String logDetails = logType.getDescription() + " - DOC:" + docId + " KEY:" + docKeyPoid;
        if (logType.equals(LogDetailsEnum.VIEWED) || logType.equals(LogDetailsEnum.MODIFIED)) {
            logDetails = logType.getDescription();
        }


        createLogSummaryEntry(docId, docKeyPoid ,logDetails);
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
            stmt.setTimestamp(2, DateUtil.getCurrentDateTimeInUserTimeZoneTimeStamp());      // P_LOGDATETIME
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
        Timestamp ts = (Timestamp) row.get("logDateTime");
        return new LogResponseDto(
                ts != null ? ts.toLocalDateTime() : null,
                (String) row.get("userName"),
                (Long) row.get("logUserPoid"),
                (String) row.get("logDetails"),
                (String) row.get("fieldName"),
                (String) row.get("oldValue"),
                (String) row.get("newValue")
        );
    }

    // ----------------------------------------------------------
    // MAP PAGINATED RESULT TO DTO
    // ----------------------------------------------------------
    @SuppressWarnings("unchecked")
    private PagedLogResponse mapToPagedResponse(Map<String, Object> result) {
        List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
        List<LogResponseDto> logs = content.stream().map(this::mapToLogResponseDto).toList();

        return new PagedLogResponse(
                logs,
                (Integer) result.get("page"),
                (Integer) result.get("size"),
                (Long) result.get("totalElements"),
                (Integer) result.get("totalPages")
        );
    }
    public <T> void logChanges(T oldObj, T newObj, Class<T> clazz, String documentId, String docKeyPoid, LogDetailsEnum logType, String keyIdLabel) {

        // 1) summary
        createLogSummaryEntry(logType, documentId, docKeyPoid);


        logDetails(oldObj, newObj, clazz, documentId, docKeyPoid, keyIdLabel);
    }

    // Same as logChanges, but embeds a document reference (e.g. DOC_REF) in the summary line
    // instead of the bare type description, so the summary log reads "Modified DOC-123" not just "Modified".
    public <T> void logchange_v2(T oldObj, T newObj, Class<T> clazz, String documentId, String docKeyPoid,
                                 LogDetailsEnum logType, String docRef, String keyIdLabel) {

        String summary = (docRef == null || docRef.isBlank())
                ? logType.getDescription()
                : String.format("%s %s", logType.getDescription(), docRef);

        // 1) summary
        createLogSummaryEntry(documentId, docKeyPoid, summary);

        // 2) detail diffs
        logDetails(oldObj, newObj, clazz, documentId, docKeyPoid, keyIdLabel);
    }

    public  <T> void logDetails(T oldObj, T newObj, Class<T> clazz, String documentId, String docKeyPoid, String keyIdLabel) {
        // prefix
        String logDetail = String.format("KeyId = %s:%s", keyIdLabel, docKeyPoid);

        // 2) diff list
        createLog(oldObj, newObj, clazz, documentId, docKeyPoid, logDetail);
    }

    public  <T> void createLog(T oldObj, T newObj, Class<T> clazz, String documentId, String docKeyPoid, String logDetail) {
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

    public void createLogSummaryEntry(String docId, String docKeyPoid, String logDetails) {

        Long userPoid = UserContext.getUserPoid();
        if (userPoid == null)
            throw new ValidationException("User not authenticated");

        if (docId == null)
            docId = UserContext.getDocumentId();

        // Build meaningful log text

        // PROC_UPDATE_LOG_SUMMARY's P_LOGDATETIME was migrated to Postgres as `date`, not
        // `timestamp` (Oracle's DATE carries a time component; Postgres's doesn't). Binding a
        // java.sql.Timestamp via setTimestamp() sends a timestamp-typed argument, and Postgres
        // only allows an assignment cast from timestamp to date, not an implicit one used during
        // procedure-argument resolution, so the call fails to resolve at all. Casting explicitly
        // in the SQL text (?::date) coerces the argument before resolution happens, sidestepping
        // that restriction — at the cost of the time-of-day being dropped, same as the DB proc
        // would do regardless of how the call arrives, until its parameter type is corrected.
        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement("CALL PROC_UPDATE_LOG_SUMMARY(?, ?::date, ?, ?, ?)")) {

            stmt.setLong(1, userPoid);                                 // P_USER_POID
            stmt.setTimestamp(2, DateUtil.getCurrentDateTimeInUserTimeZoneTimeStamp());      // P_LOGDATETIME
            stmt.setString(3, logDetails);                             // P_LOGDETAILS
            stmt.setString(4, docId);                                  // P_LOG_DOC_ID
            stmt.setString(5, docKeyPoid);                             // P_LOG_DOC_KEY_POID

            stmt.execute();

        } catch (SQLException e) {
            throw new RuntimeException("Error calling PROC_UPDATE_LOG_SUMMARY", e);
        }
    }

    public <T> void createLogBatch(List<LogRequestDto<T>> logRequests) {
        for (LogRequestDto<T> request : logRequests) {
            createLog(request.getOldObj(), request.getNewObj(), request.getClazz(),
                    request.getDocumentId(), request.getDocKeyPoid(), request.getLogDetail());
        }
    }

    public <T> String getEntityDataString(T entity) {
        if (entity == null) return "";

        StringBuilder rowData = new StringBuilder();
        java.lang.reflect.Field[] fields = entity.getClass().getDeclaredFields();

        for (java.lang.reflect.Field field : fields) {
            if (field.isAnnotationPresent(com.asg.common.lib.annotation.AuditIgnore.class)) continue;
            field.setAccessible(true);
            try {
                Object value = field.get(entity);
                if (value != null && !value.toString().isEmpty()) {
                    rowData.append(field.getName()).append("=").append(value).append(";");
                }
            } catch (IllegalAccessException e) {
                log.warn("Unable to access field: {}", field.getName());
            }
        }

        return rowData.toString();
    }

    public <T> void logDelete(T entity, String docId, String docKeyPoid) {
        String rowData = getEntityDataString(entity);
        createLogSummaryEntry(docId, docKeyPoid, "Row Deleted : " + rowData);
    }

}
