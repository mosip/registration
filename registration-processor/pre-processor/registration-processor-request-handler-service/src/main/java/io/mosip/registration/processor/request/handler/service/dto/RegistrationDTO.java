package io.mosip.registration.processor.request.handler.service.dto;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import io.mosip.registration.processor.request.handler.service.dto.demographic.DemographicDTO;
import lombok.Data;

/**
 * This class contains the Registration details.
 * 
 * @author Dinesh Asokan
 * @author Balaji Sridharan
 * @since 1.0.0
 *
 */
@Data
public class RegistrationDTO extends BaseDTO implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5931095944645820246L;
	private DemographicDTO demographicDTO;
	private String registrationId;
	private String registrationIdHash;

	private Map<String, String> metadata;

	//added for resident update service
	private String regType;

}
