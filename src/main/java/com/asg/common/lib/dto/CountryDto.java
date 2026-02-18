package com.asg.common.lib.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@RequiredArgsConstructor
public class CountryDto {
    private Long countryPoid;
    @NotNull(message = "GroupPoid is required")
    private Long groupPoid;
    @NotBlank(message = "CountryCode is required")
    private String countryCode;
    @NotBlank(message = "CountryName is required")
    private String countryName;
    private String countryName2;
    private String active;
    private Integer seqNo;
    private Double countryTicketRate;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
