package io.mosip.registration.processor.core.packet.dto.abis;

import java.io.Serializable;

import lombok.Data;

/**
 * The Class CandidatesDto.
 * 
 * @author M1048860 Kiran Raj
 */
@Data
public class CandidatesDto implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4746292144740017993L;

	/** The reference id. */
	private String referenceId;

	private Analytics analytics;

	private Modalities[] modalities;

	

	public Modalities[] getModalities() {
		return modalities != null ? modalities.clone() : null;
	}

	public void setModalities(Modalities[] modalities) {
		this.modalities = modalities != null ? modalities : null;
	}

}
