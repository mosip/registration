package io.mosip.registration.processor.packet.storage.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.code.AbisStatusCode;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.constant.AbisConstant;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.PolicyConstant;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.BiometricRecordValidationException;
import io.mosip.registration.processor.core.exception.DataShareException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.abis.AbisRequestDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisResponseDetDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisResponseDto;
import io.mosip.registration.processor.core.packet.dto.abis.Filter;
import io.mosip.registration.processor.core.packet.dto.abis.ShareableAttributes;
import io.mosip.registration.processor.core.packet.dto.abis.Source;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.storage.dao.PacketInfoDao;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.SyncTypeDto;
import io.mosip.registration.processor.status.entity.RegistrationStatusEntity;

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

	@Value("${registration.processor.policy.id}")
	private String policyId;

	@Value("${registration.processor.subscriber.id}")
	private String subscriberId;

	@Autowired
	private RegistrationProcessorRestClientService registrationProcessorRestClientService;

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
	public List<String> getUniqueRegIds(String registrationId, String registrationType, ProviderStageName stageName) throws ApisResourceAccessException, JsonProcessingException, PacketManagerException, IOException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationId, "ABISHandlerUtil::getUniqueRegIds()::entry");
		
		String latestTransactionId = utilities.getLatestTransactionId(registrationId);

		List<String> regBioRefIds = packetInfoDao.getAbisRefMatchedRefIdByRid(registrationId);

		List<String> machedRefIds = new ArrayList<>();
		List<String> uniqueRIDs = new ArrayList<>();
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
						uniqueRIDs = getUniqueProcessedRecords(registrationId, registrationType, stageName,
								matchedRegIds);

					}
				}
			}
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationId, "ABISHandlerUtil::getUniqueRegIds()::exit");
		
		return uniqueRIDs;

	}

	public List<String> getUniqueProcessedRecords(String registrationId, String registrationType,
			ProviderStageName stageName,
			List<String> matchedRegIds)
			throws ApisResourceAccessException, IOException, JsonProcessingException, PacketManagerException {
		List<String> uniqueRIDs = new ArrayList<>();
		List<RegistrationStatusEntity> matchedRegistrationStatusEntities = packetInfoDao
				.getWithoutStatusCode(matchedRegIds,
				RegistrationStatusCode.REJECTED.toString());
		List<RegistrationStatusEntity> processingRegistrationStatusEntities = matchedRegistrationStatusEntities
				.stream()
				.filter(e -> RegistrationStatusCode.PROCESSING.toString().equals(e.getStatusCode()))
				.collect(Collectors.toList());
		List<String> processingRegIds = processingRegistrationStatusEntities.stream()
				.map(RegistrationStatusEntity::getId)
				.collect(Collectors.toList());
		List<String> matchedProcessedRegIds = matchedRegistrationStatusEntities.stream()
				.map(RegistrationStatusEntity::getId).collect(Collectors.toList());
		uniqueRIDs.addAll(processingRegIds);
		List<String> processedRegIds = getUniqueRegIds(matchedProcessedRegIds, registrationId,
				registrationType,
				stageName);
		for(String rid:processedRegIds) {
			if(!uniqueRIDs.contains(rid))
				uniqueRIDs.add(rid);
		}
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
		List<AbisRequestDto> identifyRequests = getAllIdentifyRequest(registrationStatusDto.getRegistrationId());

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
	private List<AbisRequestDto> getAllIdentifyRequest(String registrationId) {
		String latestTransactionId = utilities.getLatestTransactionId(registrationId);

		List<String> regBioRefIds = packetInfoDao.getAbisRefMatchedRefIdByRid(registrationId);

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
	public List<String> getUniqueRegIds(List<String> matchedRegistrationIds, String registrationId,
										String registrationType, ProviderStageName stageName) throws ApisResourceAccessException, IOException,
			JsonProcessingException, PacketManagerException {

		Map<String, String> filteredRegMap = new LinkedHashMap<>();
		List<String> filteredRIds = new ArrayList<>();

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
			filteredRIds = new ArrayList<>(filteredRegMap.values());
		}

		return filteredRIds;

	}

	public void validateBiometricRecord(BiometricRecord biometricRecord, List<String> modalities)
			throws BiometricRecordValidationException, JsonParseException, JsonMappingException, IOException {
		if (modalities == null || modalities.isEmpty()) {
			throw new BiometricRecordValidationException(PlatformErrorMessages.RPR_DATASHARE_MODALITIES_EMPTY.getCode(),
					PlatformErrorMessages.RPR_DATASHARE_MODALITIES_EMPTY.getMessage());
		}
		if (biometricRecord == null || biometricRecord.getSegments() == null
				|| biometricRecord.getSegments().isEmpty()) {
			throw new BiometricRecordValidationException(
					PlatformErrorMessages.RPR_NO_BIOMETRICS_FOUND_WITH_DATASHARE.getCode(),
					PlatformErrorMessages.RPR_NO_BIOMETRICS_FOUND_WITH_DATASHARE.getMessage());
		}

		for (String segment : modalities) {
			Optional<BIR> optionalBIR = Optional.empty();
			if (segment.equalsIgnoreCase("Face")) {
				optionalBIR = biometricRecord.getSegments().stream().filter(bir -> bir.getBdbInfo().getType() != null
						&& bir.getBdbInfo().getType().get(0).equals(BiometricType.FACE)).findFirst();
			} else {
				String[] segmentArray = segment.split(" ");
				optionalBIR = biometricRecord.getSegments().stream().filter(bir -> bir.getBdbInfo().getSubtype() != null
						&& bir.getBdbInfo().getSubtype().size() == segmentArray.length
								? (bir.getBdbInfo().getSubtype().get(0).equalsIgnoreCase(segmentArray[0])
										&& (segmentArray.length == 2
												? bir.getBdbInfo().getSubtype().get(1).equalsIgnoreCase(segmentArray[1])
												: true))
								: false)
						.findFirst();
			}
			if (optionalBIR.isPresent()) {
				BIR bir = optionalBIR.get();
				if (bir.getBdb() != null) {
					return;
				}
			}
		}

		throw new BiometricRecordValidationException(
				PlatformErrorMessages.RPR_NO_BIOMETRIC_MATCH_WTIH_DATASAHRE.getCode(),
				PlatformErrorMessages.RPR_NO_BIOMETRIC_MATCH_WTIH_DATASAHRE.getMessage());
	}

	public Map<String, List<String>> createBiometricTypeSubtypeMappingFromAbispolicy() throws ApisResourceAccessException, DataShareException,
			JsonParseException, JsonMappingException, com.fasterxml.jackson.core.JsonProcessingException, IOException {
		Map<String, List<String>> typeAndSubTypeMap = new HashMap<>();
		ResponseWrapper<?> policyResponse = (ResponseWrapper<?>) registrationProcessorRestClientService.getApi(
				ApiName.PMS, Lists.newArrayList(policyId, PolicyConstant.PARTNER_ID, subscriberId), "", "",
				ResponseWrapper.class);
		if (policyResponse == null || (policyResponse.getErrors() != null && policyResponse.getErrors().size() > 0)) {
			throw new DataShareException(policyResponse == null ? "Policy Response response is null"
					: policyResponse.getErrors().get(0).getMessage());

		} else {
			LinkedHashMap<String, Object> responseMap = (LinkedHashMap<String, Object>) policyResponse.getResponse();
			LinkedHashMap<String, Object> policies = (LinkedHashMap<String, Object>) responseMap
					.get(PolicyConstant.POLICIES);
			List<?> attributes = (List<?>) policies.get(PolicyConstant.SHAREABLE_ATTRIBUTES);
			ObjectMapper mapper = new ObjectMapper();
			ShareableAttributes shareableAttributes = mapper.readValue(mapper.writeValueAsString(attributes.get(0)),
					ShareableAttributes.class);
			for (Source source : shareableAttributes.getSource()) {
				List<Filter> filterList = source.getFilter();
				if (filterList != null && !filterList.isEmpty()) {

					filterList.forEach(filter -> {
						if (filter.getSubType() != null && !filter.getSubType().isEmpty()) {
							typeAndSubTypeMap.put(filter.getType(), filter.getSubType());
						} else {
							typeAndSubTypeMap.put(filter.getType(), null);
						}
					});
				}
			}
		}
		return typeAndSubTypeMap;

	}

	public List<String> removeRejectedIds(List<String> matchedRegIds) {
		List<String> matchedRidsWithoutRejected = new ArrayList<>();
		List<RegistrationStatusEntity> matchedRegistrationStatusEntities = packetInfoDao
				.getWithoutStatusCode(matchedRegIds, RegistrationStatusCode.REJECTED.toString());
		matchedRidsWithoutRejected.addAll((matchedRegistrationStatusEntities).stream()
				.map(RegistrationStatusEntity::getId).collect(Collectors.toList()));
		return matchedRidsWithoutRejected;
	}
}
