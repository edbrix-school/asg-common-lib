package com.asg.common.lib.utility;

import com.asg.common.lib.repository.CompanyRepository;
import com.asg.common.lib.repository.CurrencyRepository;
import com.asg.common.lib.security.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CurrencyHelper {

    private static CompanyRepository companyRepository;
    private static CurrencyRepository currencyRepository;

    public CurrencyHelper(CompanyRepository companyRepository, CurrencyRepository currencyRepository) {
        CurrencyHelper.companyRepository = companyRepository;
        CurrencyHelper.currencyRepository = currencyRepository;
    }

    public static Integer getCurrencyDecimals() {
        try {
            Long companyPoid = UserContext.getCompanyPoid();
            if (companyPoid == null || companyRepository == null) {
                return 3;
            }

            return companyRepository.findById(companyPoid)
                    .map(company -> company.getCurrencyPoid())
                    .flatMap(currencyRepository::findById)
                    .map(currency -> currency.getDecimals())
                    .orElse(3);
        } catch (Exception e) {
            log.warn("Error fetching currency decimals, using default: {}", e.getMessage());
            return 3;
        }
    }
}
