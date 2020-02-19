package io.mosip.registration.processor.core.packet.dto;

import java.util.ArrayList;
import java.util.List;

import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.registration.processor.core.common.rest.dto.BaseRestResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class AuditRespDTO extends BaseRestResponseDTO {

	private static final long serialVersionUID = -7425716515304969709L;
	private AuditResponseDTO response;
	private List<ServiceError> errors = new ArrayList<>();
}
