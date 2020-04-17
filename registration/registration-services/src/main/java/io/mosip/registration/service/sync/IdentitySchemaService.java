package io.mosip.registration.service.sync;

import io.mosip.registration.dto.ResponseDTO;

/**
 * It makes call to the external 'Identity Sync' services to download the
 * identity schema data. Once download the data, it stores the information into
 * the DB for further processing. During the process, the required informations
 * are updated into the audit table for further tracking.
 * 
 * @author Yaswanth S
 * @since 1.0.0
 *
 */
public interface IdentitySchemaService {

	/**
	 * Download the identity Schema json and save in Local DB
	 * 
	 * @return resposnse of download success or failure
	 */
	public ResponseDTO downloadIdentitySchema();

	/**
	 * Save identity Schema json in Local DB
	 * 
	 * @param identitySchemaJson
	 *            is a string which to saved in local DB
	 */
	public void saveInLocalDB(String identitySchemaJson);

	/**
	 * Get value for the attribute
	 * @param attributeKey
	 *            for which value to be retrieved
	 * @return value of the attribute
	 */
	public String getValFromIdentityJsonSchema(String attributeKey);
}
