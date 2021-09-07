package io.mosip.registration.processor.stages.demodedupe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.abis.RegBioRefDto;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.demographicinfo.DemographicInfoDto;
import io.mosip.registration.processor.packet.storage.dao.PacketInfoDao;
import io.mosip.registration.processor.stages.app.constants.DemoDedupeConstants;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

/**
 * The Class DemoDedupe.
 *
 * @author M1048358 Alok Ranjan
 * @author M1048860 Kiran Raj
 */
@Component
public class DemoDedupe {

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(DemoDedupe.class);

	/** The env. */
	@Autowired
	private Environment env;

	@Autowired
	private RegistrationStatusService registrationStatusService;

	/** The packet info dao. */
	@Autowired
	private PacketInfoDao packetInfoDao;

	/** The packet info manager. */
	@Autowired
	private PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;

	/**
	 * Perform dedupe.
	 *
	 * @param refId
	 *            the ref id
	 * @return the list
	 */
	public List<DemographicInfoDto> performDedupe(String refId) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REFFERENCEID.toString(), refId,
				"DemoDedupe::performDedupe()::entry");

		List<DemographicInfoDto> applicantDemoDto = packetInfoDao.findDemoById(refId);
		List<DemographicInfoDto> matchedIdsWithUin;
		List<DemographicInfoDto> demographicInfoDtos;
		Set<DemographicInfoDto> infoDtos = new HashSet<>();
		for (DemographicInfoDto demoDto : applicantDemoDto) {
			infoDtos.addAll(packetInfoDao.getAllDemographicInfoDtos(demoDto.getName(), demoDto.getGenderCode(),
					demoDto.getDob(), demoDto.getLangCode()));
		}
		matchedIdsWithUin = getAllDemographicInfoDtosWithUin(infoDtos);
		demographicInfoDtos = filterByRefIdAvailability(matchedIdsWithUin);

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REFFERENCEID.toString(), refId,
				"DemoDedupe::performDedupe()::exit");
		return demographicInfoDtos;
	}

	private List<DemographicInfoDto> getAllDemographicInfoDtosWithUin(
			Set<DemographicInfoDto> duplicateDemographicDtos) {
		List<DemographicInfoDto> demographicInfoDtosWithUin = new ArrayList<>();
		for (DemographicInfoDto demographicDto : duplicateDemographicDtos) {
			if (registrationStatusService.checkUinAvailabilityForRid(demographicDto.getRegId())) {
				demographicInfoDtosWithUin.add(demographicDto);
			}

		}
		return demographicInfoDtosWithUin;
	}

	private List<DemographicInfoDto> filterByRefIdAvailability(List<DemographicInfoDto> duplicateDemographicDtos) {
		List<DemographicInfoDto> demographicInfoDtosWithRefId = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(duplicateDemographicDtos)) {
			List<String> regIds = duplicateDemographicDtos.stream().map(dto -> dto.getRegId()).collect(Collectors.toList());
			List<RegBioRefDto> finalRegBioRefDtos = packetInfoManager.getBioRefIdsByRegIds(regIds);
			if (CollectionUtils.isNotEmpty(finalRegBioRefDtos)) {
				List<String> finalRegIds = finalRegBioRefDtos.stream().map(dto -> dto.getRegId()).collect(Collectors.toList());
				demographicInfoDtosWithRefId.addAll(duplicateDemographicDtos.stream().filter(
						dto -> finalRegIds.contains(dto.getRegId())).collect(Collectors.toList()));
			}
		}
		return demographicInfoDtosWithRefId;
	}


}
