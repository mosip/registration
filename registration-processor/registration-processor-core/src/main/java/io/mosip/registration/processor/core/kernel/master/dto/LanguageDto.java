package io.mosip.registration.processor.core.kernel.master.dto;

import lombok.Data;

@Data
public class LanguageDto {
	/**
	 * Field for language code
	 */
	private String code;

	/**
	 * Field for language name
	 */
	private String name;

	/**
	 * Field for language family
	 */
	private String family;

	/**
	 * Field for language native name
	 */
	private String nativeName;

	/**
	 * Field for the status of data.
	 */
	private Boolean isActive;
}
