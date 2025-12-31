package com.asg.common.lib.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlPostingRequestDto {
    private int loginGroupPoid;
    private int loginCompanyPoid;
    private int loginUserPoid;
    private String docId;
    private int transactionPoid;
    private int docRef;
}
