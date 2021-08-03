package io.mosip.registration.processor.packet.storage.dto;

import java.io.Serializable;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class TagResponseDto implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	Map<String, String> tags;
}

