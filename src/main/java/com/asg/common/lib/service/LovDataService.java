package com.asg.common.lib.service;

import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.entity.TimeZoneEntity;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.repository.TimeZoneDataRepository;
import com.asg.common.lib.security.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LovDataService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TimeZoneDataRepository timeZoneDataRepository;

    private static final List<String> SKIP_FILTER_LOV_NAMES = Arrays.asList(
            "TERMS_TEMPLATE_MASTER",
            "CHQ_RETURN_RECEIPT_NO",
            "BENEFICIARY_NAME",
            "OPS_PC_BERTH_LIST",
            "OPS_PC_CARGO",
            "CUSTOMER_SUPPLIER_MASTER",
            "OPS_PC_PDA_REF",
            "OPS_PC_FDA_REF",
            "ADVANCE_PETTY_CASH_PENDING_V2",
            "LINE_PIC_USER",
            "IMPORT_RECEIPT_CUSTOMER_PRINT",
            "FF_INV_CUST_SUP_MASTER",
            "EMP_REJOIN_LEAVE_REQUEST",
            "PJ_CN_REF_NO",
            "TRACKING_PORT_EVENT"
    );

    private static final List<String> ENABLE_PAGINATION = Arrays.asList(
            "CUSTOMER_SUPPLIER_MASTER",
            "OPS_PC_PDA_REF",
            "ADVANCE_PETTY_CASH_PENDING_V2",
            "LINE_PIC_USER",
            "IMPORT_RECEIPT_CUSTOMER_PRINT",
            "FF_INV_CUST_SUP_MASTER",
            "PJ_CN_REF_NO",
            "TRACKING_PORT_EVENT"
    );


    // ================================
    // Generic LOV method
    // ================================
    public Map<String, Object> getLovList(
            String filter, Long groupPoid, Long companyPoid, Long userPoid,
            String lovName, int pageNumber, int pageSize,
            String sortBy, String sortDir) {
        return getLovList(filter, groupPoid, companyPoid, userPoid, lovName, pageNumber, pageSize, sortBy, sortDir, null, null);
    }

    // ================================
    // Generic LOV method with default parameters
    // ================================
    public Map<String, Object> getLovList(
            String filter, Long groupPoid, Long companyPoid, Long userPoid,
            String lovName, int pageNumber, int pageSize,
            String sortBy, String sortDir, List<String> defaultCode, List<Long> defaultPoid) {
        return getLovList(filter, groupPoid, companyPoid, userPoid, lovName, pageNumber, pageSize, sortBy, sortDir, defaultCode, defaultPoid, null);
    }

    // ================================
    // Generic LOV method with default parameters + optional filter field (backward compatible)
    // ================================
    public Map<String, Object> getLovList(
            String filter, Long groupPoid, Long companyPoid, Long userPoid,
            String lovName, int pageNumber, int pageSize,
            String sortBy, String sortDir, List<String> defaultCode, List<Long> defaultPoid,
            String filterField) {

//        lovName = 'CHQ_RETURN_RECEIPT_NO'
//        url decode the filter
        String processedFilter = filter;

        if ("CHQ_RETURN_RECEIPT_NO".equalsIgnoreCase(lovName)
                && filter != null
                && !filter.trim().isEmpty()) {
            try {
                processedFilter = java.net.URLDecoder.decode(
                        filter,
                        java.nio.charset.StandardCharsets.UTF_8
                );
            } catch (Exception e) {
                log.warn("Failed to URL decode filter", e);
            }
        }

        if ("EMP_REJOIN_LEAVE_REQUEST".equalsIgnoreCase(lovName) && StringUtils.isNotEmpty(processedFilter)
                && !StringUtils.isNumeric(processedFilter.trim())) {
            processedFilter = "";
        }



        final String finalFilter = processedFilter;

        return jdbcTemplate.execute(
                (ConnectionCallback<Map<String, Object>>) con -> {
                    // PROC_LOV_GETLIST is a Postgres FUNCTION returning TABLE(poid, code, description)
                    // — not a procedure with an OUT refcursor, despite the "PROC_" name — so it's called
                    // as a plain SELECT, not CALL. Its 2nd parameter (company) is numeric[], not a
                    // scalar; a single companyPoid is wrapped as a one-element array to preserve
                    // today's single-company filtering behavior.
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT * FROM PROC_LOV_GETLIST(?, ?, ?, ?, ?, ?)")) {
                        ps.setLong(1, groupPoid != null ? groupPoid : 1L);
                        ps.setArray(2, con.createArrayOf("numeric",
                                new Object[]{companyPoid != null ? companyPoid : 1L}));
                        ps.setLong(3, userPoid != null ? userPoid : 0L);
                        ps.setString(4, lovName != null ? lovName : "");
                        // P_LOV_FILTER_FIELD
                        ps.setString(5, filterField != null ? filterField : "");
                        ps.setString(6, finalFilter != null ? finalFilter : "");
                    try (ResultSet rs = ps.executeQuery()) {
                        List<LovGetListDto> result = new ArrayList<>();
                        boolean includeUsers = "USER_ROLES".equalsIgnoreCase(lovName);
                        while (rs.next()) {
                            LovGetListDto dto = new LovGetListDto();
                            dto.setPoid(rs.getLong("POID"));
                            dto.setCode(rs.getString("CODE"));
                            dto.setDescription(rs.getString("DESCRIPTION"));
                            String description = rs.getString("DESCRIPTION");

                            if ("PREFERRED_COMMUNICATION".equalsIgnoreCase(lovName) ||
                                    "ADDRESS_PARTY_TYPE".equalsIgnoreCase(lovName)) {
                                dto.setLabel((description == null || description.trim().isEmpty())
                                        ? dto.getCode()
                                        : description);
                            } else {
                                dto.setLabel(description);
                            }

                            dto.setValue(rs.getLong("POID"));
                            try {
                                dto.setSeqNo(rs.getInt("SEQNO"));
                            } catch (SQLException ignored) {
                                dto.setSeqNo(0);
                            }
                            if (includeUsers) {
                                dto.setUsers(rs.getString("USERS"));
                            }
                            if ("GL_MASTER_LEDGERS_JV".equalsIgnoreCase(lovName)) {
                                try {
                                    dto.setControlAcNature(rs.getString("CONTROL_AC_NATURE") != null ? rs.getString("CONTROL_AC_NATURE") : "");
                                } catch (SQLException ignored) {}
                            }
                            result.add(dto);
                        }

                        // Find default values
                        List<LovGetListDto> defaultValues = new ArrayList<>();
                        if (defaultPoid != null && !defaultPoid.isEmpty()) {
                            defaultValues = result.stream()
                                    .filter(dto -> defaultPoid.contains(dto.getPoid()))
                                    .collect(Collectors.toList());
                        } else if (defaultCode != null && !defaultCode.isEmpty()) {
                            defaultValues = result.stream()
                                    .filter(dto -> dto.getCode() != null && defaultCode.stream().anyMatch(code -> code.equalsIgnoreCase(dto.getCode())))
                                    .collect(Collectors.toList());
                        }

                        boolean needsFallback = false;

                        if (defaultPoid != null && !defaultPoid.isEmpty()) {

                            Set<Long> foundPoids = defaultValues.stream()
                                    .map(LovGetListDto::getPoid)
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toSet());

                            if (!foundPoids.containsAll(defaultPoid)) {
                                needsFallback = true;
                            }
                        }

                        if (defaultCode != null && !defaultCode.isEmpty()) {

                            Set<String> foundCodes = defaultValues.stream()
                                    .map(LovGetListDto::getCode)
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toSet());

                            if (!foundCodes.containsAll(defaultCode)) {
                                needsFallback = true;
                            }
                        }

                        if (needsFallback) {

                            log.info("Some default values missing. Calling PROC_LOV_GET_FULL_LIST for lovName: {}", lovName);

                            // PROC_LOV_GET_FULL_LIST does not exist anywhere in the Postgres catalog
                            // (confirmed, not just unmigrated under a different signature) — this is a
                            // genuine gap, not a call-syntax bug like PROC_LOV_GETLIST's was. Degrade
                            // gracefully rather than fail the whole LOV lookup: keep whatever default
                            // values were already found on the first page, and skip the rest.
                            try (PreparedStatement fullPs = con.prepareStatement(
                                    "SELECT * FROM PROC_LOV_GET_FULL_LIST(?, ?, ?, ?, ?, ?)")) {

                                fullPs.setLong(1, groupPoid != null ? groupPoid : 1L);
                                fullPs.setArray(2, con.createArrayOf("numeric",
                                        new Object[]{companyPoid != null ? companyPoid : 1L}));
                                fullPs.setLong(3, userPoid != null ? userPoid : 0L);
                                fullPs.setString(4, lovName != null ? lovName : "");
                                fullPs.setString(5, filterField != null ? filterField : "");
                                fullPs.setString(6, ""); // no filter

                                try (ResultSet fullRs = fullPs.executeQuery()) {
                                    while (fullRs.next()) {

                                        Long poid = fullRs.getLong("POID");
                                        String code = fullRs.getString("CODE");

                                        boolean matchPoid = defaultPoid != null && defaultPoid.contains(poid);
                                        boolean matchCode = defaultCode != null && code != null && defaultCode.contains(code);

                                        if (matchPoid || matchCode) {

                                            boolean alreadyExists = defaultValues.stream()
                                                    .anyMatch(d -> Objects.equals(d.getPoid(), poid));

                                            if (!alreadyExists) {

                                                LovGetListDto dto = new LovGetListDto();
                                                dto.setPoid(poid);
                                                dto.setCode(code);
                                                dto.setDescription(fullRs.getString("DESCRIPTION"));
                                                dto.setLabel(fullRs.getString("DESCRIPTION"));
                                                dto.setValue(poid);

                                                try {
                                                    dto.setSeqNo(fullRs.getInt("SEQNO"));
                                                } catch (SQLException ignored) {
                                                    dto.setSeqNo(0);
                                                }
                                                if ("GL_MASTER_LEDGERS_JV".equalsIgnoreCase(lovName)) {
                                                    try {
                                                        dto.setControlAcNature(fullRs.getString("CONTROL_AC_NATURE"));
                                                    } catch (SQLException ignored) {}
                                                }

                                                defaultValues.add(dto);
                                            }
                                        }
                                    }
                                }
                            } catch (SQLException missingProc) {
                                log.warn("PROC_LOV_GET_FULL_LIST is not available on Postgres yet for lovName: {} — returning default values found on the first page only.", lovName);
                            }
                        }

                        // ✅ Apply client-side filtering after reading all rows
                        if (filter != null && !filter.trim().isEmpty() && !SKIP_FILTER_LOV_NAMES.contains(lovName)) {
                            String filterLower = filter.trim().toLowerCase();
                            result = result.stream()

                                    .filter(dto ->
                                            (dto.getValue() != null && dto.getPoid().toString().toLowerCase().contains(filterLower)) ||
                                                    (dto.getCode() != null && dto.getCode().toLowerCase().contains(filterLower)) ||
                                                    (dto.getDescription() != null && dto.getDescription().toLowerCase().contains(filterLower)) ||
                                                    (dto.getLabel() != null && dto.getLabel().toLowerCase().contains(filterLower)) ||
                                                    (dto.getUsers() != null && dto.getUsers().toLowerCase().contains(filterLower)))
                                    .collect(Collectors.toList());
                        }
                        // Sorting
                        String safeSortDir = (sortDir != null &&
                                (sortDir.equalsIgnoreCase("asc") || sortDir.equalsIgnoreCase("desc")))
                                ? sortDir.toLowerCase()
                                : "asc";

                        Comparator<LovGetListDto> comparator = switch (sortBy != null ? sortBy.toLowerCase() : "") {
                            case "code" ->
                                    Comparator.comparing(LovGetListDto::getCode, Comparator.nullsLast(String::compareToIgnoreCase));
                            case "description" ->
                                    Comparator.comparing(LovGetListDto::getDescription, Comparator.nullsLast(String::compareToIgnoreCase));
                            case "label" ->
                                    Comparator.comparing(LovGetListDto::getLabel, Comparator.nullsLast(String::compareToIgnoreCase));
                            case "value" ->
                                    Comparator.comparing(LovGetListDto::getValue, Comparator.nullsLast(Long::compare));
                            case "seqno" ->
                                    Comparator.comparing(LovGetListDto::getSeqNo, Comparator.nullsLast(Integer::compare));
                            case "poid" ->
                                    Comparator.comparing(LovGetListDto::getPoid, Comparator.nullsLast(Long::compare));
                            default ->
                                    Comparator.comparing(LovGetListDto::getSeqNo, Comparator.nullsLast(Integer::compare));
                        };

                        if ("desc".equalsIgnoreCase(safeSortDir)) comparator = comparator.reversed();
                        result.sort(comparator);


                        List<LovGetListDto> paginatedList;
                        boolean paginationEnabled = ENABLE_PAGINATION.stream().anyMatch(name -> name.equalsIgnoreCase(lovName));
                        if (pageSize <= 0 || ( SKIP_FILTER_LOV_NAMES.contains(lovName) && !paginationEnabled )) {
                            paginatedList = result; // return all
                        } else {
                            int fromIndex = Math.max(pageNumber * pageSize, 0);
                            int toIndex = Math.min(fromIndex + pageSize, result.size());
                            paginatedList = (fromIndex < result.size()) ? result.subList(fromIndex, toIndex) : new ArrayList<>();
                        }

                        Map<String, Object> response = new HashMap<>();
                        response.put("totalRecords", result.size());
                        response.put("data", paginatedList);
                        response.put("defaultValues", defaultValues);
                        return response;
                    }
                    }
                }
        );
    }

    public Map<String, Object> getBankMasterLov(
            String filter, Long groupPoid, Long companyPoid, Long userPoid,
            int pageNumber, int pageSize, String sortBy, String sortDir) {
        return getBankMasterLov(filter, groupPoid, companyPoid, userPoid, pageNumber, pageSize, sortBy, sortDir, null);
    }

    // ================================
    // Bank Master LOV with filter field (backward compatible)
    // ================================
    public Map<String, Object> getBankMasterLov(
            String filter, Long groupPoid, Long companyPoid, Long userPoid,
            int pageNumber, int pageSize, String sortBy, String sortDir,
            String filterField) {

        try {

            return jdbcTemplate.execute(
                    (ConnectionCallback<Map<String, Object>>) con -> {
                        // See getLovList() above: PROC_LOV_GETLIST is a table-returning FUNCTION,
                        // called via SELECT, with the company parameter as numeric[].
                        List<LovGetListDto> result = new ArrayList<>();
                        try (PreparedStatement ps = con.prepareStatement(
                                "SELECT * FROM PROC_LOV_GETLIST(?, ?, ?, ?, ?, ?)")) {

                            // NUMERIC arguments
                            ps.setLong(1, groupPoid != null ? groupPoid : 1L);       // NUMBER
                            ps.setArray(2, con.createArrayOf("numeric",
                                    new Object[]{companyPoid != null ? companyPoid : 1L}));   // NUMBER[]
                            ps.setLong(3, userPoid != null ? userPoid : 0L);         // NUMBER

                            // STRING arguments
                            ps.setString(4, "BANK_MASTER");                           // VARCHAR2
                            ps.setString(5, filterField != null ? filterField : "");   // VARCHAR2 - P_LOV_FILTER_FIELD
                            ps.setString(6, filter != null ? filter : "");            // VARCHAR2

                            try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                    LovGetListDto dto = new LovGetListDto();
                                    dto.setPoid(rs.getLong("POID"));
                                    dto.setCode(rs.getString("CODE"));
                                    dto.setDescription(rs.getString("DESCRIPTION"));
                                    dto.setLabel(rs.getString("DESCRIPTION"));
                                    dto.setValue(rs.getLong("POID"));
                                    dto.setSeqNo(0);
                                    result.add(dto);
                                }
                            }
                        }

                        Map<String, Object> response = new HashMap<>();
                        response.put("totalRecords", result.size());
                        response.put("data", result);
                        return response;
                    }
            );
        } catch (Exception e) {
            throw new RuntimeException("Error fetching Bank Master LOV: " + e.getMessage(), e);
        }
    }

    // ================================
    // TIMEZONE LOV
    // ================================
    public Map<String, Object> getTimezoneList(
            String filter, int pageNumber, int pageSize,
            String sortBy, String sortDir) {

        List<TimeZoneEntity> allTimezones = timeZoneDataRepository.findAll();

        List<TimeZoneEntity> activeTimezones = allTimezones.stream()
                .filter(tz -> "Y".equals(tz.getStatus()))
                .collect(Collectors.toList());

        if (filter != null && !filter.trim().isEmpty()) {
            String filterLower = filter.toLowerCase();
            activeTimezones = activeTimezones.stream()
                    .filter(tz ->
                            (tz.getTimezoneCode() != null && tz.getTimezoneCode().toLowerCase().contains(filterLower)) ||
                                    (tz.getTimezoneName() != null && tz.getTimezoneName().toLowerCase().contains(filterLower)) ||
                                    (tz.getTimezoneId() != null && tz.getTimezoneId().toString().contains(filter))
                    )
                    .collect(Collectors.toList());
        }

        List<LovGetListDto> result = activeTimezones.stream()
                .map(tz -> {
                    LovGetListDto dto = new LovGetListDto();
                    dto.setPoid(tz.getTimezoneId());
                    dto.setCode(tz.getTimezoneCode());
                    dto.setDescription(tz.getTimezoneName());
                    dto.setLabel(tz.getTimezoneName());
                    dto.setValue(tz.getTimezoneId());
                    dto.setSeqNo(tz.getTimezoneId().intValue());
                    return dto;
                })
                .collect(Collectors.toList());

        // Sorting
        String safeSortDir = (sortDir != null &&
                (sortDir.equalsIgnoreCase("asc") || sortDir.equalsIgnoreCase("desc")))
                ? sortDir.toLowerCase()
                : "asc";

        Comparator<LovGetListDto> comparator = switch (sortBy != null ? sortBy.toLowerCase() : "") {
            case "code" ->
                    Comparator.comparing(LovGetListDto::getCode, Comparator.nullsLast(String::compareToIgnoreCase));
            case "description" ->
                    Comparator.comparing(LovGetListDto::getDescription, Comparator.nullsLast(String::compareToIgnoreCase));
            case "label" ->
                    Comparator.comparing(LovGetListDto::getLabel, Comparator.nullsLast(String::compareToIgnoreCase));
            case "value" -> Comparator.comparing(LovGetListDto::getValue, Comparator.nullsLast(Long::compare));
            case "seqno" -> Comparator.comparing(LovGetListDto::getSeqNo, Comparator.nullsLast(Integer::compare));
            case "poid" -> Comparator.comparing(LovGetListDto::getPoid, Comparator.nullsLast(Long::compare));
            default -> Comparator.comparing(LovGetListDto::getCode, Comparator.nullsLast(String::compareToIgnoreCase));
        };

        if ("desc".equalsIgnoreCase(safeSortDir)) comparator = comparator.reversed();
        result.sort(comparator);

        int fromIndex = Math.max(pageNumber * pageSize, 0);
        int toIndex = Math.min(fromIndex + pageSize, result.size());
        List<LovGetListDto> paginatedList =
                (fromIndex < result.size()) ? result.subList(fromIndex, toIndex) : new ArrayList<>();

        Map<String, Object> response = new HashMap<>();
        response.put("totalRecords", result.size());
        response.put("data", paginatedList);
        return response;
    }

    // ================================
    // AGEING MASTER LOV
    // ================================
    public List<LovGetListDto> getAgeingBreakupTypes(Long groupPoid, Long companyPoid, Long userPoid) {
        return jdbcTemplate.execute((Connection con) -> {
            // See getLovList() above: PROC_LOV_GETLIST is a table-returning FUNCTION, called via
            // SELECT, with the company parameter as numeric[].
            List<LovGetListDto> result = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT * FROM PROC_LOV_GETLIST(?, ?, ?, ?, ?, ?)")) {
                ps.setObject(1, groupPoid, Types.NUMERIC);
                if (companyPoid != null) {
                    ps.setArray(2, con.createArrayOf("numeric", new Object[]{companyPoid}));
                } else {
                    ps.setNull(2, Types.ARRAY);
                }
                ps.setObject(3, userPoid, Types.NUMERIC);
                ps.setString(4, "GL_AGEING_TYPES");
                ps.setString(5, null);
                ps.setString(6, null);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LovGetListDto dto = new LovGetListDto();
                        dto.setPoid(rs.getLong("POID"));
                        dto.setCode(rs.getString("CODE"));

                        String description = rs.getString("DESCRIPTION");
                        dto.setLabel(description != null ? description : rs.getString("CODE"));
                        dto.setValue(0L);
                        dto.setDescription(description);
                        dto.setSeqNo(0);
                        result.add(dto);
                    }
                }
            }
            return result;
        });
    }

    public LovGetListDto getDetailsByPoidAndLovName(Long poid, String lovName) {
        log.info("poid : {}, lovName : {}", poid, lovName);
        if (poid == null || lovName == null || lovName.isEmpty())
            return new LovGetListDto();

        LovGetListDto dto = new LovGetListDto();
        Map<String, Object> listValue = this.getLovList("", UserContext.getGroupPoid(), UserContext.getCompanyPoid(), UserContext.getUserPoid(),
                lovName,
                0, 0,
                "", "", null, List.of(poid)
        );
        if (listValue != null) {
            @SuppressWarnings("unchecked")
            List<LovGetListDto> lovGetListDtos = (List<LovGetListDto>) listValue.get("data");
            if (lovGetListDtos != null) {
                dto = lovGetListDtos.stream()
                        .filter(x -> x.getPoid().equals(poid))
                        .findAny()
                        .orElse(null);
            }
            
            if (dto == null) {
                @SuppressWarnings("unchecked")
                List<LovGetListDto> defaultValues = (List<LovGetListDto>) listValue.get("defaultValues");
                if (defaultValues != null) {
                    dto = defaultValues.stream()
                            .filter(x -> x.getPoid().equals(poid))
                            .findAny()
                            .orElse(new LovGetListDto(poid, null, null, null, null, null, null,null));
                } else {
                    dto = new LovGetListDto(poid, null, null, null, null, null, null,null);
                }
            }
        }
        return dto;
    }

    public LovGetListDto getDetailsByCodeAndLovName(String code, String lovName) {

        log.info("code : {}, lovName : {}", code, lovName);
        if (code == null || code.isEmpty() || lovName == null || lovName.isEmpty())
            return new LovGetListDto();

        LovGetListDto dto = new LovGetListDto();
        Map<String, Object> listValue = this.getLovList("", UserContext.getGroupPoid(), UserContext.getCompanyPoid(), UserContext.getUserPoid(),
                lovName,
                0, 0,
                "", "",
                List.of(code), null
        );
        if (listValue != null) {
            @SuppressWarnings("unchecked")
            List<LovGetListDto> lovGetListDtos = (List<LovGetListDto>) listValue.get("data");

            if (lovGetListDtos != null) {
                dto = lovGetListDtos.stream()
                        .filter(x -> x.getCode().equalsIgnoreCase(code))
                        .findAny()
                        .orElse(null);
            }
            
            if (dto == null) {
                @SuppressWarnings("unchecked")
                List<LovGetListDto> defaultValues = (List<LovGetListDto>) listValue.get("defaultValues");
                if (defaultValues != null) {
                    dto = defaultValues.stream()
                            .filter(x -> x.getCode().equalsIgnoreCase(code))
                            .findAny()
                            .orElse(new LovGetListDto(null, code, null, null, null, null, null,null));
                } else {
                    dto = new LovGetListDto(null, code, null, null, null, null, null,null);
                }
            }
        }
        return dto;
    }

    @SuppressWarnings("unchecked")
    public Map<Long, LovGetListDto> getDetailsByPoidsAndLovName(List<Long> poids, String lovName) {
        if (poids == null || lovName == null || lovName.isEmpty()) return Collections.emptyMap();
        List<Long> distinctPoids = poids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (distinctPoids.isEmpty()) return Collections.emptyMap();

        Map<String, Object> listValue = this.getLovList(
                "", UserContext.getGroupPoid(), UserContext.getCompanyPoid(), UserContext.getUserPoid(),
                lovName, 0, 0, "", "", null, distinctPoids);

        Map<Long, LovGetListDto> result = new HashMap<>();
        if (listValue != null) {
            List<LovGetListDto> data = (List<LovGetListDto>) listValue.get("data");
            if (data != null) data.stream().filter(d -> d.getPoid() != null).forEach(d -> result.put(d.getPoid(), d));
            List<LovGetListDto> defaultValues = (List<LovGetListDto>) listValue.get("defaultValues");
            if (defaultValues != null) defaultValues.stream().filter(d -> d.getPoid() != null).forEach(d -> result.putIfAbsent(d.getPoid(), d));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<String, LovGetListDto> getDetailsByCodesAndLovName(List<String> codes, String lovName) {
        if (codes == null || lovName == null || lovName.isEmpty()) return Collections.emptyMap();
        List<String> distinctCodes = codes.stream()
                .filter(c -> c != null && !c.isEmpty()).distinct().collect(Collectors.toList());
        if (distinctCodes.isEmpty()) return Collections.emptyMap();

        Map<String, Object> listValue = this.getLovList(
                "", UserContext.getGroupPoid(), UserContext.getCompanyPoid(), UserContext.getUserPoid(),
                lovName, 0, 0, "", "", distinctCodes, null);

        Map<String, LovGetListDto> result = new HashMap<>();
        if (listValue != null) {
            List<LovGetListDto> data = (List<LovGetListDto>) listValue.get("data");
            if (data != null) data.stream().filter(d -> d.getCode() != null).forEach(d -> result.put(d.getCode(), d));
            List<LovGetListDto> defaultValues = (List<LovGetListDto>) listValue.get("defaultValues");
            if (defaultValues != null) defaultValues.stream().filter(d -> d.getCode() != null).forEach(d -> result.putIfAbsent(d.getCode(), d));
        }
        return result;
    }

    public LovGetListDto getLovItemByCodeFast(String code, String lovName) {
        log.info("Fetching LOV item (fast) - code: {}, lovName: {}, groupPoid: {}, companyPoid: {}, userPoid: {}",
                code, lovName, UserContext.getGroupPoid(), UserContext.getCompanyPoid(), UserContext.getUserPoid());

        if (code == null || code.isEmpty() || lovName == null || lovName.isEmpty()) {
            return new LovGetListDto();
        }

        Map<String, Object> listValue = this.getLovList(code, UserContext.getGroupPoid(), UserContext.getCompanyPoid(), UserContext.getUserPoid(),
                lovName, 0, 0, "", "", List.of(code), null);

        if (listValue != null) {
            @SuppressWarnings("unchecked")
            List<LovGetListDto> lovGetListDtos = (List<LovGetListDto>) listValue.get("data");

            if (lovGetListDtos != null) {
                LovGetListDto dto = lovGetListDtos.stream()
                        .filter(x -> x.getCode().equals(code))
                        .findFirst()
                        .orElse(null);
                
                if (dto != null) {
                    return dto;
                }
            }
            
            @SuppressWarnings("unchecked")
            List<LovGetListDto> defaultValues = (List<LovGetListDto>) listValue.get("defaultValues");
            if (defaultValues != null) {
                return defaultValues.stream()
                        .filter(x -> x.getCode().equals(code))
                        .findFirst()
                        .orElse(new LovGetListDto());
            }
        }
        return new LovGetListDto();
    }

    public LovGetListDto getDetailsByPoidAndLovNameFast(Long poid, String lovName) {
        log.info("poid : {}, lovName : {}", poid, lovName);
        if (poid == null || lovName == null || lovName.isEmpty())
            return new LovGetListDto();

        LovGetListDto dto = null;
        Map<String, Object> listValue = this.getLovList(poid.toString(), UserContext.getGroupPoid(), UserContext.getCompanyPoid(), UserContext.getUserPoid(),
                lovName,
                0, 0,
                "", "", null, List.of(poid)
        );
        if (listValue != null) {
            @SuppressWarnings("unchecked")
            List<LovGetListDto> lovGetListDtos = (List<LovGetListDto>) listValue.get("data");
            if (lovGetListDtos != null && !lovGetListDtos.isEmpty()) {
                dto = lovGetListDtos.stream()
                        .filter(x -> x.getPoid().equals(poid))
                        .findAny()
                        .orElse(null);
            }
            
            // If not found in data, check defaultValues
            if (dto == null) {
                @SuppressWarnings("unchecked")
                List<LovGetListDto> defaultValues = (List<LovGetListDto>) listValue.get("defaultValues");
                if (defaultValues != null && !defaultValues.isEmpty()) {
                    dto = defaultValues.stream()
                            .filter(x -> x.getPoid().equals(poid))
                            .findAny()
                            .orElse(null);
                }
            }
        }
        return dto != null ? dto : new LovGetListDto(poid, null, null, null, null, null, null,null);
    }
}
