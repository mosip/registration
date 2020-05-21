/**
 * 
 */
package io.mosip.registration.processor.core.packet.dto.abis;

import java.io.Serializable;

import lombok.Data;

/**
 * @author M1022006
 *
 */
@Data
public class Flag implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** The max results. */
	private String maxResults;

	/** The target FPIR. */
	private String targetFPIR;

}
