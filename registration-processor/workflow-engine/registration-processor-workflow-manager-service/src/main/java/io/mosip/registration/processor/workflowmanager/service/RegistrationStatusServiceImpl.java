package io.mosip.registration.processor.workflowmanager.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.workflow.dto.SearchInfo;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.dao.RegistrationStatusDao;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusSubRequestDto;
import io.mosip.registration.processor.status.dto.TransactionDto;
import io.mosip.registration.processor.status.entity.RegistrationStatusEntity;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.TransactionService;

@Component("workflowStatusServiceImpl")
public class RegistrationStatusServiceImpl
		implements RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> {

	private static Logger regProcLogger = RegProcessorLogger.getLogger(RegistrationStatusServiceImpl.class);

	@Autowired
	private RegistrationStatusDao registrationStatusDao;

	@Autowired
	private TransactionService<TransactionDto> transcationStatusService;

	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;

	@Override
	public InternalRegistrationStatusDto getRegistrationStatus(String enrolmentId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addRegistrationStatus(InternalRegistrationStatusDto registrationStatusDto, String moduleId,
			String moduleName) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateRegistrationStatus(InternalRegistrationStatusDto registrationStatusDto, String moduleId,
			String moduleName) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationStatusDto.getRegistrationId(),
				"RegistrationStatusServiceImpl::updateRegistrationStatus()::entry");
		boolean isTransactionSuccessful = false;
		LogDescription description = new LogDescription();
		String transactionId = generateId();
		String latestTransactionId = getLatestTransactionId(registrationStatusDto.getRegistrationId());
		TransactionDto transactionDto = new TransactionDto(transactionId, registrationStatusDto.getRegistrationId(),
				latestTransactionId, registrationStatusDto.getLatestTransactionTypeCode(),
				"updated registration status record", registrationStatusDto.getLatestTransactionStatusCode(),
				registrationStatusDto.getStatusComment(), registrationStatusDto.getSubStatusCode());
		if (registrationStatusDto.getRefId() == null) {
			transactionDto.setReferenceId(registrationStatusDto.getRegistrationId());
		} else {
			transactionDto.setReferenceId(registrationStatusDto.getRefId());
		}

		transactionDto.setReferenceIdType("updated registration record");
		transcationStatusService.addRegistrationTransaction(transactionDto);

		registrationStatusDto.setLatestRegistrationTransactionId(transactionId);
		try {
			InternalRegistrationStatusDto dto = getRegistrationStatus(registrationStatusDto.getRegistrationId());
			if (dto != null) {
				dto.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
				RegistrationStatusEntity entity = convertDtoToEntity(registrationStatusDto,
						dto.getLastSuccessStageName());
				registrationStatusDao.save(entity);
				isTransactionSuccessful = true;
				description.setMessage("Updated registration status successfully");
			}
		} catch (DataAccessException | DataAccessLayerException e) {
			description.setMessage("DataAccessLayerException while Updating registration status for registration Id"
					+ registrationStatusDto.getRegistrationId() + "::" + e.getMessage());

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationStatusDto.getRegistrationId(), e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw new TablenotAccessibleException(
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage(), e);
		} finally {

			String eventId = isTransactionSuccessful ? EventId.RPR_407.toString() : EventId.RPR_405.toString();
			String eventName = eventId.equalsIgnoreCase(EventId.RPR_407.toString()) ? EventName.UPDATE.toString()
					: EventName.EXCEPTION.toString();
			String eventType = eventId.equalsIgnoreCase(EventId.RPR_407.toString()) ? EventType.BUSINESS.toString()
					: EventType.SYSTEM.toString();

			auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, registrationStatusDto.getRegistrationId());

		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationStatusDto.getRegistrationId(),
				"RegistrationStatusServiceImpl::updateRegistrationStatus()::exit");
	}

	@Override
	public List<InternalRegistrationStatusDto> getByStatus(String status) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<RegistrationStatusDto> getByIds(List<RegistrationStatusSubRequestDto> requestIds) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<InternalRegistrationStatusDto> getUnProcessedPackets(Integer fetchSize, long elapseTime,
			Integer reprocessCount, List<String> status) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer getUnProcessedPacketsCount(long elapseTime, Integer reprocessCount, List<String> status) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean checkUinAvailabilityForRid(String rid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<InternalRegistrationStatusDto> getByIdsAndTimestamp(List<String> ids) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<InternalRegistrationStatusDto> getActionablePausedPackets(Integer fetchSize) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Page<InternalRegistrationStatusDto> searchRegistrationDetails(SearchInfo searchInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<InternalRegistrationStatusDto> getResumablePackets(Integer fetchSize) {
		// TODO Auto-generated method stub
		return null;
	}

	public String generateId() {
		return UUID.randomUUID().toString();
	}

	private String getLatestTransactionId(String registrationId) {
		RegistrationStatusEntity entity = registrationStatusDao.findById(registrationId);
		return entity != null ? entity.getLatestRegistrationTransactionId() : null;

	}

	private RegistrationStatusEntity convertDtoToEntity(InternalRegistrationStatusDto dto,
			String existingLastSuccessStageName) {
		RegistrationStatusEntity registrationStatusEntity = new RegistrationStatusEntity();
		registrationStatusEntity.setId(dto.getRegistrationId());
		registrationStatusEntity.setRegistrationType(dto.getRegistrationType());
		registrationStatusEntity.setReferenceRegistrationId(dto.getReferenceRegistrationId());
		registrationStatusEntity.setStatusCode(dto.getStatusCode());
		registrationStatusEntity.setLangCode(dto.getLangCode());
		registrationStatusEntity.setStatusComment(dto.getStatusComment());
		registrationStatusEntity.setLatestRegistrationTransactionId(dto.getLatestRegistrationTransactionId());
		registrationStatusEntity.setIsActive(dto.isActive());
		registrationStatusEntity.setCreatedBy(dto.getCreatedBy());
		if (dto.getCreateDateTime() == null) {
			registrationStatusEntity.setCreateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		} else {
			registrationStatusEntity.setCreateDateTime(dto.getCreateDateTime());
		}
		registrationStatusEntity.setUpdatedBy(dto.getUpdatedBy());
		registrationStatusEntity.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		registrationStatusEntity.setIsDeleted(dto.isDeleted());

		if (registrationStatusEntity.isDeleted() != null && registrationStatusEntity.isDeleted()) {
			registrationStatusEntity.setDeletedDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		} else {
			registrationStatusEntity.setDeletedDateTime(null);
		}

		registrationStatusEntity.setRetryCount(dto.getRetryCount());
		registrationStatusEntity.setApplicantType(dto.getApplicantType());
		registrationStatusEntity.setRegProcessRetryCount(dto.getReProcessRetryCount());
		registrationStatusEntity.setLatestTransactionStatusCode(dto.getLatestTransactionStatusCode());
		registrationStatusEntity.setLatestTransactionTypeCode(dto.getLatestTransactionTypeCode());
		registrationStatusEntity.setRegistrationStageName(dto.getRegistrationStageName());
		registrationStatusEntity.setLatestTransactionTimes(LocalDateTime.now(ZoneId.of("UTC")));
		registrationStatusEntity.setResumeTimeStamp(dto.getResumeTimeStamp());
		registrationStatusEntity.setDefaultResumeAction(dto.getDefaultResumeAction());
		registrationStatusEntity.setResumeRemoveTags(dto.getResumeRemoveTags());
		if (dto.getLatestTransactionStatusCode().equals(RegistrationTransactionStatusCode.SUCCESS.toString())
				|| dto.getLatestTransactionStatusCode().equals(RegistrationTransactionStatusCode.PROCESSED.toString()))
			registrationStatusEntity.setLastSuccessStageName(dto.getRegistrationStageName());
		else
			registrationStatusEntity.setLastSuccessStageName(existingLastSuccessStageName);
		return registrationStatusEntity;
	}

}
