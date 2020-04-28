package io.mosip.registration.processor.core.packet.dto.abis;

import java.io.Serializable;

import lombok.Data;

/**
 * The Class AbisIdentifyRequestDto.
 *
 * @author M1048860 Kiran Raj
 */
@Data
public class AbisIdentifyRequestDto extends AbisCommonRequestDto implements Serializable {


	
	/** The reference url. */
	private String referenceUrl;
	
	
	private Flag flags;


	/** The gallery. */
	private AbisIdentifyRequestGalleryDto gallery;





}
