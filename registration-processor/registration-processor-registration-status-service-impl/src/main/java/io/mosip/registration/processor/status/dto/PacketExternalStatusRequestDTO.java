package io.mosip.registration.processor.status.dto;

import java.util.List;

import io.mosip.registration.processor.core.common.rest.dto.BaseRestRequestDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Instantiates a new packet external status request DTO.
 */
@Data

/* (non-Javadoc)
 * @see io.mosip.registration.processor.core.common.rest.dto.BaseRestRequestDTO#hashCode()
 */
@EqualsAndHashCode(callSuper = true)
public class PacketExternalStatusRequestDTO extends BaseRestRequestDTO {
	
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -6179194616292457421L; 
	
	/** The request. */
	private List<PacketExternalStatusSubRequestDTO> request;

}
