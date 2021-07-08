package io.mosip.registration.processor.status.service.impl;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.status.code.PacketExternalStatusCode;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.PacketExternalStatusDTO;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.service.PacketExternalStatusService;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.SyncRegistrationService;

@Component
public class PacketExternalStatusServiceImpl implements PacketExternalStatusService {
	
	@Autowired
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(PacketExternalStatusServiceImpl.class);

	/** The sync registration service. */
	@Autowired
	SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationService;

	@Value("#{'${mosip.registration.processor.packet.status.transactiontypecodes-before-uploading-to-objectstore:PACKET_RECEIVER,SECUREZONE_NOTIFICATION}'.split(',')}")
	private List<String> transactionTypeCodesBeforeUploadingToObjectStore;

	@Value("${mosip.registration.processor.packet.status.transactiontypecodes-uploading-to-objectstore:UPLOAD_PACKET}")
	private String transactionTypeCodesUploadingToObjectStore;

	@Value("#{'${mosip.registration.processor.packet.status.transactiontypecodes-time-based-resend-required:PACKET_RECEIVER}'.split(',')}")
	private List<String> transactionTypeCodeTimeBasesResendRequired;

	/** The elapsed time. */
	@Value("${registration.processor.reprocess.elapse.time}")
	private int elapsedTime;

	/** The threshold time. */
	@Value("${registration.processor.max.retry}")
	private int maxRetryCount;

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
			PacketExternalStatusCode packetExternalStatusCode = getPacketExternalStatusCode(
					internalRegistrationStatusDto);

			String mappedValue = packetExternalStatusCode.toString();

			packetStatusDTO.setStatusCode(mappedValue);
		} else {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					internalRegistrationStatusDto.getReferenceRegistrationId(),
					PlatformErrorMessages.RPR_RGS_REGISTRATION_STATUS_NOT_EXIST.getMessage());
		}
		return packetStatusDTO;
	}

	private PacketExternalStatusCode getPacketExternalStatusCode(
			InternalRegistrationStatusDto internalRegistrationStatusDto) {
		PacketExternalStatusCode mappedValue = null;
		String status = internalRegistrationStatusDto.getStatusCode();
		if (transactionTypeCodesBeforeUploadingToObjectStore
				.contains(internalRegistrationStatusDto.getLatestTransactionTypeCode())
				|| transactionTypeCodesUploadingToObjectStore
						.contains(internalRegistrationStatusDto.getLatestTransactionTypeCode())) {
			if (status.equalsIgnoreCase(RegistrationStatusCode.PROCESSING.toString())
					|| status.equalsIgnoreCase(RegistrationStatusCode.REPROCESS.toString())
					|| status.equalsIgnoreCase(RegistrationStatusCode.RESUMABLE.toString())) {
				if (transactionTypeCodeTimeBasesResendRequired
						.contains(internalRegistrationStatusDto.getLatestTransactionTypeCode())) {
					long timeElapsedinPacketreceiver = checkElapsedTime(internalRegistrationStatusDto);
					if (timeElapsedinPacketreceiver > elapsedTime) {
						if ((internalRegistrationStatusDto.getRetryCount() < maxRetryCount)) {
							mappedValue = PacketExternalStatusCode.RESEND;
						} else {
							mappedValue = PacketExternalStatusCode.PROCESSED;// REJECTED?
						}
					} else {
						mappedValue = PacketExternalStatusCode.PROCESSING;
					}
				}else {
					mappedValue = PacketExternalStatusCode.PROCESSING;
				}
           
			} else if (status.equalsIgnoreCase(RegistrationStatusCode.FAILED.toString())) {
				if ((internalRegistrationStatusDto.getRetryCount() < maxRetryCount)) {
					mappedValue = PacketExternalStatusCode.RESEND;
				} else {
					mappedValue = PacketExternalStatusCode.PROCESSED;
				}
			}
			else if (!transactionTypeCodesUploadingToObjectStore
					.contains(internalRegistrationStatusDto.getLatestTransactionTypeCode())) {
				mappedValue = PacketExternalStatusCode.PROCESSING;
			} else {
				mappedValue = PacketExternalStatusCode.PROCESSED;
			}
		} else {
			mappedValue = PacketExternalStatusCode.PROCESSED;
		}
		return mappedValue;
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
		packetStatusDTO.setStatusCode(PacketExternalStatusCode.UPLOAD_PENDING.toString());
		return packetStatusDTO;
	}

	private Long checkElapsedTime(InternalRegistrationStatusDto internalRegistrationStatusDto) {
		LocalDateTime createdTime = internalRegistrationStatusDto.getLatestTransactionTimes();
		LocalDateTime currentTime = LocalDateTime.now();
		LocalDateTime tempDate = LocalDateTime.from(createdTime);
		long seconds = tempDate.until(currentTime, ChronoUnit.SECONDS);
		return seconds;
	}
}
