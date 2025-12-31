package com.asg.common.lib.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalTermsInsertRequestDto {
    private Long groupPoid;
    private Long companyPoid;
    private String docId;
    private Long docKeyPoid;
    private Long loginUserPoid;

    private Long termsPoid;
    private Long detRowId;
    private Long rowSeqNo;

    private String clauseNo;
    private String clauseDetails;
}
