package io.mosip.registration.processor.status.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dtp to hold the search parameters
 * 
 * @author Abhishek Kumar
 * @since 1.0.0
 */

@Data
@AllArgsConstructor()
@NoArgsConstructor
public class FilterInfo {

	private String value;

	private String fromValue;

	private String toValue;

	private String columnName;

	private String type;
}
