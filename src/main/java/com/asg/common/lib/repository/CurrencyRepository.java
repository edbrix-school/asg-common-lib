package com.asg.common.lib.repository;

import com.asg.common.lib.entity.CurrencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrencyRepository extends JpaRepository<CurrencyEntity, Long> {
}
