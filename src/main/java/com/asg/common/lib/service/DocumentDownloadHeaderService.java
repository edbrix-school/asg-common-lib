package com.asg.common.lib.service;

import com.asg.common.lib.entity.DocumentEntity;
import com.asg.common.lib.repository.DocumentCommonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentDownloadHeaderService {

    private static final String DEFAULT_KEY_COLUMN = "TRANSACTION_POID";

    private final DocumentCommonRepository documentCommonRepository;
    private final JdbcTemplate jdbcTemplate;

    public HttpHeaders buildAttachmentHeaders(String docId, Long transactionPoid, String filePrefix, String extension) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(buildFileName(docId, transactionPoid, filePrefix, extension))
                .build());
        return headers;
    }

    public String buildFileName(String docId, Long transactionPoid, String filePrefix, String extension) {
        String token = resolveFileToken(docId, transactionPoid);
        String prefix = sanitizeToken(filePrefix);
        String ext = normalizeExtension(extension);

        if (StringUtils.isBlank(prefix)) {
            prefix = "report";
        }

        return prefix + "-" + token + ext;
    }

    private String resolveFileToken(String docId, Long transactionPoid) {
        String docRef = fetchDocRef(docId, transactionPoid);
        if (StringUtils.isNotBlank(docRef)) {
            return sanitizeToken(docRef);
        }
        return transactionPoid == null ? "unknown" : String.valueOf(transactionPoid);
    }

    private String fetchDocRef(String docId, Long transactionPoid) {
        if (StringUtils.isBlank(docId) || transactionPoid == null) {
            return null;
        }

        DocumentEntity document = documentCommonRepository.findByDocId(docId);
        if (document == null || StringUtils.isBlank(document.getMainTableName())) {
            log.debug("Unable to resolve docRef filename token because document metadata was missing for docId={}", docId);
            return null;
        }

        String keyColumn = StringUtils.defaultIfBlank(document.getDocKeyField(), DEFAULT_KEY_COLUMN);
        String sql = "SELECT DOC_REF FROM " + document.getMainTableName() + " WHERE " + keyColumn + " = ?";

        try {
            return jdbcTemplate.query(sql,
                    ps -> ps.setLong(1, transactionPoid),
                    rs -> rs.next() ? rs.getString("DOC_REF") : null);
        } catch (Exception ex) {
            log.debug("DOC_REF lookup failed for docId={}, table={}, keyColumn={}, transactionPoid={}",
                    docId, document.getMainTableName(), keyColumn, transactionPoid, ex);
            return null;
        }
    }

    private String sanitizeToken(String value) {
        if (StringUtils.isBlank(value)) {
            return "unknown";
        }
        return value.trim().replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    private String normalizeExtension(String extension) {
        if (StringUtils.isBlank(extension)) {
            return ".bin";
        }
        String normalized = extension.trim();
        return normalized.startsWith(".") ? normalized : "." + normalized;
    }
}
