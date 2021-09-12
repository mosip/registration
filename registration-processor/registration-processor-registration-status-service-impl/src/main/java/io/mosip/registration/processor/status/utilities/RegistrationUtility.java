package io.mosip.registration.processor.status.utilities;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * The Class RegistrationUtility.
 *
 * @author M1048219
 */
@Component
public class RegistrationUtility {

	private String mappingJson = null;

	/** The config server file storage URL. */
	@Value("${config.server.file.storage.uri}")
	private String configServerFileStorageURL;

	/** The get reg processor identity json. */
	@Value("${registration.processor.identityjson}")
	private String getRegProcessorIdentityJson;

	/**
	 * Generate id.
	 *
	 * @return the string
	 */
	public static String generateId() {
		return UUID.randomUUID().toString();
	}

	public String getMappingJson() {
		if (mappingJson == null) {
			RestTemplate restTemplate = new RestTemplate();
			mappingJson =restTemplate.getForObject(configServerFileStorageURL + getRegProcessorIdentityJson,
					String.class);
		}
		return mappingJson;
	}

}
