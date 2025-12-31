package com.asg.common.lib.dto.response;

import com.asg.common.lib.dto.AddressTypeMapDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class AddressMasterResponse {
    private Long addressMasterPoid;
    
    @NotBlank(message = "Address Name is mandatory")
    @Size(max = 100, message = "Address Name must not exceed 100 characters")
    private String addressName;
    
    @Size(max = 100, message = "Address Name 2 must not exceed 100 characters")
    private String addressName2;

    @Size(max = 5, message = "Preferred Communication list must not have more than 5 values")
    private List<@Size(max = 20, message = "Preferred Communication value must not exceed 20 characters") String> preferredCommunication;

    @Size(max = 250, message = "Remarks must not exceed 250 characters")
    private String remarks;
    
    @Size(max = 100, message = "Party Type must not exceed 100 characters")
    private List<String> partyType;

    @Size(max = 30, message = "Mobile must not exceed 30 characters")
    private String mobile;
    
    private List<@Size(max = 100, message = "Email value must not exceed 100 characters") String> email;

    @Size(max = 50, message = "WhatsApp Number must not exceed 50 characters")
    private String whatsappNo;
    
    @Size(max = 255, message = "LinkedIn must not exceed 255 characters")
    private String linkedIn;
    
    @Size(max = 255, message = "Instagram must not exceed 255 characters")
    private String instagram;
    
    @Size(max = 255, message = "Facebook must not exceed 255 characters")
    private String facebook;
    
    @Size(max = 100, message = "CR Number must not exceed 100 characters")
    private String crNumber;
    
    private Boolean isForwarder;
    
    @Size(max = 1, message = "Active must be Y or N")
    private String active;

    private Long seqno;
    
    @NotNull(message = "Country is mandatory")
    private Long countryId;
    
    private String countryName;

    @Valid
    private AddressTypeMapDTO addressTypeMap;
    
    private String createdBy;
    private OffsetDateTime createdDate;
    private String lastModifiedBy;
    private OffsetDateTime lastModifiedDate;
}
