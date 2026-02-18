package com.asg.common.lib.entity.key;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class CurrencyRateId implements Serializable {

    private Long groupPoid;
    private String currencyCode;
    private LocalDate rateDate;

    public CurrencyRateId() {}

    public CurrencyRateId(Long groupPoid, String currencyCode, LocalDate rateDate) {
        this.groupPoid = groupPoid;
        this.currencyCode = currencyCode;
        this.rateDate = rateDate;
    }

    // equals and hashCode are REQUIRED
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CurrencyRateId)) return false;
        CurrencyRateId that = (CurrencyRateId) o;
        return Objects.equals(groupPoid, that.groupPoid) &&
                Objects.equals(currencyCode, that.currencyCode) &&
                Objects.equals(rateDate, that.rateDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupPoid, currencyCode, rateDate);
    }

    // Getters and setters
}
