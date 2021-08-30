package io.mosip.registration.processor.core.idrepo.dto;

import java.util.List;


import lombok.Data;

@Data
public class BaseRequestResponseDTO1 {

	/** The status. */
	private String status;

	/** The identity. */
	private Object identity;
	
	private AnonymousProfileDTO anonymousProfile;

	/** The documents. */
	private List<DocumentsDTO> documents;
}