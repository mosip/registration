package io.mosip.registration.processor.status.dto;

import io.mosip.registration.processor.core.common.rest.dto.BaseRestRequestDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The Class LostRidResponseDto
 *
 * @author Dhanendra
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LostRidRequestDto extends BaseRestRequestDTO {

	private static final long serialVersionUID = -2987693214912415439L;

	private SearchInfo request;
}
