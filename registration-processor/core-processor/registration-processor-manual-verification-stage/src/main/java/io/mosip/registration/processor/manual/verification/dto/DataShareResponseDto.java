package io.mosip.registration.processor.manual.verification.dto;

import java.util.List;

import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import lombok.Data;


@Data
public class DataShareResponseDto {

	private String policyId;
	private String signature;
	private String subscriberId;
	private Integer transactionsAllowed;
	private String url;
	private Integer validForInMinutes;
	private List<ErrorDTO> errors;
}
