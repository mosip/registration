package io.mosip.registration.service.sync.impl;

import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.sync.IdentitySchemaService;

public class IdentitySchemaServiceImpl extends BaseService implements IdentitySchemaService {

	@Override
	public ResponseDTO downloadIdentitySchema() {
		ResponseDTO responseDTO = new ResponseDTO();

		// TODO make Request to be sent

		// TODO Rest Call to and get the response

		// If response scuccess and saved in local DB
		// setSuccessResponse(responseDTO, RegistrationConstants.SUCCESS, null);

		// Else
		// setErrorResponse(responseDTO, RegistrationConstants.FAILURE, null);

		return responseDTO;
	}

	@Override
	public void saveInLocalDB(String identitySchemaJson) {
		// TODO Prepare Enity to be saved inside the db & Save

	}

	@Override
	public String getValFromIdentityJsonSchema(String attributeKey) {

		String val;
		// TODO get JSON from DB

		// TODO get By Attribute
		val = "VAL";
		return val;
	}

}
