package io.mosip.registration.processor.status.dto;

import java.time.LocalDateTime;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.mosip.registration.processor.status.constants.CryptomanagerConstant;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 
 * @author Girish Yarru
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "Model representing a Crypto-Manager-Service Request")
public class CryptomanagerRequestDto {
	/**
	 * Application id of decrypting module
	 */

	@NotBlank(message = "should not be null or empty")
	private String applicationId;
	/**
	 * Refrence Id
	 */

	private String referenceId;
	/**
	 * Timestamp
	 */

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
	@NotNull
	private LocalDateTime timeStamp;
	/**
	 * Data in BASE64 encoding to encrypt/decrypt
	 */

	@NotBlank(message = "should not be null or empty")
	private String data;

	/**
	 * salt in BASE64 encoding for encrypt/decrypt
	 */

	@NotBlank(message = "should not be null or empty")
	private String salt;

	/**
	 * aad in BASE64 encoding for encrypt/decrypt
	 */

	@NotBlank(message = "should not be null or empty")
	private String aad;
}
