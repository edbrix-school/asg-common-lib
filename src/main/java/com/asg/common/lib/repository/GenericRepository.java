package com.asg.common.lib.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GenericRepository extends JpaRepository<Object, Long> {

    @Query(value = "SELECT c.CURRENCY_DECIMALS FROM GLOBAL_CURRENCY_MASTER c " +
                   "WHERE c.CURRENCY_POID = (SELECT cm.CURRENCY_POID FROM GLOBAL_COMPANY_MASTER cm WHERE cm.COMPANY_POID = :companyPoid)", 
           nativeQuery = true)
    Integer getCurrencyDecimalsByCompanyPoid(@Param("companyPoid") Long companyPoid);
}
