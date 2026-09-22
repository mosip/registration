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

	/**
	 * Builds the anonymous profile JSON from packet data.
	 *
	 * <p>All profile fields are read from the packet except supervisorDecision and
	 * supervisorComment: the supervisor takes that decision on the registration
	 * client and it reaches the registration_list table on sync, so those two are
	 * looked up by {@code workflowInstanceId} - the same unique key
	 * {@code SupervisorApprovalStatusTagGenerator} uses.
	 *
	 * <p>That lookup is best-effort enrichment. A blank {@code workflowInstanceId},
	 * a missing registration_list record (the normal case at classification time,
	 * before the supervisor decision has synced) or a failed read leaves both
	 * fields null; the profile is still built and returned in full. Supervisor
	 * reporting must never cost the profile itself.
	 *
	 * @param workflowInstanceId key used to look up the supervisor decision; may be
	 *                           null or blank, in which case no lookup is attempted
	 *                           and both supervisor fields stay null
	 */
	public String buildJsonStringFromPacketInfo(BiometricRecord biometricRecord, Map<String, String> fieldMap,
			Map<String, String> fieldTypeMap, Map<String, String> metaInfoMap, String statusCode, String processStage,
			String workflowInstanceId)
			throws JSONException, ApisResourceAccessException, PacketManagerException, IOException, BaseCheckedException;
}
