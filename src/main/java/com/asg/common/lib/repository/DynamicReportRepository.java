package com.asg.common.lib.repository;

import com.asg.common.lib.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Map;

@Repository
public interface DynamicReportRepository extends JpaRepository<DocumentEntity, Long> {

    @Query(value = "SELECT CM.COMPANY_CODE, CM.COMPANY_NAME, CM.LOGO_HDR_IMAGE, " +
                   "FINANCIAL_PERIOD_START, FINANCIAL_PERION_END, REPORT_PERIOD_START, REPORT_PERIOD_END, " +
                   "TRANS_PERIOD_START, TRANS_PERIOD_END, PROVISIONAL_CLOSED_DATE " +
                   "FROM GLOBAL_COMPANY_MASTER CM " +
                   "WHERE CM.COMPANY_POID = :companyPoid", nativeQuery = true)
    Map<String, Object> getLoginReportPeriod(@Param("companyPoid") Long companyPoid);
}