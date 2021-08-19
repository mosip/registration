package io.mosip.registration.processor.core.packet.dto.packetmanager;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class TagResponseDto implements Serializable {
	
	Map<String, String> tags;
}
