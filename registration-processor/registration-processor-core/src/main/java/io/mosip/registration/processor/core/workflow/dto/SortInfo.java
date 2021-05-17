package io.mosip.registration.processor.core.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * dto to hold the Sort criteria.
 * 
 * @author Abhishek Kumar
 * @since 1.0
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SortInfo {

	private String sortField;

	private String sortType;
}
