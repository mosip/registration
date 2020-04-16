package io.mosip.registration.processor.core.packet.dto.demographicinfo;

import java.io.Serializable;

import lombok.Data;

/**
 * Instantiates a new demographic info dto.
 */

/**
 * Instantiates a new demographic info dto.
 */
@Data
public class DemographicInfoDto implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The reg id. */
	private String regId;

	/** The lang code. */
	private String langCode;

	/** The name. */
	private String name;

	/** The dob. */
	private String dob;

	/** The gender code. */
	private String genderCode;

	/** The phone. */
	private String phone;

	/** The email. */
	private String email;

	/** The postalcode. */
	private String postalcode;

}
