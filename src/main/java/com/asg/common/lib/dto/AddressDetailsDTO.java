package com.asg.common.lib.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressDetailsDTO {
    private String addressPoid;

    @Size(max = 20, message = "Address Type must not exceed 20 characters")
    private String addressType;

    @Size(max = 50, message = "Contact Person must not exceed 50 characters")
    private String contactPerson;

    @Size(max = 50, message = "Designation must not exceed 50 characters")
    private String designation;

    @Size(max = 30, message = "Office Tel 1 must not exceed 30 characters")
    private String offTel1;

    @Size(max = 30, message = "Office Tel 2 must not exceed 30 characters")
    private String offTel2;

    @Size(max = 30, message = "Mobile must not exceed 30 characters")
    private String mobile;

    @Size(max = 30, message = "Fax must not exceed 30 characters")
    private String fax;

    private List<@Email(message = "Invalid email format") String> email;

    @Size(max = 500, message = "Website must not exceed 500 characters")
    private String website;

    @Size(max = 30, message = "PO Box must not exceed 30 characters")
    private String poBox;

    @Size(max = 100, message = "Office Number must not exceed 100 characters")
    private String offNo;

    @Size(max = 100, message = "Building must not exceed 100 characters")
    private String bldg;

    @Size(max = 100, message = "Road must not exceed 100 characters")
    private String road;

    @Size(max = 100, message = "Area must not exceed 100 characters")
    private String area;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Size(max = 100, message = "State must not exceed 100 characters")
    private List<String> state;

    @Size(max = 250, message = "Landmark must not exceed 250 characters")
    private String landMark;

    @Size(max = 1, message = "Verified must be Y or N")
    private String verified;

    @Size(max = 100, message = "Verified By must not exceed 100 characters")
    private String verifiedBy;

    @JsonFormat(pattern = "dd-MMM-yyyy")
    private LocalDate verifiedDate;

    private String createdBy;

    @JsonFormat(pattern = "dd-MMM-yyyy HH:mm:ss")
    private LocalDateTime createdDate;

    private String lastModifiedBy;

    @JsonFormat(pattern = "dd-MMM-yyyy HH:mm:ss")
    private LocalDateTime lastModifiedDate;

    @Size(max = 30, message = "WhatsApp Number must not exceed 30 characters")
    private String whatsappNo;

    @Size(max = 250, message = "LinkedIn must not exceed 250 characters")
    private String linkedIn;

    @Size(max = 250, message = "Instagram must not exceed 250 characters")
    private String instagram;

    @Size(max = 250, message = "Facebook must not exceed 250 characters")
    private String facebook;
    
    private String actionType;
}
