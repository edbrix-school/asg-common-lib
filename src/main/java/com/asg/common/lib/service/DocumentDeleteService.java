package com.asg.common.lib.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.entity.DocumentEntity;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.repository.DocumentCommonRepository;
import com.asg.common.lib.security.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

    @Autowired
    private DocumentSearchService documentSearchService;

    private Date transactionPeriodStart;
    private Date transactionPeriodEnd;
    private Date financialPeriodStart;
    private Date financialPeriodEnd;
    private Date stockPeriodStart;
    private Date stockPeriodEnd;

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
        DocumentSearchService.DocumentInfo info = null;
        
        if ("Transactions".equalsIgnoreCase(docType)) {
            info = documentSearchService.loadDocumentInfo(docId);
        }
        
        boolean isGLTransaction = info != null && info.isGlDocument() && "Transactions".equals(docType);
        boolean isStockDocument = info != null && info.isInventoryDocument() && "Transactions".equals(docType);

        if (isGLTransaction) {
            transactionPeriodStart = info.getTransPeriodStart();
            transactionPeriodEnd = info.getTransPeriodEnd();
            loadFinancialPeriodFromCompanyMaster(companyPoid);
        }
        
        if (isStockDocument) {
            stockPeriodStart = info.getStockPeriodStart();
            stockPeriodEnd = info.getStockPeriodEnd();
        }

        boolean hasEditPermission = grantEditPermissionGetStatus(docKeyPoid, docId);
        
        Date docDate = transactionDate != null ? Date.valueOf(transactionDate) : null;
        
        if (isGLTransaction && !isThisDateWithinValidTransactionPeriod(docDate)) {
            if (!hasEditPermission) {
                throw new ValidationException("This Document is not within the transaction period");
            }
        }
        
        if (isGLTransaction && !isThisDateWithinValidFinancialPeriod(docDate)) {
            throw new ValidationException("This Document is not within the financial period");
        }
        
        if (isStockDocument && !isThisDateWithinValidStockPeriod(docDate)) {
            if (!hasEditPermission) {
                throw new ValidationException("This Document is not within the stock period");
            }
        }

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



    public boolean isThisDateWithinValidTransactionPeriod(Date dateField) {
        if (dateField == null) {
            return false;
        }

        if (transactionPeriodStart == null || transactionPeriodEnd == null) {
            throw new ValidationException("Transaction period is invalid for this company");
        }

        return !dateField.before(transactionPeriodStart) && !dateField.after(transactionPeriodEnd);
    }

    public boolean isThisDateWithinValidFinancialPeriod(Date dateField) {
        if (dateField == null) {
            return false;
        }

        if (financialPeriodStart == null || financialPeriodEnd == null) {
            throw new ValidationException("Financial period is invalid for this company (some null values)");
        }

        if (financialPeriodStart.after(financialPeriodEnd)) {
            throw new ValidationException("Financial period is invalid for this company (start date is after end date)");
        }

        return !dateField.before(financialPeriodStart) && !dateField.after(financialPeriodEnd);
    }

    public boolean isThisDateWithinValidStockPeriod(Date dateField) {
        if (dateField == null) {
            return false;
        }

        if (stockPeriodStart == null || stockPeriodEnd == null) {
            throw new ValidationException("Stock period is invalid for this company (some null values)");
        }

        return !dateField.before(stockPeriodStart) && !dateField.after(stockPeriodEnd);
    }

    private void loadFinancialPeriodFromCompanyMaster(Long companyPoid) {
        String sql = "SELECT FINANCIAL_PERIOD_START, FINANCIAL_PERION_END FROM GLOBAL_COMPANY_MASTER WHERE COMPANY_POID = ?";
        
        jdbcTemplate.query(sql, ps -> ps.setLong(1, companyPoid), rs -> {
            if (rs.next()) {
                financialPeriodStart = rs.getDate("FINANCIAL_PERIOD_START");
                financialPeriodEnd = rs.getDate("FINANCIAL_PERION_END");
            }
            return null;
        });
    }

    public Boolean grantEditPermissionGetStatus(Long documentKeyPoid, String docId) {
        if (documentKeyPoid == null || docId == null) {
            return false;
        }

        String sql = "{CALL PROC_GLOBAL_DOC_EDIT_RIGHT_GET(?,?,?,?,?)}";

        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setLong(1, UserContext.getGroupPoid());
            stmt.setLong(2, UserContext.getUserPoid());
            stmt.setString(3, docId);
            stmt.setLong(4, documentKeyPoid);
            stmt.registerOutParameter(5, Types.VARCHAR);

            stmt.execute();

            String status = stmt.getString(5);
            log.debug("==> GrantEditPermissionGetStatus = {}", status);

            return status != null && status.contains("SUCCESS");

        } catch (SQLException e) {
            log.error("Error checking edit permission: {}", e.getMessage(), e);
            return false;
        }
    }

    public String fetchDocRef(String tableName, String poidColumnName, Long docKeyPoid) {

        try {

            if (StringUtils.isBlank(tableName)) {
                throw new IllegalArgumentException("Invalid table name");
            }
            if (StringUtils.isBlank(poidColumnName)) {
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
