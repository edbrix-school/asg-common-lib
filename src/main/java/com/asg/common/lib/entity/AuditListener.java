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
        String currentUser = UserContext.getUserId();
        entity.setCreatedDate(now);
        entity.setCreatedBy(currentUser);
        entity.setLastModifiedDate(now);
        entity.setLastModifiedBy(currentUser);
    }

    @PreUpdate
    public void setLastModifiedDate(BaseEntity entity) {
        entity.setLastModifiedDate(getCurrentDateTimeInUserTimeZone());
        entity.setLastModifiedBy(UserContext.getUserId());
    }

    private LocalDateTime getCurrentDateTimeInUserTimeZone() {
        String timeZoneCode = UserContext.getTimeZoneCode();
        ZoneId zoneId = ZoneId.of(timeZoneCode);
        return ZonedDateTime.now(zoneId).toLocalDateTime();
    }
}
