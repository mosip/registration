package io.mosip.registration.processor.status.service;

import java.util.List;

import org.springframework.stereotype.Service;

import io.mosip.registration.processor.status.dto.LostRidDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusSubRequestDto;
import io.mosip.registration.processor.status.dto.RegistrationSyncRequestDTO;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SearchInfo;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;

/**
 * The Interface SyncRegistrationService.
 *
 * @author M1047487
 * @param <T>
 *            the generic type
 * @param <U>
 *            the generic type
 */
@Service
public interface SyncRegistrationService<T, U> {

	/**
	 * Sync.
	 *
	 * @param syncResgistrationdto
	 *            the sync resgistrationdto
	 * @return the list
	 */
	public List<T> sync(List<U> syncResgistrationdto, String referenceId, String timeStamp);

	/**
	 * Checks if is present.
	 *
	 * @param resgistrationId
	 *            the sync registration id
	 * @return true, if is present
	 */
	public boolean isPresent(String resgistrationId);

	/**
	 * Find by registration id.
	 *
	 * @param resgistrationId
	 *            the resgistration id
	 * @return the sync registration entity
	 */
	public List<SyncRegistrationEntity> findByRegistrationId(String resgistrationId);

	public SyncRegistrationEntity findByWorkflowInstanceId(String workflowInstanceId);

	/**
	 * Find by registration id and additional info req id.
	 * @param registrationId
	 * @param additionalInfoReqId
	 * @return sync registration entity
	 */
	public SyncRegistrationEntity findByRegistrationIdAndAdditionalInfoReqId(String registrationId, String additionalInfoReqId);

	public List<SyncRegistrationEntity> findByAdditionalInfoReqId(String additionalInfoReqId);

	public SyncRegistrationEntity findByPacketId(String packetId);

	/**
	 * Decrypt and get sync request.
	 *
	 * @param encryptedSyncMetaInfo
	 *            the encrypted sync meta info
	 * @param referenceId
	 *            the reference id
	 * @param timeStamp
	 *            the time stamp
	 * @param syncResponseList
	 *            the sync response list
	 * @return the registration sync request DTO
	 */
	public RegistrationSyncRequestDTO decryptAndGetSyncRequest(Object encryptedSyncMetaInfo, String referenceId,
			String timeStamp, List<SyncResponseDto> syncResponseList);

	/**
	 * Gets the by ids.
	 *
	 * @param requestIds
	 *            the request ids
	 * @return the by ids
	 */
	public List<RegistrationStatusDto> getByIds(List<RegistrationStatusSubRequestDto> requestIds);
	
	/**
	 * Gets the external status by ids.
	 *
	 * @param requestIds
	 *            the request ids
	 * @return the by ids
	 */
	public List<RegistrationStatusDto> getExternalStatusByIds(List<String> requestIds);

	/**
	 * Delete additional info by registration id.
	 *
	 * @param syncEntity
	 *            the sync Entity
	 * @return true / false
	 */
	public boolean deleteAdditionalInfo(SyncRegistrationEntity syncEntity);

	public List<SyncResponseDto> syncV2(List<SyncRegistrationDto> request, String referenceId, String timeStamp);

	public List<SyncRegistrationEntity> getByPacketIds(List<String> packetIdList);

	public List<LostRidDto> searchLostRid(SearchInfo searchInfo);

}
