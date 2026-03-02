package com.asg.common.lib.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddressTypeMapDTO {
    @Valid
    @JsonProperty("MAIN")
    private List<AddressDetailsDTO> MAIN;

    @Valid
    @JsonProperty("FINANCE")
    private List<AddressDetailsDTO> FINANCE;

    @Valid
    @JsonProperty("SALES")
    private List<AddressDetailsDTO> SALES;

    @Valid
    @JsonProperty("OPERATIONS")
    private List<AddressDetailsDTO> OPERATIONS;

    @Valid
    @JsonProperty("INVOICE")
    private List<AddressDetailsDTO> INVOICE;

    @Valid
    @JsonProperty("DELIVERY_ORDER")
    private List<AddressDetailsDTO> DELIVERY_ORDER;

    @Valid
    @JsonProperty("CARGO_ARRIVAL_NOTICE")
    private List<AddressDetailsDTO> CARGO_ARRIVAL_NOTICE;

    @Valid
    @JsonProperty("SHIP_CHANDLING")
    private List<AddressDetailsDTO> SHIP_CHANDLING;

    @Valid
    @JsonProperty("CLAIM_UAC")
    private List<AddressDetailsDTO> CLAIM_UAC;

    @Valid
    @JsonProperty("CAN")
    private List<AddressDetailsDTO> CAN;
}
