package io.mosip.registration.processor.packet.manager.dto;

import java.util.List;

import io.mosip.registration.processor.core.idrepo.dto.Documents;
import lombok.Data;

/**
 * The Class ResponseDTO.
 *
 * @author M1049387
 */
@Data
public class ResponseDTO {
	
	/** The entity. */
	private Object anonymousProfile;

	private String biometricReferenceId;

	/** The identity. */
	private Object identity;

	private List<Documents> documents;

	private String registrationId;

	/** The status. */
	private String status;

	private String uin;
}
