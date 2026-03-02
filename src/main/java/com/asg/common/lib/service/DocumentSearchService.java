package com.asg.common.lib.service;

import com.asg.common.lib.dto.Clause;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.entity.DocumentEntity;
import com.asg.common.lib.repository.DocumentCommonRepository;
import com.asg.common.lib.repository.TableMetaRepository;
import com.asg.common.lib.security.util.UserContext;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


@CommonsLog
@Service
public class DocumentSearchService {

    @Autowired
    TableMetaRepository tableMetaRepository;

    @Autowired
    DocumentCommonRepository documentRepository;

    // Used by search method for filtering
    public List<String> getSearchableFieldNames(DocumentEntity doc) {
        String sql = doc.getListOfRecordsSql();

        return (sql != null && !sql.isBlank())
                ? tableMetaRepository.getColumnsFromSql(sql)
                : tableMetaRepository.getColumnsFromTable(doc.getMainTableName());
    }

    /**
     * Search with filters (globalsearch + field filters + pagination)
     */
    public RawSearchResult search(String docId, List<FilterDto> filters, String operator, Pageable pageable, String isDeleted,
                                  @Nullable String labelField,
                                  @Nullable String valueField) {
        DocumentEntity doc = getDocument(docId);

        Map<String, String> displayCols = getDisplayableFields(doc);

//        System.out.println("Displayable Columns for docId=" + docId);
//        displayCols.forEach((k, v) -> System.out.println("   " + k + " -> " + v));

        String rawBaseSql = !doc.getListOfRecordsSql().isBlank()
                ? cleanSql(doc.getListOfRecordsSql())
                : "SELECT * FROM " + doc.getMainTableName();

        String baseSql = "SELECT * FROM (" + rawBaseSql + ") BASE";

        List<String> columnNames = getSearchableFieldNames(doc);

        Clause clause = buildWhereClause(doc, columnNames, filters, operator, isDeleted);

        // Apply sorting and get the SQL with WHERE clause
        String sortedSql = applySorting(baseSql, pageable, columnNames, clause.sql());

        // Add pagination
        String finalSql = sortedSql + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        List<Object> params = new ArrayList<>(clause.params());
        params.add(pageable.getPageNumber() * pageable.getPageSize()); // offset
        params.add(pageable.getPageSize()); // limit

        List<Map<String, Object>> rows = tableMetaRepository.executeDynamicQuery(finalSql, params, columnNames);

        // Normalize date/timestamp values to ISO-8601 (UTC) so timezone is not shifted by JVM/serialization
        for (Map<String, Object> row : rows) {
            normalizeRowDateValues(row);
            for (String field : displayCols.keySet()) {
                row.putIfAbsent(field, null);
            }
            // Handle required fields for frontend label/value
            if (labelField != null) row.putIfAbsent("label", row.get(labelField));
            if (valueField != null) row.putIfAbsent("value", row.get(valueField));
        }

        //  return new RawSearchResult(rows, displayCols, rows.size());
        String countSql = "SELECT COUNT(*) FROM (" + baseSql + " " + clause.sql() + ") total_count";
        Long totalRecords = tableMetaRepository.executeCountQuery(countSql, clause.params());

        return new RawSearchResult(rows, displayCols, totalRecords);
    }

    // Helper to normalize SQL
    private String cleanSql(String sql) {
        // Remove carriage returns, multiple spaces, and trim
        return sql.replaceAll("\\r", "")
                .replaceAll("\\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Helper to apply sorting; respects Pageable first, otherwise uses SQL order, else unsorted
     */
    private String applySorting(String baseSql, Pageable pageable, List<String> columnNames, String whereClause) {

        // Strip ORDER BY from base SQL if Pageable has sorting (remove only inside the first parentheses)
        String sql = pageable.getSort().isSorted()
                ? baseSql.replaceAll("(?i)ORDER\\s+BY[\\s\\S]*?(?=\\))", "")  // remove ORDER BY until the next ')'
                : baseSql;

        StringBuilder sqlBuilder = new StringBuilder(sql).append(whereClause);

        // Apply dynamic sorting from Pageable
        if (pageable.getSort().isSorted()) {
            String orderBy = pageable.getSort().stream()
                    .filter(order -> columnNames.contains(order.getProperty().toUpperCase()))
                    .map(order -> order.getProperty() + " " + order.getDirection().name())
                    .collect(Collectors.joining(", "));
            if (!orderBy.isEmpty()) {
                sqlBuilder.append(" ORDER BY ").append(orderBy);
            }
        }

        return sqlBuilder.toString();
    }


    /**
     * Get displayable fields with type {colName -> type}
     */
    public Map<String, String> getDisplayableFields(DocumentEntity doc) {
        if (doc.getListOfDisplayColumnsAndTypes() != null && !doc.getListOfDisplayColumnsAndTypes().isBlank()) {
            return parseDisplayColumns(doc.getListOfDisplayColumnsAndTypes());
        }
        return null;
    }

    /**
     * Build WHERE clause with positional params
     */
    private Clause buildWhereClause(DocumentEntity doc, List<String> fields, List<FilterDto> filters, String operator, String isDeleted) {
        StringBuilder sql = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        // Filter to search only in deleted or active fields
        if ("Y".equalsIgnoreCase(isDeleted)) {
            // show only deleted
            sql.append(" AND DELETED = 'Y'");
        } else {
            // default or anything else → show active
            sql.append(" AND (DELETED IS NULL OR DELETED = 'N')");
        }

        // Add COMPANY_POID filter for Transactions document type
        if ("Transactions".equalsIgnoreCase(doc.getDocType()) && fields.contains("COMPANY_POID")) {
            Long companyPoid = UserContext.getCompanyPoid();
            if (companyPoid != null) {
                sql.append(" AND COMPANY_POID = ?");
                params.add(companyPoid);
            }
        }

        // variables to check for operator precedence case with OR/AND
        boolean hasOrGroup = "OR".equalsIgnoreCase(operator);
        boolean groupStarted = false;

        for (FilterDto f : Optional.ofNullable(filters).orElse(List.of())) {
            String field = f.searchField().toUpperCase();
            String rawValue = f.searchValue();
            if (rawValue == null) continue;

            if ("GLOBALSEARCH".equals(field)) {
                handleGlobalSearch(sql, fields, rawValue, params);
            }
            else if (fields.contains(field)) {
                String[] cmp = parseComparison(rawValue);
                String op = cmp[0], value = cmp[1];
                boolean isDateField = value.matches("\\d{4}-\\d{2}-\\d{2}");
                String fieldCondition = buildFieldCondition(field, op, value, isDateField, params);

                // close OR group before date filters
                if (isDateField && groupStarted) {
                    sql.append(")");
                    groupStarted = false;
                }

                // append condition with proper grouping
                if (isDateField) {
                    sql.append(" AND ").append(fieldCondition); //Handle case when date fields will always AND together not OR
                } else {
                    if (hasOrGroup && !groupStarted) {
                        sql.append(" AND (");
                        groupStarted = true;
                    } else {
                        sql.append(" ").append(operator).append(" ");
                    }
                    sql.append(fieldCondition);
                }
            }
        }

        // close open OR group as a failsafe
        if (groupStarted) sql.append(")");

        return new Clause(sql.toString(), params);
    }

    // ----------------- helpers -----------------

    /**
     * Replaces date/timestamp values in the row with ISO-8601 strings in UTC
     * so that JVM default timezone or JSON serialization does not shift the instant.
     */
    private void normalizeRowDateValues(Map<String, Object> row) {
        if (row == null) return;
        for (Map.Entry<String, Object> entry : new ArrayList<>(row.entrySet())) {
            Object value = entry.getValue();
            if (value == null) continue;
            Instant instant = null;
            if (value instanceof Timestamp ts) {
                instant = ts.toInstant();
            } else if (value instanceof java.sql.Date sqlDate) {
                instant = new java.util.Date(sqlDate.getTime()).toInstant();
            } else if (value instanceof java.util.Date date) {
                instant = date.toInstant();
            }
            if (instant != null) {
                row.put(entry.getKey(), instant.toString());
            }
        }
    }

    private DocumentEntity getDocument(String docId) {
        return documentRepository.findByDocId(docId);
    }

    // Sends fields to be displayed and types as key value pairs from db
    private Map<String, String> parseDisplayColumns(String config) {
        if (config == null || config.isBlank()) return Map.of();

        Map<String, String> map = new LinkedHashMap<>();
        for (String part : config.split("\\|")) {
            String trimmed = part.trim();
            if (!trimmed.startsWith("<") || !trimmed.endsWith(">") || !trimmed.contains(",")) {
                throw new IllegalArgumentException("Invalid display field format: " + part);
            }
            String[] kv = trimmed.substring(1, trimmed.length() - 1).split(",", 2);
            map.put(kv[0].toUpperCase().trim(), kv[1].trim());
        }
        return map;
    }

    public String resolveOperator(@Nullable FilterRequestDto request) {
        if (request == null || request.operator() == null || request.operator().isBlank()) {
            return "OR";
        }
        return request.operator().toUpperCase();
    }

    public String resolveIsDeleted(@Nullable FilterRequestDto request) {
        if (request == null || request.isDeleted() == null || request.isDeleted().isBlank()) {
            return "N";
        }
        return request.isDeleted().toUpperCase();
    }

    //Filters for Master Types
    public List<FilterDto> resolveFilters(@Nullable FilterRequestDto request) {
        return resolveDateFilters(request,null,null,null);
    }

    //Filters for Transaction Types
    //Add date filters if needed to filters for dynamic query building
    public List<FilterDto> resolveDateFilters(@Nullable FilterRequestDto request,
                                              @Nullable String dateField,
                                              @Nullable LocalDate startDateValue,
                                              @Nullable LocalDate endDateValue) {

        if(request != null) {
            List<FilterDto> filters = new ArrayList<>(request.filters());

            // Skip adding any date filters if global search is present
            boolean hasGlobalSearch = filters.stream()
                    .anyMatch(f -> "GLOBALSEARCH".equalsIgnoreCase(f.searchField()));

            // Only add date filters if date field and values are given and globalsearch is not given
            if (!hasGlobalSearch && dateField != null && startDateValue != null && endDateValue != null) {
                filters.add(new FilterDto(dateField, ">=" + startDateValue));
                filters.add(new FilterDto(dateField, "<=" + endDateValue));
            }
            return filters;

        }else{
            return Collections.emptyList();
        }
    }

    //Seperate out condition and value while keeping = for non date fields as identifier
    private String[] parseComparison(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty())
            return new String[]{"=", ""};

        String value = rawValue.trim();
        String op = "=";

        if (value.startsWith(">=")) { op = ">="; value = value.substring(2).trim(); }
        else if (value.startsWith("<=")) { op = "<="; value = value.substring(2).trim(); }
        else if (value.startsWith(">"))  { op = ">";  value = value.substring(1).trim(); }
        else if (value.startsWith("<"))  { op = "<";  value = value.substring(1).trim(); }

        return new String[]{op, value};
    }

    //Handle globalsearch conditions
    private void handleGlobalSearch(StringBuilder sql, List<String> fields, String value, List<Object> params) {
        List<String> orClauses = new ArrayList<>();

        for (String f : fields) {
            orClauses.add("UPPER(" + f + ") LIKE ?");
            params.add("%" + value.toUpperCase() + "%");
        }

        if (!orClauses.isEmpty()) {
            sql.append(" AND (").append(String.join(" OR ", orClauses)).append(")");
        }
    }

    //Handle non globalsearch conditions
    private String buildFieldCondition(String field, String op, String value, boolean isDateField, List<Object> params) {
        if (!"=".equals(op)) {
            if (isDateField) {
                return "TO_DATE(" + field + ") " + op + " DATE '" + value.trim() + "'";
            }
            return field + " " + op + " ?";
        }
        if (value.contains("|")) {
            String[] vals = value.split("\\|");
            List<String> orClauses = new ArrayList<>();
            for (String v : vals) {
                orClauses.add("UPPER(" + field + ") LIKE ?");
                params.add("%" + v.toUpperCase() + "%");
            }
            return "(" + String.join(" OR ", orClauses) + ")";
        }
        params.add("%" + value.toUpperCase() + "%");
        return "UPPER(" + field + ") LIKE ?";
    }

}