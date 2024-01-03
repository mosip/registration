package io.mosip.registration.processor.packet.storage.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.code.AbisStatusCode;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.entity.RegistrationStatusEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.constant.AbisConstant;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.abis.AbisRequestDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisResponseDetDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisResponseDto;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.storage.dao.PacketInfoDao;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.SyncTypeDto;

/**
 * The Class ABISHandlerUtil.
 * 
 * @author Nagalakshmi
 * @author Horteppa
 */
@Component
public class ABISHandlerUtil {
	
	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(ABISHandlerUtil.class);

	/** The utilities. */
	@Autowired
	Utilities utilities;

	/** The packet info manager. */
	@Autowired
	private PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;

	/** The packet info dao. */
	@Autowired
	private PacketInfoDao packetInfoDao;

	@Autowired
	private IdRepoService idRepoService;

	/**
	 * Gets the unique reg ids.
	 *
	 * @param registrationId   the registration id
	 * @param registrationType the registration type
	 * @return the unique reg ids
	 * @throws ApisResourceAccessException           the apis resource access
	 *                                               exception
	 * @throws IOException                           Signals that an I/O exception
	 *                                               has occurred.
	 * @throws                                       io.mosip.kernel.core.exception.IOException
	 */
	public Set<String> getUniqueRegIds(String registrationId, String registrationType,
										int iteration, String workflowInstanceId, ProviderStageName stageName) throws ApisResourceAccessException, JsonProcessingException, PacketManagerException, IOException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationId, "ABISHandlerUtil::getUniqueRegIds()::entry");
		
		String latestTransactionId = utilities.getLatestTransactionId(registrationId, registrationType, iteration, workflowInstanceId);

		List<String> regBioRefIds = packetInfoDao.getAbisRefIdByWorkflowInstanceId(workflowInstanceId);

		List<String> machedRefIds = new ArrayList<>();
		Set<String> uniqueRIDs = new HashSet<>();
		List<AbisResponseDetDto> abisResponseDetDtoList = new ArrayList<>();

		if (!regBioRefIds.isEmpty()) {
			List<AbisResponseDto> abisResponseDtoList = packetInfoManager.getAbisResponseRecords(regBioRefIds.get(0),
					latestTransactionId, AbisConstant.IDENTIFY);
			for (AbisResponseDto abisResponseDto : abisResponseDtoList) {
				abisResponseDetDtoList.addAll(packetInfoManager.getAbisResponseDetails(abisResponseDto.getId()));
			}
			if (!abisResponseDetDtoList.isEmpty()) {
				for (AbisResponseDetDto abisResponseDetDto : abisResponseDetDtoList) {
					machedRefIds.add(abisResponseDetDto.getMatchedBioRefId());
				}
				if (!CollectionUtils.isEmpty(machedRefIds)) {
					List<String> matchedRegIds = packetInfoDao.getAbisRefRegIdsByMatchedRefIds(machedRefIds);
					if (!CollectionUtils.isEmpty(matchedRegIds)) {
						List<RegistrationStatusEntity> matchedRegistrationStatusEntities = packetInfoDao
								.getWithoutStatusCode(matchedRegIds,
										RegistrationStatusCode.REJECTED.toString());
						List<RegistrationStatusEntity> processingRegistrationStatusEntities = matchedRegistrationStatusEntities
								.stream()
								.filter(e -> RegistrationStatusCode.PROCESSING.toString().equals(e.getStatusCode()))
								.collect(Collectors.toList());
						List<String> processingRegIds = processingRegistrationStatusEntities.stream()
								.map(RegistrationStatusEntity::getRegId)
								.collect(Collectors.toList());
						List<String> matchedProcessedRegIds = matchedRegistrationStatusEntities.stream()
								.map(RegistrationStatusEntity::getRegId).collect(Collectors.toList());
						uniqueRIDs.addAll(processingRegIds);
						Set<String> processedRegIds = getUniqueRegIds(matchedProcessedRegIds, registrationId,
								registrationType,
								stageName);
						for(String rid:processedRegIds) {
							if(!uniqueRIDs.contains(rid))
								uniqueRIDs.add(rid);
						}
					}
				}
			}
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationId, "ABISHandlerUtil::getUniqueRegIds()::exit");
		
		return uniqueRIDs;

	}

	/**
	 * Gets the packet status.
	 *
	 * @param registrationStatusDto
	 *            the registration status dto
	 * @return the packet status
	 */
	public String getPacketStatus(InternalRegistrationStatusDto registrationStatusDto) {
		// get all identify requests for latest transaction id
		List<AbisRequestDto> identifyRequests = getAllIdentifyRequest(registrationStatusDto.getRegistrationId(),
				registrationStatusDto.getRegistrationType(), registrationStatusDto.getIteration(), registrationStatusDto.getWorkflowInstanceId());

		// if there are no identify requests present
		if (CollectionUtils.isEmpty(identifyRequests))
			return AbisConstant.PRE_ABIS_IDENTIFICATION;
		// if there are unprocessed pending identify request for same transaction id then consider it as duplicate
		else if (isIdentifyRequestsPendingForLatestTransactionId(identifyRequests))
			return AbisConstant.DUPLICATE_FOR_SAME_TRANSACTION_ID;
		// else if all the identify requests processed for latest transaction id
		else
			return AbisConstant.POST_ABIS_IDENTIFICATION;
	}

	/**
	 * This method returns all identify requests
	 *
	 * @param registrationId
	 *            the registration id
	 * @return the matched reg ids
	 */
	private List<AbisRequestDto> getAllIdentifyRequest(String registrationId, String process, int iteration, String workflowInstanceId) {
		String latestTransactionId = utilities.getLatestTransactionId(registrationId, process, iteration, workflowInstanceId);

		List<String> regBioRefIds = packetInfoDao.getAbisRefIdByWorkflowInstanceId(workflowInstanceId);

		if (!regBioRefIds.isEmpty()) {
			List<AbisRequestDto> abisRequestDtoList = packetInfoManager.getInsertOrIdentifyRequest(regBioRefIds.get(0), latestTransactionId);
			if (!CollectionUtils.isEmpty(abisRequestDtoList)) {
				return abisRequestDtoList.stream().filter(reqDto ->
						reqDto.getRequestType().equalsIgnoreCase(AbisStatusCode.IDENTIFY.toString())).collect(Collectors.toList());
			}
		}

		return null;
	}

	/**
	 * This method returns all unprocessed identify requests
	 *
	 * @param identifyRequests
	 * @return
	 */
	private boolean isIdentifyRequestsPendingForLatestTransactionId(List<AbisRequestDto> identifyRequests) {
		// check if any of the identify request is not processed for same transaction id
		return identifyRequests.stream().filter(
				identifyReq -> !identifyReq.getStatusCode().equalsIgnoreCase(AbisStatusCode.PROCESSED.toString()))
				.findAny().isPresent();
	}

	/**
	 * Gets the unique reg ids.
	 *
	 * @param matchedRegistrationIds the matched registration ids
	 * @param registrationId         the registration id
	 * @param registrationType       the registration type
	 * @return the unique reg ids
	 * @throws ApisResourceAccessException           the apis resource access
	 *                                               exception
	 * @throws IOException                           Signals that an I/O exception
	 *                                               has occurred.
	 * @throws                                       io.mosip.kernel.core.exception.IOException
	 */
	public Set<String> getUniqueRegIds(List<String> matchedRegistrationIds, String registrationId,
										String registrationType, ProviderStageName stageName) throws ApisResourceAccessException, IOException,
			JsonProcessingException, PacketManagerException {

		Map<String, String> filteredRegMap = new LinkedHashMap<>();
		Set<String> filteredRIds = new HashSet<>();

		for (String machedRegId : matchedRegistrationIds) {

			String matchedUin = idRepoService.getUinByRid(machedRegId,
					utilities.getGetRegProcessorDemographicIdentity());

			if (registrationType.equalsIgnoreCase(SyncTypeDto.UPDATE.toString())) {
				String packetUin = utilities.getUIn(registrationId, registrationType, stageName);
				if (matchedUin != null && !packetUin.equals(matchedUin)) {
					filteredRegMap.put(matchedUin, machedRegId);
				}
			}
			if (registrationType.equalsIgnoreCase(SyncTypeDto.NEW.toString()) && matchedUin != null) {
				filteredRegMap.put(matchedUin, machedRegId);
			}

			if (registrationType.equalsIgnoreCase(SyncTypeDto.LOST.toString()) && matchedUin != null) {
				filteredRegMap.put(matchedUin, machedRegId);
			}

		}
		if (!filteredRegMap.isEmpty()) {
			filteredRIds = new HashSet<String>(filteredRegMap.values());
		}

		return filteredRIds;

	}

}
