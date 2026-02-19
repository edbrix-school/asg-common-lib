package com.asg.common.lib.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.key.CurrencyRateId;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@IdClass(CurrencyRateId.class)
@Table(name = "GLOBAL_CURRENCY_RATES")
public class CurrencyRateEntity {

    @Id
    @Column(name = "GROUP_POID")
    @AuditIgnore
    private Long groupPoid;

    @Id
    @Column(name = "CURRENCY_CODE")
    @AuditIgnore
    private String currencyCode;

    @Id
    @Column(name = "RATE_DATE", nullable = false)
    private LocalDate rateDate;

    @Column(name = "BUY_RATE")
    private BigDecimal buyRate;

    @Column(name = "SELL_RATE")
    private BigDecimal sellRate;

    @Column(name = "DOC_REF", length = 25)
    @AuditIgnore
    private String docRef;

    @Column(name = "DELETED", length = 1)
    @AuditIgnore
    private String deleted;

}
