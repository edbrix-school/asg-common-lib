package com.asg.common.lib.config;

import com.asg.common.lib.utility.DiffUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@RequiredArgsConstructor
public class DiffUtilAutoConfig {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        DiffUtil.setJdbcTemplate(jdbcTemplate);
    }
}
