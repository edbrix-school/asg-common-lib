package com.asg.common.lib.security.model;

public class CustomAuthDetails {
    private String actionRequested;
    private String documentId;
    private String userName;
    private Long userPoid;
    private String userId;
    private Long groupPoid;
    private Long companyPoid;
    private String userEmail;
    private String userRole;
    private Boolean logEnabled = true;
    private String timeZoneCode;

    public CustomAuthDetails() {
    }

    public String getActionRequested() {
        return actionRequested;
    }

    public void setActionRequested(String actionRequested) {
        this.actionRequested = actionRequested;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Long getUserPoid() {
        return userPoid;
    }

    public void setUserPoid(Long userPoid) {
        this.userPoid = userPoid;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getGroupPoid() {
        return groupPoid;
    }

    public void setGroupPoid(Long groupPoid) {
        this.groupPoid = groupPoid;
    }

    public Long getCompanyPoid() {
        return companyPoid;
    }

    public void setCompanyPoid(Long companyPoid) {
        this.companyPoid = companyPoid;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public Boolean getLogEnabled() {
        return logEnabled;
    }

    public void setLogEnabled(Boolean logEnabled) {
        this.logEnabled = logEnabled;
    }
    public String getTimeZoneCode() {
        return timeZoneCode;
    }
    public void setTimeZoneCode(String timeZoneCode) {
        this.timeZoneCode = timeZoneCode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String actionRequested;
        private String documentId;
        private String userName;
        private Long userPoid;
        private String userId;
        private Long groupPoid;
        private Long companyPoid;
        private String userEmail;
        private String userRole;
        private Boolean logEnabled;
        private String timeZoneCode;

        public Builder actionRequested(String actionRequested) {
            this.actionRequested = actionRequested;
            return this;
        }

        public Builder documentId(String documentId) {
            this.documentId = documentId;
            return this;
        }

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder userPoid(Long userPoid) {
            this.userPoid = userPoid;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder groupPoid(Long groupPoid) {
            this.groupPoid = groupPoid;
            return this;
        }

        public Builder companyPoid(Long companyPoid) {
            this.companyPoid = companyPoid;
            return this;
        }

        public Builder userEmail(String userEmail) {
            this.userEmail = userEmail;
            return this;
        }

        public Builder userRole(String userRole) {
            this.userRole = userRole;
            return this;
        }

        public Builder logEnabled(Boolean logEnabled) {
            this.logEnabled = logEnabled;
            return this;
        }

        public Builder timeZoneCode(String timeZoneCode) {
            this.timeZoneCode = timeZoneCode;
            return this;
        }

        public CustomAuthDetails build() {
            CustomAuthDetails details = new CustomAuthDetails();
            details.setActionRequested(this.actionRequested);
            details.setDocumentId(this.documentId);
            details.setUserName(this.userName);
            details.setUserPoid(this.userPoid);
            details.setUserId(this.userId);
            details.setGroupPoid(this.groupPoid);
            details.setCompanyPoid(this.companyPoid);
            details.setUserEmail(this.userEmail);
            details.setUserRole(this.userRole);
            details.setLogEnabled(this.logEnabled);
            details.setTimeZoneCode(this.timeZoneCode);
            return details;
        }
    }
}