package com.asg.common.lib.entity;

import com.asg.common.lib.security.util.UserContext;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class AuditListener {

    @PrePersist
    public void setCreatedDate(BaseEntity entity) {
        LocalDateTime now = getCurrentDateTimeInUserTimeZone();
        entity.setCreatedDate(now);
        entity.setCreatedBy(getCurrentUser());
        entity.setLastModifiedDate(now);
        entity.setLastModifiedBy(getCurrentUser());
    }

    @PreUpdate
    public void setLastModifiedDate(BaseEntity entity) {
        entity.setLastModifiedDate(getCurrentDateTimeInUserTimeZone());
        entity.setLastModifiedBy(getCurrentUser());
    }

    private LocalDateTime getCurrentDateTimeInUserTimeZone() {
        String timeZoneCode = UserContext.getTimeZoneCode();
        ZoneId zoneId = ZoneId.of(timeZoneCode);
        return ZonedDateTime.now(zoneId).toLocalDateTime();
    }

    private static String getCurrentUser() {
        return UserContext.getUserId() != null ? String.valueOf(UserContext.getUserId()) : "SYSTEM";
    }

}
