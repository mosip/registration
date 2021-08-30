package io.mosip.registration.processor.core.idrepo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentsDTO {

	/** The doc type. */
	private String category;

	/** The doc value. */
	private String value;
}
