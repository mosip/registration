package io.mosip.registration.processor.status.dto;

import java.util.List;

import io.mosip.registration.processor.core.common.rest.dto.BaseRestResponseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The Class LostRidResponseDto.
 * 
 * @author Dhanendra
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LostRidResponseDto extends BaseRestResponseDTO {

	private static final long serialVersionUID = 4422198670538094471L;

	private List<LostRidDto> response;

	private List<ErrorDTO> errors;

}
