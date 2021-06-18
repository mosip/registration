package io.mosip.registration.processor.status.dto;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import io.mosip.registration.processor.core.workflow.dto.SortInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 
 * @author Dhanendra
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchInfo {

	@NotNull
	@Valid
	private List<FilterInfo> filters;

	@NotNull
	private List<SortInfo> sort;

}
