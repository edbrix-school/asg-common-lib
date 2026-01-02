package com.asg.common.lib.entity;

import com.asg.common.lib.entity.key.CurrencyRateId;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Date;

@Data
@Entity
@IdClass(CurrencyRateId.class)
@Table(name = "GLOBAL_CURRENCY_RATES")
public class CurrencyRateEntity {

    @Id
    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Id
    @Column(name = "CURRENCY_CODE")
    private String currencyCode;

    @Id
    @Column(name = "RATE_DATE", nullable = false)
    private Date rateDate;

    @Column(name = "BUY_RATE")
    private BigDecimal buyRate;

    @Column(name = "SELL_RATE")
    private BigDecimal sellRate;

    @Column(name = "DOC_REF", length = 25)
    private String docRef;

    @Column(name = "DELETED", length = 1)
    private String deleted;

}
