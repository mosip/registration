package io.mosip.registration.processor.packet.utility.dto;

import java.time.LocalDateTime;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.mosip.registration.processor.packet.utility.constants.CryptomanagerConstant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 
 * @author Sowmya
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor

public class CryptomanagerRequestDto {
	/**
	 * Application id of decrypting module
	 */
	
	@NotBlank(message = io.mosip.registration.processor.packet.utility.constants.CryptomanagerConstant.INVALID_REQUEST)
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
	
	@NotBlank(message = CryptomanagerConstant.INVALID_REQUEST)
	private String data;
}
