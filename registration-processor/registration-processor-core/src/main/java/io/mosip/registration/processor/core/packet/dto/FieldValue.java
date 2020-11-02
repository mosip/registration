package io.mosip.registration.processor.core.packet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class contains the attributes to be displayed for flat value object in
 * PacketMetaInfo JSON
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldValue {

	private String label;
	private String value;
}
