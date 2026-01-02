package com.asg.common.lib.service;

import com.asg.common.lib.security.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Component
public class PdfContextParamsProvider {

    private final DataSource dataSource;

    public Map<String, Object> baseParams() {
        Map<String, Object> p = new HashMap<>();
        p.put("LOGIN_COMP_POID", UserContext.getCompanyPoid());
        p.put("LOGIN_DIV_POID", getDivisionPoid(UserContext.getCompanyPoid()));
        p.put("LOGIN_GROUP_POID", UserContext.getGroupPoid());
        p.put("LOGIN_USER_POID", UserContext.getUserPoid());
        return p;
    }

    private String getDivisionPoid(Long companyPoid) {
        if (companyPoid == null) return "581";
        try (Connection conn = dataSource.getConnection()) {
            var stmt = conn.prepareStatement("SELECT DIV_POID FROM GLOBAL_COMPANY_MASTER_DIV_DTL WHERE COMPANY_POID = ? AND ROWNUM = 1");
            stmt.setLong(1, companyPoid);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch division POID for company {}", companyPoid, e);
        }
        return "581";
    }
}
