package com.asg.common.lib.service;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Builds the {@code Content-Disposition} header for document downloads, naming the file after the
 * business {@code DOC_REF} instead of the internal POID.
 *
 * <p>The owning table is supplied by the caller as its JPA entity class; the table name is read from
 * {@link Table}. This replaces the earlier lookup through {@code GLOBAL_DOC_MASTER.MAIN_TABLE_NAME},
 * which is unpopulated for most documents and so silently degraded every filename to the POID.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentDownloadHeaderService {

    private static final String DEFAULT_KEY_COLUMN = "TRANSACTION_POID";
    private static final String DOC_REF_COLUMN = "DOC_REF";
    private static final String UNKNOWN_TOKEN = "unknown";
    private static final String DEFAULT_PREFIX = "report";
    private static final String DEFAULT_EXTENSION = ".bin";

    /** Marks a class that carries no usable table mapping, so the miss is cached too. */
    private static final String NO_TABLE = "";

    private static final Pattern SQL_IDENTIFIER = Pattern.compile("^[A-Za-z_][A-Za-z0-9_$#]*$");
    private static final Pattern ILLEGAL_FILENAME_CHARS = Pattern.compile("[\\\\/:*?\"<>|\\s]+");

    private static final Map<Class<?>, String> TABLE_NAME_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Boolean> DOC_REF_CACHE = new ConcurrentHashMap<>();

    private final JdbcTemplate jdbcTemplate;

    /**
     * @param entityClass    JPA entity owning the {@code DOC_REF} column, e.g. {@code GlJournalVoucherHdr.class}
     * @param transactionPoid value of the key column identifying the row
     * @param filePrefix     leading segment of the filename, e.g. {@code "journal-voucher"}
     * @param extension      file extension, with or without a leading dot
     */
    public HttpHeaders buildAttachmentHeaders(Class<?> entityClass, Long transactionPoid, String filePrefix,
                                              String extension) {
        return buildAttachmentHeaders(entityClass, DEFAULT_KEY_COLUMN, transactionPoid, filePrefix, extension);
    }

    /** Overload for entities keyed by something other than {@code TRANSACTION_POID}. */
    public HttpHeaders buildAttachmentHeaders(Class<?> entityClass, String keyColumn, Long transactionPoid,
                                              String filePrefix, String extension) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(buildFileName(entityClass, keyColumn, transactionPoid, filePrefix, extension))
                .build());
        return headers;
    }

    public String buildFileName(Class<?> entityClass, Long transactionPoid, String filePrefix, String extension) {
        return buildFileName(entityClass, DEFAULT_KEY_COLUMN, transactionPoid, filePrefix, extension);
    }

    public String buildFileName(Class<?> entityClass, String keyColumn, Long transactionPoid, String filePrefix,
                                String extension) {
        String token = resolveFileToken(entityClass, keyColumn, transactionPoid);
        String prefix = capitalizeSegments(
                sanitizeToken(StringUtils.defaultIfBlank(filePrefix, DEFAULT_PREFIX)));
        String ext = normalizeExtension(extension);

        return prefix + "-" + token + ext;
    }

    private String resolveFileToken(Class<?> entityClass, String keyColumn, Long transactionPoid) {
        String docRef = fetchDocRef(entityClass, keyColumn, transactionPoid);
        if (StringUtils.isNotBlank(docRef)) {
            return sanitizeToken(docRef);
        }
        return transactionPoid == null ? UNKNOWN_TOKEN : String.valueOf(transactionPoid);
    }

    private String fetchDocRef(Class<?> entityClass, String keyColumn, Long transactionPoid) {
        if (transactionPoid == null) {
            return null;
        }

        String tableName = resolveTableName(entityClass);
        if (StringUtils.isBlank(tableName)) {
            return null;
        }

        // Some documents (employee master, salary master, user roles, customer master) have no
        // DOC_REF column, so skip the round trip instead of issuing a query that always errors.
        if (!hasDocRefColumn(entityClass)) {
            return null;
        }

        String column = StringUtils.defaultIfBlank(keyColumn, DEFAULT_KEY_COLUMN);
        if (!isValidIdentifier(column)) {
            log.warn("Rejected DOC_REF lookup for table={} because key column {} is not a valid identifier",
                    tableName, column);
            return null;
        }

        String sql = "SELECT DOC_REF FROM " + tableName + " WHERE " + column + " = ?";

        try {
            return jdbcTemplate.query(sql,
                    ps -> ps.setLong(1, transactionPoid),
                    rs -> rs.next() ? rs.getString("DOC_REF") : null);
        } catch (Exception ex) {
            log.debug("DOC_REF lookup failed for table={}, keyColumn={}, transactionPoid={}",
                    tableName, column, transactionPoid, ex);
            return null;
        }
    }

    /** Reads {@code @Table(name=...)} off the entity, falling back to {@code @Entity(name=...)}. */
    private String resolveTableName(Class<?> entityClass) {
        if (entityClass == null) {
            return null;
        }

        String tableName = TABLE_NAME_CACHE.computeIfAbsent(entityClass, cls -> {
            Table table = cls.getAnnotation(Table.class);
            if (table != null && StringUtils.isNotBlank(table.name())) {
                return table.name();
            }
            Entity entity = cls.getAnnotation(Entity.class);
            if (entity != null && StringUtils.isNotBlank(entity.name())) {
                return entity.name();
            }
            log.warn("{} carries no @Table(name=...) mapping; download filenames will fall back to the POID",
                    cls.getName());
            return NO_TABLE;
        });

        if (NO_TABLE.equals(tableName)) {
            return null;
        }
        if (!isValidIdentifier(tableName)) {
            log.warn("Rejected DOC_REF lookup because {} maps to a non-identifier table name: {}",
                    entityClass.getName(), tableName);
            return null;
        }
        return tableName;
    }

    /** True when the entity (or a mapped superclass) maps a field to the {@code DOC_REF} column. */
    private boolean hasDocRefColumn(Class<?> entityClass) {
        return DOC_REF_CACHE.computeIfAbsent(entityClass, cls -> {
            for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field field : c.getDeclaredFields()) {
                    Column column = field.getAnnotation(Column.class);
                    if (column != null && DOC_REF_COLUMN.equalsIgnoreCase(column.name())) {
                        return true;
                    }
                    if (column == null && DOC_REF_COLUMN.equalsIgnoreCase(toColumnName(field.getName()))) {
                        return true;
                    }
                }
            }
            return false;
        });
    }

    /** Mirrors Hibernate's default camelCase -> SNAKE_CASE column naming. */
    private String toColumnName(String fieldName) {
        return fieldName.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase();
    }

    private boolean isValidIdentifier(String value) {
        return StringUtils.isNotBlank(value) && SQL_IDENTIFIER.matcher(value).matches();
    }

    /**
     * Upper-cases the first letter of every hyphen-separated segment, so callers passing
     * {@code "journal-voucher"} and {@code "Journal-voucher"} both yield {@code "Journal-Voucher"}.
     * Applied only to the caller-supplied prefix; the {@code DOC_REF} token is business data and is
     * emitted verbatim.
     */
    private String capitalizeSegments(String value) {
        String[] segments = value.split("-", -1);
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                sb.append('-');
            }
            String segment = segments[i];
            if (segment.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(segment.charAt(0))).append(segment.substring(1));
        }
        return sb.toString();
    }

    private String sanitizeToken(String value) {
        if (StringUtils.isBlank(value)) {
            return UNKNOWN_TOKEN;
        }
        return ILLEGAL_FILENAME_CHARS.matcher(value.trim()).replaceAll("_");
    }

    private String normalizeExtension(String extension) {
        if (StringUtils.isBlank(extension)) {
            return DEFAULT_EXTENSION;
        }
        String normalized = extension.trim();
        return normalized.startsWith(".") ? normalized : "." + normalized;
    }
}
