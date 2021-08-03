package io.mosip.registration.processor.packet.storage.dto;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;



/**
 * The Class TagRequestDto.
 */
@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class TagRequestDto implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** The id. */
	private String id;
	
	/** The tag names. */
	private List<String> tagNames;
}
