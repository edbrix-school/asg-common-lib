package com.asg.common.lib.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.entity.DocumentEntity;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.repository.DocumentCommonRepository;
import com.asg.common.lib.security.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;

@Service
@Slf4j
public class DocumentDeleteService {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private LoggingService loggingService;

    @Autowired
    private DocumentCommonRepository documentCommonRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public String deleteDocument(Long docKeyPoid, String tableName, String poidColumnName,
                                 DeleteReasonDto deleteReason, LocalDate transactionDate) {
        
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();
        String docId = UserContext.getDocumentId();

        if (groupPoid == null || companyPoid == null || userPoid == null) {
            throw new ValidationException("User not authenticated");
        }

        if (docId == null || docId.isEmpty()) {
            throw new ValidationException("Document ID not found in context");
        }

        if (docKeyPoid == null) {
            throw new ValidationException("Please open an existing document");
        }

        DocumentEntity document = documentCommonRepository.findByDocId(docId);
        if (document == null) {
            throw new ValidationException("Document not found: " + docId);
        }

        String docType = document.getDocType();

        String sql = "{CALL PROC_GLOB_DOC_DELETE(?,?,?,?,?,?,?,?,?,?,?)}";

        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setLong(1, groupPoid);
            stmt.setLong(2, companyPoid);
            stmt.setLong(3, userPoid);
            stmt.setString(4, docId);
            stmt.setString(5, poidColumnName);
            stmt.setLong(6, docKeyPoid);
            stmt.setString(7, tableName);
            stmt.setString(8, "MARK_AS_DELETE");
            Date docDate = null;
            if (transactionDate != null) {
                docDate = Date.valueOf(transactionDate);
            }
            stmt.setDate(9, docDate);
            stmt.setString(10, docType);
            stmt.registerOutParameter(11, Types.VARCHAR);

            stmt.execute();

            String result = stmt.getString(11);

            if (result != null && result.contains("SUCCESS")) {
                String logDetails = deleteReason != null ? deleteReason.getDeleteReason() : "";
                loggingService.createLogSummaryEntry(
                        docId,
                        String.valueOf(docKeyPoid),
                        String.format("Deleted - %s - %s", docKeyPoid, logDetails)
                );
                return result;
            } else {
                String docRef = fetchDocRef(tableName, poidColumnName, docKeyPoid);
                loggingService.createLogSummaryEntry(
                        docId,
                        String.valueOf(docKeyPoid),
                        String.format("Error on delete - %s - %s", docRef, result)
                );
                throw new ValidationException("Some error occured while deleting: " + result);
            }

        } catch (SQLException e) {
            log.error("Error deleting document", e);
            throw new ValidationException("Error deleting document: " + e.getMessage());
        }
    }



    public String fetchDocRef(String tableName, String poidColumnName, Long docKeyPoid) {

        try {

            if (tableName == null || tableName.trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid table name");
            }
            if (poidColumnName == null || poidColumnName.trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid column name");
            }
            if (docKeyPoid == null) {
                throw new IllegalArgumentException("Invalid document key");
            }

            String sql = "SELECT DOC_REF FROM " + tableName +
                    " WHERE " + poidColumnName + " = ?";

            return jdbcTemplate.query(
                    sql,
                    ps -> ps.setLong(1, docKeyPoid),
                    rs -> rs.next() ? rs.getString("DOC_REF") : null
            );

        } catch (Exception ex) {
            log.warn("Unable to fetch DOC_REF", ex);
            return null;
        }
    }
}
