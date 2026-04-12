package com.asg.common.lib.dto.response;

import com.asg.common.lib.dto.DetailsDto;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CostCenterResponseDto {
	private String costCenterCode;
	private String costCenterDescription;
	private String costCenterDescription2;
	private String costCenterType;
	private Long parentCostCenterPoid;
	private DetailsDto parentCostCenterPoidDtl;
	private Long groupPoid;
	private String remarks;
	private String active;
	private Integer seqNo;
	private String createdBy;
	private LocalDateTime createdDate;
}