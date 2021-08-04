package io.mosip.registration.processor.status.dto;

import java.util.List;
import io.mosip.registration.processor.core.common.rest.dto.BaseRestRequestDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Instantiates a new registration external status request DTO.
 * @author Satish Gohil
 */
@Data

/* (non-Javadoc)
 * @see io.mosip.registration.processor.core.common.rest.dto.BaseRestRequestDTO#hashCode()
 */
@EqualsAndHashCode(callSuper = true)
public class RegistrationExternalStatusRequestDTO extends BaseRestRequestDTO {
	
	/**
	 *
	 */
	private static final long serialVersionUID = -2987693214912415439L;
	/** The request. */
	private List<RegistrationExternalStatusSubRequestDto> request;
		
}
