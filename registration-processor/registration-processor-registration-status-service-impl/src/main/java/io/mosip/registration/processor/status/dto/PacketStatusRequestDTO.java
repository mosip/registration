package io.mosip.registration.processor.status.dto;

import java.util.List;

import io.mosip.registration.processor.core.common.rest.dto.BaseRestRequestDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PacketStatusRequestDTO extends BaseRestRequestDTO {/**
	 * 
	 */
	private static final long serialVersionUID = -6179194616292457421L; 
	
	private List<PacketStatusSubRequestDTO> request;

}
