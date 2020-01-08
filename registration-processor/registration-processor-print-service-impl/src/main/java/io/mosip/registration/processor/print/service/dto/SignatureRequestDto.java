package io.mosip.registration.processor.print.service.dto;


import javax.validation.constraints.NotBlank;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Crypto-Manager-Request model
 * 
 * @author Urvil Joshi
 *
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "Model representing a Crypto-Manager-Service Request")
public class SignatureRequestDto {
	/**
	 * Application id of decrypting module
	 */
	

	private String applicationId;
	/**
	 * Refrence Id
	 */

	private String referenceId;
	/**
	 * Timestamp
	 */

	private String timeStamp;
	/**
	 * Data in BASE64 encoding to encrypt/decrypt
	 */

	private String data;

}
