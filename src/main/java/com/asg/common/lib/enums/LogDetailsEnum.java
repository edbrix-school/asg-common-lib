package com.asg.common.lib.enums;

public enum LogDetailsEnum {
    VIEWED("Viewed -"),
    CREATED("Created -"),
    MODIFIED("Modified -"),
    DEACTIVATED("Deactivated -"),
    DELETED("Deleted -"),
    APPROVED("Approved -"),
    REJECTED("Rejected -"),
    SUBMITTED("Submitted -"),
    CANCELLED("Cancelled -"),
    LOGIN("Login -"),
    LOGOUT("Logout -"),
    PASSWORD_RESET("Password Reset -"),
    PASSWORD_EMAIL_SENT("Password Email Sent -"),
    STATUS_CHANGED("Status Changed-"),
    ATTACHMENTS_UPLOADED("Attachments uploaded -"),
    ATTACHMENT_DELETED("Attachment deleted -"),
    ATTACHMENT_UPDATED("Attachment updated -"),
    ATTACHMENT_ARCHIVED("Attachment archived -"),
    ATTACHMENT_DOWNLOADED("Attachment downloaded -"),
    ATTACHMENT_VIEWED("Attachment viewed -");

    private final String description;

    LogDetailsEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}