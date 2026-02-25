package com.asg.common.lib.utility;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.dto.DiffObject;
import com.asg.common.lib.security.util.UserContext;
import org.apache.commons.lang3.ObjectUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

/**
 * Utility to generate audit diffs between two entity instances.
 *
 * - FAIL-CLOSED: Only fields annotated with @AuditField are logged
 * - Compares values semantically (not Java-type sensitive)
 * - Normalizes NEW value into OLD value's type
 * - Ensures consistent audit log representation
 *
 * Java Version: 21
 */
public final class DiffUtil {

    private DiffUtil() {
    }

    private static ZoneId getUserZoneId() {
        String timeZoneCode = UserContext.getTimeZoneCode();
        return ZoneId.of(timeZoneCode);
    }

    public static <T> List<DiffObject> createDiffList(
            T oldEntity,
            T newEntity,
            Class<T> entityClass
    ) {
        List<DiffObject> diffs = new ArrayList<>();

        if (newEntity == null) {
            return diffs;
        }

        for (Field field : entityClass.getDeclaredFields()) {
            field.setAccessible(true);

            String fieldName = field.getName();

            if (fieldName.equals("createdBy") || fieldName.equals("createdDate") || fieldName.equals("createdAt") ||
                    fieldName.equals("lastModifiedBy") || fieldName.equals("lastModifiedDate") ||
                    fieldName.equals("updatedBy") || fieldName.equals("updatedAt") ||
                    fieldName.equals("updatedDate")) {
                continue;
            }

            if (field.isAnnotationPresent(AuditIgnore.class)) {
                continue;
            }

            try {
                Object oldValue = oldEntity != null ? field.get(oldEntity) : null;
                Object newValue = field.get(newEntity);

                Object normalizedNewValue =
                        normalizeToOldType(oldValue, newValue);

                if (!areEqual(oldValue, normalizedNewValue)) {
                    diffs.add(new DiffObject(
                            field.getName(),
                            formatForLog(oldValue),
                            formatForLog(normalizedNewValue)
                    ));
                }

            } catch (IllegalAccessException ignored) {
                // intentionally ignored
            }
        }
        return diffs;
    }

    /**
     * Converts the new value into the old value's type
     * to keep audit logs type-consistent.
     */
    private static Object normalizeToOldType(Object oldValue, Object newValue) {
        if (newValue == null || oldValue == null) {
            return newValue;
        }

        return switch (oldValue) {

            case Timestamp ignored -> {
                Instant instant = extractInstant(newValue);
                yield instant != null ? Timestamp.from(instant) : newValue;
            }

            case Date ignored -> {
                Instant instant = extractInstant(newValue);
                yield instant != null ? Date.from(instant) : newValue;
            }

            case Instant ignored ->
                    extractInstant(newValue);

            case LocalDateTime ignored -> {
                Instant instant = extractInstant(newValue);
                yield instant != null
                        ? instant.atZone(getUserZoneId()).toLocalDateTime()
                        : newValue;
            }

            case LocalDate ignored -> {
                Instant instant = extractInstant(newValue);
                yield instant != null
                        ? instant.atZone(getUserZoneId()).toLocalDate()
                        : newValue;
            }

            case BigDecimal bdOld -> {
                if (newValue instanceof BigDecimal bdNew) {
                    yield bdNew.stripTrailingZeros();
                }
                yield newValue;
            }

            default -> newValue;
        };
    }

    /**
     * Extracts an Instant from any supported date/time type.
     */
    private static Instant extractInstant(Object value) {
        return switch (value) {
            case null -> null;
            case Timestamp ts -> ts.toInstant();
            case java.sql.Date sqlDate -> new Date(sqlDate.getTime()).toInstant();
            case Date d -> d.toInstant();
            case Instant i -> i;
            case LocalDateTime ldt ->
                    ldt.atZone(getUserZoneId()).toInstant();
            case LocalDate ld ->
                    ld.atStartOfDay(getUserZoneId()).toInstant();
            case OffsetDateTime odt -> odt.toInstant();
            case ZonedDateTime zdt -> zdt.toInstant();
            default -> null;
        };
    }

    /**
     * Ensures consistent and readable audit log values.
     */
    private static String formatForLog(Object value) {
        if (value == null) {
            return null;
        }

        return switch (value) {
            case Timestamp ts -> ts.toInstant().toString();
            case java.sql.Date sqlDate -> new Date(sqlDate.getTime()).toInstant().toString();
            case Date d -> d.toInstant().toString();
            case Instant i -> i.toString();
            case LocalDateTime ldt -> ldt.toString();
            case LocalDate ld -> ld.toString();
            case BigDecimal bd -> bd.stripTrailingZeros().toPlainString();
            case Double d -> BigDecimal.valueOf(d)
                    .setScale(3, RoundingMode.DOWN)
                    .toPlainString();
            default -> value.toString();
        };
    }

    private static boolean areEqual(Object oldValue, Object newValue) {
        if (!ObjectUtils.notEqual(oldValue, newValue)) {
            return true;
        }

        if (oldValue == null || newValue == null) {
            return false;
        }

        // byte[] content comparison
        if (oldValue instanceof byte[] o && newValue instanceof byte[] n) {
            return Arrays.equals(o, n);
        }

        // future-proof: other array types
        if (oldValue.getClass().isArray() && newValue.getClass().isArray()) {
            return Objects.deepEquals(oldValue, newValue);
        }

        return Objects.equals(oldValue, newValue);
    }
}
