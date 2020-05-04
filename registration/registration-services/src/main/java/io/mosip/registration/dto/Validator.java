package io.mosip.registration.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * Validator class will be having information of what type of validator it is,
 * and validator value, and required arguments also
 * 
 * @author YASWANTH S
 *
 */
@Setter
@Getter
public class Validator {

	/**
	 * Type of the validator
	 */
	private String type;
	/**
	 * Validator value
	 */
	private String validator;
	/**
	 * Arguments if required
	 */
	private List<String> arguments;
}
