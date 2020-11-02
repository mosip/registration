/**
 * 
 */
package io.mosip.registration.processor.core.auth.dto;

import java.time.LocalDateTime;

import io.mosip.registration.processor.core.packet.dto.DigitalId;
import lombok.Data;

/**
 * @author Ranjitha Siddegowda
 *
 */
@Data
public class DataInfoDTO {

	private DigitalId digitalId;

	/** The Value for type */
	private String bioType;

	/** The Value for subType */
	private String bioSubType;

	/** The Value for bioValue */
	private String bioValue;

	/** The Value for deviceCode */
	private String deviceCode;

	/** The Value for deviceServiceVersion */
	private String deviceServiceVersion;

	/** The Value for transactionID */
	private String transactionId;

	/** The Value for time stamp */
	private String timestamp;

	/** The Value for mosipProcess */
	private String purpose;

	/** The Value for environment */
	private String env;

	/** The Value for version */
	private String version;

	private String domainUri;

	private Float requestedScore;

	private Float qualityScore;

}
