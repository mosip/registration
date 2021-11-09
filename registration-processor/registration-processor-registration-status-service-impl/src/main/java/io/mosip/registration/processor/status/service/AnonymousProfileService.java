package io.mosip.registration.processor.status.service;

import java.io.IOException;
import java.util.Map;

import org.json.JSONException;
import org.springframework.stereotype.Service;

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;

@Service
public interface AnonymousProfileService {

	/**
	 * save anonymous profile
	 * @param id
	 * @param processStage
	 * @param profileJson
	 */
	public void saveAnonymousProfile(String regId, String processStage,String profileJson);

	public String buildJsonStringFromPacketInfo(BiometricRecord biometricRecord, Map<String, String> fieldMap,
			Map<String, String> fieldTypeMap, Map<String, String> metaInfoMap, String statusCode, String processStage)
			throws JSONException, ApisResourceAccessException, PacketManagerException, IOException, BaseCheckedException;
}
