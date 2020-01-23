package io.mosip.registration.processor.core.packet.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AuditRequestDTO extends BaseDTO {
	private AuditDTO request;
}
