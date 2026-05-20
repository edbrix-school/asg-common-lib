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
    private List<AddressDetailsDTO> main;

    @Valid
    @JsonProperty("FINANCE")
    private List<AddressDetailsDTO> finance;

    @Valid
    @JsonProperty("SALES")
    private List<AddressDetailsDTO> sales;

    @Valid
    @JsonProperty("OPERATION")
    private List<AddressDetailsDTO> operation;

    @Valid
    @JsonProperty("INVOICE_ADDRESS")
    private List<AddressDetailsDTO> invoiceAddress;

    @Valid
    @JsonProperty("DELIVERY_ORDER")
    private List<AddressDetailsDTO> deliveryOrder;

    @Valid
    @JsonProperty("SHIP_CHANDLING")
    private List<AddressDetailsDTO> shipChandling;

    @Valid
    @JsonProperty("CLAIM_UAC")
    private List<AddressDetailsDTO> claimUac;

    @Valid
    @JsonProperty("CAN")
    private List<AddressDetailsDTO> can;
}
