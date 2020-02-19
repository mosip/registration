package io.mosip.registration.processor.core.packet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditResponseDTO {

	/**
	 * The boolean audit status
	 */
	private boolean status;
}
