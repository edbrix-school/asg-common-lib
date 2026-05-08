package com.asg.common.lib.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class TableMetaRepository {

    @PersistenceContext
    private EntityManager em;

    /** Get columns from given SQL */
    public List<String> getColumnsFromSql(String sql) {
        return getColumnNames(sql, true);
    }

    /** Get columns from the main table */
    public List<String> getColumnsFromTable(String tableName) {
        return getColumnNames(tableName, false);
    }

    /** Get column names (from SQL or main table) */
    private List<String> getColumnNames(String source, boolean isSql) {
        return em.unwrap(Session.class).doReturningWork(conn -> {
            String sql = isSql
                    ? withRownumLimit(normalizeSql(source))
                    : withRownumLimit("SELECT * FROM " + source);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSetMetaData meta = ps.executeQuery().getMetaData();
                List<String> cols = new ArrayList<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    cols.add(meta.getColumnName(i).toUpperCase());
                }
                return cols;
            }
        });
    }

    /** Execute dynamic query with positional params */
    public List<Map<String, Object>> executeDynamicQuery(String sql, List<Object> params,List<String> cols) {
        Query q = em.createNativeQuery(sql);

        for (int i = 0; i < params.size(); i++) {
            q.setParameter(i + 1, params.get(i)); // positional binding starts at 1
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();

//        System.out.println("Query result size = " + rows.size());
//        rows.forEach(r -> System.out.println("Row = " + Arrays.toString(r)));

        if (rows.isEmpty()) return List.of();

        List<Map<String, Object>> result = new ArrayList<>();

        for (Object[] row : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (int i = 0; i < cols.size(); i++) {
                map.put(cols.get(i), row[i]);
            }
            result.add(map);
        }

//        System.out.println("Mapped result = " + result);
        return result;
    }

    // ----------------- Helpers -----------------

    /** Wrap SQL in outer select if not already wrapped */
    public String normalizeSql(String sql) {
        if (sql == null || sql.isBlank()) return sql;

        // Clean hidden characters and trim
        String cleaned = sql.replace("\r", " ").replaceAll("\\s+", " ").trim();

        // Wrap if not already wrapped
        String lower = cleaned.toLowerCase();
        if (lower.startsWith("select * from (")) return cleaned;

        return "SELECT * FROM (" + cleaned + ") tmp";
    }


    /** Add rownum filter to reduce data being fetched */
    public static String withRownumLimit(String sql) {
        return sql + " WHERE ROWNUM <= 1";
    }

    /** Execute a COUNT(*) query and return total record count */
    public Long executeCountQuery(String sql, List<Object> params) {
        Query q = em.createNativeQuery(sql);
        for (int i = 0; i < params.size(); i++) {
            q.setParameter(i + 1, params.get(i));
        }
        Object singleResult = q.getSingleResult();
        return ((Number) singleResult).longValue();
    }
}