package com.asg.common.lib.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Types;

@Service
@Slf4j
@RequiredArgsConstructor
public class GlobalParameterService {

    private final JdbcTemplate jdbcTemplate;

    public String getParameterValue(String parameterName, String parameterKeyIdType, String parameterKeyId, String defaultValue) {
        try {
            String function = "{ ? = call RTN_GLOBAL_PARAMETER(?,?,?,?,?) }";

            return jdbcTemplate.execute(function, (CallableStatementCallback<String>) cs -> {
                cs.registerOutParameter(1, Types.VARCHAR);
                cs.setInt(2, 1);
                cs.setString(3, parameterName);
                cs.setString(4, parameterKeyIdType);
                cs.setString(5, parameterKeyId);
                cs.setString(6, defaultValue);
                cs.execute();
                return cs.getString(1);
            });
        } catch (Exception ex) {
            log.error("Failed to load parameter {} using legacy function", parameterName, ex);
            return "";
        }
    }
}
