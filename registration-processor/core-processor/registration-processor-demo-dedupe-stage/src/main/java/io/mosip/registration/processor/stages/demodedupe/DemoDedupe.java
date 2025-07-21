package io.mosip.registration.processor.stages.demodedupe;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.mosip.registration.processor.status.dao.RegistrationStatusDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.demographicinfo.DemographicInfoDto;
import io.mosip.registration.processor.packet.storage.dao.PacketInfoDao;
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

	@Autowired
	private RegistrationStatusDao registrationStatusDao;

	/**
	 * Perform dedupe.
	 *
	 * @param refId
	 *            the ref id
	 * @return the list
	 */
	/*public List<DemographicInfoDto> performDedupe(String refId) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REFERENCEID.toString(), refId,
				"DemoDedupe::performDedupe()::entry");

		List<DemographicInfoDto> applicantDemoDto = packetInfoDao.findDemoById(refId);
		List<DemographicInfoDto> demographicInfoDtos;
		List<DemographicInfoDto> infoDtos = new ArrayList<>();
		for (DemographicInfoDto demoDto : applicantDemoDto) {
			infoDtos.addAll(packetInfoDao.getAllDemographicInfoDtos(demoDto.getName(), demoDto.getGenderCode(),
					demoDto.getDob(), demoDto.getLangCode()));
		}
		demographicInfoDtos = getAllDemographicInfoDtosWithUin(infoDtos);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REFERENCEID.toString(), refId,
				"DemoDedupe::performDedupe()::exit");
		return demographicInfoDtos;
	}*/
	public List<DemographicInfoDto> performDedupe(String refId) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REFERENCEID.toString(), refId,
				"DemoDedupe::performDedupe()::entry");

		List<DemographicInfoDto> applicantDemoDto = packetInfoDao.findDemoById(refId);

		// Collect all unique parameter sets
		List<PacketInfoDao.NameGenderDobLangCode> params = applicantDemoDto.stream()
				.map(dto -> new PacketInfoDao.NameGenderDobLangCode(dto.getName(), dto.getGenderCode(), dto.getDob(), dto.getLangCode()))
				.distinct()
				.collect(Collectors.toList());

		// Batch query for all demographic infos matching any of the parameter sets
		List<DemographicInfoDto> infoDtos = packetInfoDao.getAllDemographicInfoDtosBatch(params);

		List<DemographicInfoDto> demographicInfoDtos = getAllDemographicInfoDtosWithUin(infoDtos);

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REFERENCEID.toString(), refId,
				"DemoDedupe::performDedupe()::exit");
		return demographicInfoDtos;
	}

	private List<DemographicInfoDto> getAllDemographicInfoDtosWithUin(List<DemographicInfoDto> duplicateDemographicDtos) {
		List<String> regIds = duplicateDemographicDtos.stream()
				.map(DemographicInfoDto::getRegId)
				.collect(Collectors.toList());
		List<String> availableUins = registrationStatusDao.getProcessedRegIds(regIds); // new batch method
		return duplicateDemographicDtos.stream()
				.filter(dto -> availableUins.contains(dto.getRegId()))
				.collect(Collectors.toList());
	}

}
