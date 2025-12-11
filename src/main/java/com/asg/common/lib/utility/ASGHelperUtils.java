package com.asg.common.lib.utility;


import com.asg.common.lib.security.util.UserContext;
import io.micrometer.common.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ASGHelperUtils {
    public static String convertListToString(List<String> roles) {
        return (roles == null || roles.isEmpty()) ? null : String.join(";", roles);
    }

    public static List<String> convertFromStringToList(String dbValue) {
        return (StringUtils.isBlank(dbValue)) ? Collections.emptyList() : Arrays.asList(dbValue.split(";"));
    }

    public static List<Long> convertFromStringToLongList(String dbValue) {
        if (StringUtils.isBlank(dbValue)) {
            return Collections.emptyList();
        }

        return Arrays.stream(dbValue.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    public static String getCurrentUser() {
        return UserContext.getUserId() != null ? String.valueOf(UserContext.getUserId()) : "SYSTEM";
    }

    public static Long getCompanyId() {
        return UserContext.getCompanyPoid() != null ? UserContext.getCompanyPoid() : 1L;
    }

    public static Long getGroupId() {
        return UserContext.getGroupPoid() != null ? UserContext.getGroupPoid() : 1L;
    }

    public static Long getUserPoid() {
        return UserContext.getUserPoid() != null ? UserContext.getUserPoid()  : 1L;
    }

}

