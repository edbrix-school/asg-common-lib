package com.asg.common.lib.entity;
import com.asg.common.lib.annotation.AuditIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "GLOBAL_CURRENCY_MASTER")
public class CurrencyEntity {
    @Id
    @SequenceGenerator(
            name = "currency_seq",
            sequenceName = "GLOBAL_CURRENCY_MASTER_SEQ",
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "currency_seq")
    @Column(name = "CURRENCY_POID")
    @AuditIgnore
    private Long currencyPoid;

    @Column(name = "GROUP_POID", nullable = false)
    @AuditIgnore
    private Long groupPoid;

    @Column(name = "CURRENCY_CODE", nullable = false)
    @AuditIgnore
    private String currencyCode;

    @Column(name = "CURRENCY_NAME", unique = true)
    private String currencyName;

    @Column(name = "CURRENCY_NAME2")
    private String currencyName2;

    @Column(name = "CURRENCY_SHORT_NAME")
    private String currencyShortName;

    @Column(name = "COIN_SHORT_NAME")
    private String coinShortName;

    @Column(name = "NUMBER_FORMAT_CURRENCY")
    @AuditIgnore
    private String numberFormatCurrency;

    @Column(name = "SEQNO")
    @AuditIgnore
    private Integer seqno;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "CURRENCY_DECIMALS")
    private Integer decimals;

    @Column(name = "CREATED_BY")
    @AuditIgnore
    private String createdBy;

    @Column(name = "CREATED_DATE")
    @AuditIgnore
    private OffsetDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    @AuditIgnore
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    @AuditIgnore
    private OffsetDateTime lastModifiedDate;

    @Column(name = "DELETED")
    @AuditIgnore
    private String deleted;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "CURRENCY_CODE", referencedColumnName = "CURRENCY_CODE", insertable = false, updatable = false)
    @OrderBy("rateDate DESC")
    private List<CurrencyRateEntity> rates;
}