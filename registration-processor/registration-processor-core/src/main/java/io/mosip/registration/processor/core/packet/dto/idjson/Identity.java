package io.mosip.registration.processor.core.packet.dto.idjson;

import java.math.BigInteger;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * This class contains the applicant demographic, biometric, proofs and introducer
 * biometric details.
 *
 * @author Balaji Sridharan
 * @since 1.0.0
 */

@JsonInclude(value = Include.NON_EMPTY)
@Data
public class Identity { 

	/** The ID schema version. */
	@JsonProperty(value = "IDSchemaVersion")
	private double idSchemaVersion;

	/** The uin. */
	@JsonProperty(value = "UIN")
	private BigInteger uin;

	/** The full name. */
	private List<ValuesDTO> fullName;

	/** The date of birth. */
	private String dateOfBirth;

	/** The age. */
	private Integer age;

	/** The gender. */
	private List<ValuesDTO> gender;

	/** The phone. */
	private String phone;

	/** The email. */
	private String email;

	/** The introducer RID or UIN. */
	private BigInteger introducerRIDOrUIN;

	/** The introducer name. */
	private List<ValuesDTO> introducerName;

	/** The proof of address. */
	private DocumentDetailsDTO proofOfAddress;

	/** The proof of identity. */
	private DocumentDetailsDTO proofOfIdentity;

	/** The proof of relationship. */
	private DocumentDetailsDTO proofOfRelationship;

	/** The date of birth proof. */
	private DocumentDetailsDTO proofOfDateOfBirth;

	/** The individual biometrics. */
	private CBEFFFilePropertiesDTO individualBiometrics;

	/** The introducer biometrics. */
	private CBEFFFilePropertiesDTO introducerBiometrics;

	@Override
	public String toString() {
		return "Identity [idSchemaVersion=" + idSchemaVersion + ", uin=" + uin + ", fullName=" + fullName
				+ ", dateOfBirth=" + dateOfBirth + ", age=" + age + ", gender=" + gender + ", phone="
				+ phone + ", email=" + email + ", introducerRIDOrUIN=" + introducerRIDOrUIN
				+ ", introducerName=" + introducerName + ", proofOfAddress=" + proofOfAddress
				+ ", proofOfIdentity=" + proofOfIdentity + ", proofOfRelationship=" + proofOfRelationship
				+ ", proofOfDateOfBirth=" + proofOfDateOfBirth + ", individualBiometrics=" + individualBiometrics
				+ ", introducerBiometrics=" + introducerBiometrics + "]";
	}

}