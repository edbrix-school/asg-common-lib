package com.asg.common.lib.repository;

import com.asg.common.lib.dto.DropdownDto;
import com.asg.common.lib.entity.TimeZoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimeZoneDataRepository extends JpaRepository<TimeZoneEntity, Long> {
    @Query("SELECT new com.asg.common.lib.dto.DropdownDto(CAST(t.timezoneId AS long), t.timezoneCode) " +
            "FROM TimeZoneEntity t WHERE t.status = 'Y' ORDER BY t.timezoneCode")
    List<DropdownDto> findActiveTimezoneDropdown();

    TimeZoneEntity findByTimezoneId(Long timezoneId);
}
