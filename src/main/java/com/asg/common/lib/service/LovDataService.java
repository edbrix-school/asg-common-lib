package com.asg.common.lib.service;

import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.entity.TimeZoneEntity;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.repository.TimeZoneDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.dialect.OracleTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.CallableStatementCreator;
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

        return jdbcTemplate.execute(
                (CallableStatementCreator) con -> {
                    CallableStatement cs = con.prepareCall("{call PROC_LOV_GETLIST(?, ?, ?, ?, ?, ?, ?)}");
                    cs.setLong(1, groupPoid != null ? groupPoid : 1L);
                    cs.setLong(2, companyPoid != null ? companyPoid : 1L);
                    cs.setLong(3, userPoid != null ? userPoid : 0L);
                    cs.setString(4, lovName != null ? lovName : "");
                    cs.setString(5, "");
                    cs.setString(6, filter != null ? filter : "");
                    cs.registerOutParameter(7, OracleTypes.CURSOR);
                    return cs;
                },
                (CallableStatementCallback<Map<String, Object>>) cs -> {
                    cs.execute();
                    ResultSet rs = (ResultSet) cs.getObject(7);
                    if (rs == null) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("totalRecords", 0);
                        response.put("data", Collections.emptyList());
                        response.put("warning", "No data returned for lovName: " + lovName);
                        return response;
                    }
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
                        result.add(dto);
                    }
                    rs.close();

                    // Find default values
                    List<LovGetListDto> defaultValues = new ArrayList<>();
                    if (defaultPoid != null && !defaultPoid.isEmpty()) {
                        defaultValues = result.stream()
                                .filter(dto -> defaultPoid.contains(dto.getPoid()))
                                .collect(Collectors.toList());
                    } else if (defaultCode != null && !defaultCode.isEmpty()) {
                        defaultValues = result.stream()
                                .filter(dto -> defaultCode.contains(dto.getCode()))
                                .collect(Collectors.toList());
                    }

                    // ✅ Apply client-side filtering after reading all rows
                    if (filter != null && !filter.trim().isEmpty()) {
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
                    if (pageSize <= 0) {
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
        );
    }

    public Map<String, Object> getBankMasterLov(
            String filter, Long groupPoid, Long companyPoid, Long userPoid,
            int pageNumber, int pageSize, String sortBy, String sortDir) {

        try {

            return jdbcTemplate.execute(
                    (CallableStatementCreator) con -> {
                        CallableStatement cs = con.prepareCall("{call PROC_LOV_GETLIST(?, ?, ?, ?, ?, ?, ?)}");

                        // NUMERIC arguments
                        cs.setLong(1, groupPoid != null ? groupPoid : 1L);       // NUMBER
                        cs.setLong(2, companyPoid != null ? companyPoid : 1L);   // NUMBER
                        cs.setLong(3, userPoid != null ? userPoid : 0L);         // NUMBER

                        // STRING arguments
                        cs.setString(4, "BANK_MASTER");                           // VARCHAR2
                        cs.setString(5, "");                                      // VARCHAR2
                        cs.setString(6, filter != null ? filter : "");            // VARCHAR2

                        // OUTPUT CURSOR
                        cs.registerOutParameter(7, OracleTypes.CURSOR);
                        return cs;
                    },
                    (CallableStatementCallback<Map<String, Object>>) cs -> {
                        cs.execute();
                        ResultSet rs = (ResultSet) cs.getObject(7);
                        List<LovGetListDto> result = new ArrayList<>();
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
                        rs.close();

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
            CallableStatement cs = con.prepareCall("{call PROC_LOV_GETLIST(?, ?, ?, ?, ?, ?, ?)}");

            cs.setObject(1, groupPoid, Types.NUMERIC);
            cs.setObject(2, companyPoid, Types.NUMERIC);
            cs.setObject(3, userPoid, Types.NUMERIC);
            cs.setString(4, "GL_AGEING_TYPES");
            cs.setString(5, null);
            cs.setString(6, null);
            cs.registerOutParameter(7, OracleTypes.CURSOR);

            cs.execute();
            ResultSet rs = (ResultSet) cs.getObject(7);

            List<LovGetListDto> result = new ArrayList<>();
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
            return result;
        });
    }

    public LovGetListDto getDetailsByPoidAndLovName(Long poid, String lovName) {
        log.info("poid : {}, lovName : {}", poid, lovName);
        if (poid == null || lovName == null || lovName.isEmpty())
            return new LovGetListDto();

        LovGetListDto dto = new LovGetListDto();
        Map<String, Object> listValue = this.getLovList("", 0L, 0L, 0L,
                lovName,
                0, 0,
                "", ""
        );
        if (listValue != null) {
            @SuppressWarnings("unchecked")
            List<LovGetListDto> lovGetListDtos = (List<LovGetListDto>) listValue.get("data");
            if (lovGetListDtos != null) {
                dto = lovGetListDtos.stream()
                        .filter(x -> x.getPoid().equals(poid))
                        .findAny()
                        .orElse(new LovGetListDto(poid, null, null, null, null, null, null));
            }
        }
        return dto;
    }

    public LovGetListDto getDetailsByCodeAndLovName(String code, String lovName) {

        log.info("code : {}, lovName : {}", code, lovName);
        if (code == null || code.isEmpty() || lovName == null || lovName.isEmpty())
            return new LovGetListDto();

        LovGetListDto dto = new LovGetListDto();
        Map<String, Object> listValue = this.getLovList("", 0L, 0L, 0L,
                lovName,
                0, 0,
                "", ""
        );
        if (listValue != null) {
            @SuppressWarnings("unchecked")
            List<LovGetListDto> lovGetListDtos = (List<LovGetListDto>) listValue.get("data");

            if (lovGetListDtos != null) {
                dto = lovGetListDtos.stream().filter(x -> x.getCode().equalsIgnoreCase(code)).findAny().orElseThrow(() -> new ResourceNotFoundException("Master Data", "CODE", code));
            }
        }
        return dto;
    }
}
