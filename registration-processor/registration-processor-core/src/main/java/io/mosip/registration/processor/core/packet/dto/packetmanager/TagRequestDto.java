package io.mosip.registration.processor.core.packet.dto.packetmanager;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class TagRequestDto implements Serializable {
	
	private String id;
	private List<String> tagNames;
}
