package io.mosip.registration.processor.packet.storage.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
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
	public List<String> getUniqueRegIds(String registrationId, String registrationType)
			throws ApisResourceAccessException, IOException, io.mosip.kernel.core.exception.IOException, JsonProcessingException, PacketManagerException {
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
						List<String> processingRegIds = packetInfoDao.getProcessedOrProcessingRegIds(matchedRegIds,
								RegistrationTransactionStatusCode.IN_PROGRESS.toString());
						List<String> processedRegIds = packetInfoDao.getProcessedOrProcessingRegIds(matchedRegIds,
								RegistrationTransactionStatusCode.PROCESSED.toString());
						uniqueRIDs = getUniqueRegIds(processedRegIds, registrationId, registrationType);
						uniqueRIDs.addAll(processingRegIds);
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
		if (getMatchedRegIds(registrationStatusDto.getRegistrationId()).isEmpty()) {
			return AbisConstant.PRE_ABIS_IDENTIFICATION;
		}
		return AbisConstant.POST_ABIS_IDENTIFICATION;
	}

	/**
	 * Gets the matched reg ids.
	 *
	 * @param registrationId
	 *            the registration id
	 * @return the matched reg ids
	 */
	private List<AbisRequestDto> getMatchedRegIds(String registrationId) {
		String latestTransactionId = utilities.getLatestTransactionId(registrationId);

		List<String> regBioRefIds = packetInfoDao.getAbisRefMatchedRefIdByRid(registrationId);

		List<AbisRequestDto> abisRequestDtoList = new ArrayList<>();

		if (!regBioRefIds.isEmpty()) {
			abisRequestDtoList = packetInfoManager.getInsertOrIdentifyRequest(regBioRefIds.get(0), latestTransactionId);
		}

		return abisRequestDtoList;
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
	private List<String> getUniqueRegIds(List<String> matchedRegistrationIds, String registrationId,
			String registrationType) throws ApisResourceAccessException, IOException,
			JsonProcessingException, PacketManagerException {

		Map<String, String> filteredRegMap = new LinkedHashMap<>();
		List<String> filteredRIds = new ArrayList<>();

		for (String machedRegId : matchedRegistrationIds) {

			String matchedUin = idRepoService.getUinByRid(machedRegId,
					utilities.getGetRegProcessorDemographicIdentity());

			if (registrationType.equalsIgnoreCase(SyncTypeDto.UPDATE.toString())) {
				String packetUin = utilities.getUIn(registrationId, registrationType);
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
			if (!filteredRegMap.isEmpty()) {
				filteredRIds = new ArrayList<>(filteredRegMap.values());
			}

		}

		return filteredRIds;

	}

}
