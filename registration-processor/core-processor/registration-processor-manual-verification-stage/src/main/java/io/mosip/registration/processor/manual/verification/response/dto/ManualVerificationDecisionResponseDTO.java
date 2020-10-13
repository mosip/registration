package io.mosip.registration.processor.manual.verification.response.dto;

import java.util.List;

import io.mosip.registration.processor.core.common.rest.dto.BaseRestResponseDTO;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.manual.verification.dto.ManualVerificationDecisionDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
@EqualsAndHashCode(callSuper = true)
@Data
public class ManualVerificationDecisionResponseDTO extends BaseRestResponseDTO{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1619991299257766060L;
	/** The response. */
	private ManualVerificationDecisionDto response;
	
	/** The error. */
	private List<ErrorDTO> errors;
}
