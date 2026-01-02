package com.asg.common.lib.dto;

import lombok.Data;

@Data
public class AddressMasterUpsertDto {
    private Long addressMasterPoid;
    private String addressName;
    private String addressName2;
    private Long groupPoid;
    private Long countryId;
    private String active;
    private Long seqno;
    private AddressTypeMapDTO addressTypeMap;
}
