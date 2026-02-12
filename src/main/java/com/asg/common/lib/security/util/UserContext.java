package com.asg.common.lib.security.util;

import com.asg.common.lib.security.model.CustomAuthDetails;

public class UserContext {
    private static final ThreadLocal<CustomAuthDetails> userContext = new ThreadLocal<>();

    public static void setCurrentUser(CustomAuthDetails userDetails) {
        userContext.set(userDetails);
    }

    public static CustomAuthDetails getCurrentUser() {
        return userContext.get();
    }

    public static String getUserEmail() {
        CustomAuthDetails details = getCurrentUser();
        return details != null ? details.getUserEmail() : null;
    }

    public static String getUserId() {
        CustomAuthDetails details = getCurrentUser();
        return details != null ? details.getUserId() : null;
    }

    public static String getUserName() {
        CustomAuthDetails details = getCurrentUser();
        return details != null ? details.getUserName() : null;
    }

    public static Long getUserPoid() {
        CustomAuthDetails details = getCurrentUser();
        return details != null ? details.getUserPoid() : null;
    }

    public static Long getGroupPoid() {
        CustomAuthDetails details = getCurrentUser();
        return details != null ? details.getGroupPoid() : null;
    }

    public static Long getCompanyPoid() {
        CustomAuthDetails details = getCurrentUser();
        return details != null ? details.getCompanyPoid() : null;
    }

    public static String getActionRequested() {
        CustomAuthDetails details = getCurrentUser();
        return details != null ? details.getActionRequested() : null;
    }

    public static String getDocumentId() {
        CustomAuthDetails details = getCurrentUser();
        return details != null ? details.getDocumentId() : null;
    }

    public static String getUserRole() {
        CustomAuthDetails details = getCurrentUser();
        return details != null ? details.getUserRole() : null;
    }

    public static Boolean isLogEnabled() {
        CustomAuthDetails details = getCurrentUser();
        return details != null ? details.getLogEnabled() : true;
    }

    public static String getTimeZoneCode() {
        CustomAuthDetails details = getCurrentUser();
        return details != null ? details.getTimeZoneCode() : null;
    }

    public static void clear() {
        userContext.remove();
    }
}
