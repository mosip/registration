package io.mosip.registration.processor.status.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.status.code.RegistrationExternalStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.PacketExternalStatusDTO;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.service.PacketExternalStatusService;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.SyncRegistrationService;
import io.mosip.registration.processor.status.utilities.RegistrationExternalStatusUtility;

@Component
public class PacketExternalStatusServiceImpl implements PacketExternalStatusService {
	
	@Autowired
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The regexternalstatus util. */
	@Autowired
	private RegistrationExternalStatusUtility regexternalstatusUtil;

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(PacketExternalStatusServiceImpl.class);

	/** The sync registration service. */
	@Autowired
	SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationService;

	@Override
	public List<PacketExternalStatusDTO> getByPacketIds(List<String> packetIdList) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"PacketExternalStatusServiceImpl::getByPacketIds()::entry");
		List<PacketExternalStatusDTO> packetExternalStatusDTOList = new ArrayList<>();
		List<SyncRegistrationEntity> syncRegistrationEntityList = syncRegistrationService
				.getByPacketIds(packetIdList);
		if (syncRegistrationEntityList != null) {
			for (SyncRegistrationEntity syncRegistrationEntity : syncRegistrationEntityList) {
				InternalRegistrationStatusDto internalRegistrationStatusDto = registrationStatusService
						.getRegistrationStatus(null, null, null,
						syncRegistrationEntity.getWorkflowInstanceId());
				if (internalRegistrationStatusDto != null) {
					PacketExternalStatusDTO packetExternalStatusDTO = getExternalStatusForPacket(
							internalRegistrationStatusDto);
					packetExternalStatusDTO.setPacketId(syncRegistrationEntity.getPacketId());
					packetExternalStatusDTOList.add(packetExternalStatusDTO);
				}
			}
			List<SyncRegistrationEntity> packetIdsNotAvailableInRegistration = syncRegistrationEntityList
					.stream().filter(
							syncRegistrationEntity -> packetExternalStatusDTOList.stream()
									.noneMatch(packetExternalStatusDTO -> packetExternalStatusDTO.getPacketId()
											.equals(syncRegistrationEntity.getPacketId())))
					.collect(Collectors.toList());
			packetExternalStatusDTOList.addAll(
					convertEntityListToDtoListAndGetExternalStatusForPacket(packetIdsNotAvailableInRegistration));
		}

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"PacketExternalStatusServiceImpl::getByPacketIds()::exit");
		return packetExternalStatusDTOList;
	}

	private PacketExternalStatusDTO getExternalStatusForPacket(
			InternalRegistrationStatusDto internalRegistrationStatusDto) {
		PacketExternalStatusDTO packetStatusDTO = new PacketExternalStatusDTO();
		if (internalRegistrationStatusDto.getStatusCode() != null) {
			RegistrationExternalStatusCode registrationExternalStatusCode = null;// regexternalstatusUtil.getExternalStatus(entity);

			String mappedValue = registrationExternalStatusCode.toString();

			packetStatusDTO.setStatusCode(mappedValue);
		} else {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					internalRegistrationStatusDto.getReferenceRegistrationId(),
					PlatformErrorMessages.RPR_RGS_REGISTRATION_STATUS_NOT_EXIST.getMessage());
		}
		return packetStatusDTO;
	}

	private List<PacketExternalStatusDTO> convertEntityListToDtoListAndGetExternalStatusForPacket(
			List<SyncRegistrationEntity> syncRegistrationEntityList) {
		List<PacketExternalStatusDTO> list = new ArrayList<>();
		if (syncRegistrationEntityList != null) {
			for (SyncRegistrationEntity entity : syncRegistrationEntityList) {
				list.add(convertEntityToDtoAndGetExternalStatusForPacket(entity));
			}
		}
		return list;
	}

	private PacketExternalStatusDTO convertEntityToDtoAndGetExternalStatusForPacket(SyncRegistrationEntity entity) {
		PacketExternalStatusDTO packetStatusDTO = new PacketExternalStatusDTO();
		packetStatusDTO.setPacketId(entity.getPacketId());
		packetStatusDTO.setStatusCode(RegistrationExternalStatusCode.UPLOAD_PENDING.toString());
		return packetStatusDTO;
	}
}
