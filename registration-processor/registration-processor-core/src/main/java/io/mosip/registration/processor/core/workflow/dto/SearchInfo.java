package io.mosip.registration.processor.core.workflow.dto;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dto to hold the search criteria.
 * 
 * @author Abhishek Kumar
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchInfo {

	@NotNull
	@Valid
	private List<FilterInfo> filters;

	@NotNull
	private SortInfo sort;

	// @NotNull
	private PaginationInfo pagination;

	// private String languageCode;
}
