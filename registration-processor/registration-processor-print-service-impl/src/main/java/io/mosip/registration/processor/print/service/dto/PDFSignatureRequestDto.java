package io.mosip.registration.processor.print.service.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Crypto-Manager-Request model
 * 
 * @author Urvil Joshi
 *
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "Model representing a PDF sign request")
public class PDFSignatureRequestDto extends SignatureRequestDto {
	/**
	 * The lower left x value of sign rectangle.
	 */

	private int lowerLeftX; 
	
	/**
	 * The lower left y value of sign rectangle.
	 */

	private int lowerLeftY; 
	
	/**
	 * The upper right x value of sign rectangle.
	 */

	private int upperRightX; 
	
	/**
	 *  The upper right y value of sign rectangle.
	 */

	private int upperRightY;
	
	/**
	 *  Reason for signing.
	 */

	private String reason;
	
	/**
	 * Page number for signature.
	 */

	private int pageNumber;
	
	/**
	 * Password for protecting PDF
	 */

	private String password;
}

