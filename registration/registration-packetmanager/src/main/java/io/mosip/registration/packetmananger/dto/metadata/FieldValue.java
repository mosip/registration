package io.mosip.registration.packetmananger.dto.metadata;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode
public class FieldValue {
	
	private String label;
	private String value;

}
