package io.mosip.registration.processor.status.service.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import io.mosip.registration.processor.core.code.RegistrationTransactionTypeCode;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import io.mosip.registration.processor.status.code.RegistrationExternalStatusCode;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dao.RegistrationStatusDao;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusSubRequestDto;
import io.mosip.registration.processor.status.dto.TransactionDto;
import io.mosip.registration.processor.status.entity.BaseRegistrationPKEntity;
import io.mosip.registration.processor.status.entity.RegistrationStatusEntity;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.TransactionService;
import io.mosip.registration.processor.status.utilities.RegistrationExternalStatusUtility;

/**
 * The Class RegistrationStatusServiceImpl.
 */
@Component
public class RegistrationStatusServiceImpl
		implements RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> {

	@Value("${mosip.regproc.registration.status.service.disable-audit:false}")
	private boolean disableAudit;

	/** The registration status dao. */
	@Autowired
	private RegistrationStatusDao registrationStatusDao;

	/** The transcation status service. */
	@Autowired
	private TransactionService<TransactionDto> transcationStatusService;

	/** The core audit request builder. */
	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;

	/** The regexternalstatus util. */
	@Autowired
	private RegistrationExternalStatusUtility regexternalstatusUtil;

	@Value("#{'${registration.processor.main-processes}'.split(',')}")
	private List<String> mainProcess;

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(RegistrationStatusServiceImpl.class);

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * io.mosip.registration.processor.status.service.RegistrationStatusService#
	 * getRegistrationStatus(java.lang.Object)
	 */
	@Override
	public InternalRegistrationStatusDto getRegistrationStatus(String registrationId, String process, Integer iteration, String workflowInstanceId) {

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationId, "RegistrationStatusServiceImpl::getRegistrationStatus()::entry");
		try {
			RegistrationStatusEntity entity = registrationStatusDao.find(registrationId, process, iteration, workflowInstanceId);

			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
					registrationId, "RegistrationStatusServiceImpl::getRegistrationStatus()::exit");

			return entity != null ? convertEntityToDto(entity) : null;
		} catch (DataAccessLayerException e) {

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw new TablenotAccessibleException(
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage(), e);
		}
	}

	@Override
	public List<InternalRegistrationStatusDto> getAllRegistrationStatuses(String registrationId) {

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationId, "RegistrationStatusServiceImpl::getAllRegistrationStatuses()::entry");
		List<InternalRegistrationStatusDto> dtos = new ArrayList<>();
		try {
			List<RegistrationStatusEntity> entities = registrationStatusDao.findAll(registrationId);

			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
					registrationId, "RegistrationStatusServiceImpl::getAllRegistrationStatuses()::exit");

			if(entities != null)
				entities.forEach(e -> dtos.add(convertEntityToDto(e)));
			return dtos;
		} catch (DataAccessLayerException e) {

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw new TablenotAccessibleException(
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage(), e);
		}
	}

	@Override
	public InternalRegistrationStatusDto getRegStatusForMainProcess(String registrationId) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationId, "RegistrationStatusServiceImpl::getRegStatusForMainProcess()::entry");
		List<InternalRegistrationStatusDto> dtos = new ArrayList<>();
		try {
			List<RegistrationStatusEntity> entities = registrationStatusDao.findAll(registrationId);

			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
					registrationId, "RegistrationStatusServiceImpl::getRegStatusForMainProcess()::exit");

			if(entities != null)
				entities.forEach(e -> dtos.add(convertEntityToDto(e)));
			return CollectionUtils.isNotEmpty(dtos) ?
					dtos.stream().filter(s -> mainProcess.contains(s.getRegistrationType())).collect(Collectors.toList()).iterator().next()
					: null;
		} catch (DataAccessLayerException e) {

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw new TablenotAccessibleException(
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * io.mosip.registration.processor.status.service.RegistrationStatusService#
	 * searchRegistrationDetails(java.lang.Object)
	 */

	@Override
	public Page<InternalRegistrationStatusDto> searchRegistrationDetails(SearchInfo searchInfo) {

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"RegistrationStatusServiceImpl::searchRegistrationDetails()::entry");
		List<InternalRegistrationStatusDto> regList = new ArrayList<InternalRegistrationStatusDto>();
		try {
			Page<RegistrationStatusEntity> pageDto = registrationStatusDao
					.getPagedSearchResults(searchInfo.getFilters(), searchInfo.getSort(), searchInfo.getPagination());

			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
					"RegistrationStatusServiceImpl::searchRegistrationDetails()::exit");

			if (!pageDto.getContent().isEmpty()) {
				for (RegistrationStatusEntity regs : pageDto.getContent()) {
					InternalRegistrationStatusDto internalRegis = convertEntityToDto(regs);
					regList.add(internalRegis);
				}

			}
			return new PageImpl<>(regList,
					PageRequest.of(searchInfo.getPagination().getPageStart(),
							searchInfo.getPagination().getPageFetch()),
					pageDto.getTotalElements());
		} catch (DataAccessLayerException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw new TablenotAccessibleException(
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * io.mosip.registration.processor.status.service.RegistrationStatusService#
	 * addRegistrationStatus(java.lang.Object)
	 */
	@Override
	public void addRegistrationStatus(InternalRegistrationStatusDto registrationStatusDto, String moduleId,
			String moduleName) {
		boolean isTransactionSuccessful = false;
		LogDescription description = new LogDescription();
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationStatusDto.getRegistrationId(),
				"RegistrationStatusServiceImpl::addRegistrationStatus()::entry");
		try {
			String transactionId = generateId();
			registrationStatusDto.setLatestRegistrationTransactionId(transactionId);
			registrationStatusDto.setCreateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
			RegistrationStatusEntity entity = convertDtoToEntity(registrationStatusDto, null, false);
			entity.setStatusCode(registrationStatusDto.getStatusCode());
			registrationStatusDao.save(entity);
			isTransactionSuccessful = true;
			description.setMessage("Registration status added successfully");
			TransactionDto transactionDto = new TransactionDto(transactionId, registrationStatusDto.getRegistrationId(),
					null, registrationStatusDto.getLatestTransactionTypeCode(), "Added registration status record",
					registrationStatusDto.getLatestTransactionStatusCode(), registrationStatusDto.getStatusComment(),
					registrationStatusDto.getSubStatusCode());
			transactionDto.setReferenceId(registrationStatusDto.getRegistrationId());
			transactionDto.setReferenceIdType("Added registration record");
			transcationStatusService.addRegistrationTransaction(transactionDto);

		} catch (DataAccessException | DataAccessLayerException e) {
			description.setMessage("DataAccessLayerException while adding Registration status for Registration Id : "
					+ registrationStatusDto.getRegistrationId() + "::" + e.getMessage());

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationStatusDto.getRegistrationId(), e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw new TablenotAccessibleException(
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage(), e);
		} finally {

			String eventId = isTransactionSuccessful ? EventId.RPR_407.toString() : EventId.RPR_405.toString();
			String eventName = eventId.equalsIgnoreCase(EventId.RPR_407.toString()) ? EventName.ADD.toString()
					: EventName.EXCEPTION.toString();
			String eventType = eventId.equalsIgnoreCase(EventId.RPR_407.toString()) ? EventType.BUSINESS.toString()
					: EventType.SYSTEM.toString();

			if(!disableAudit)
				auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, registrationStatusDto.getRegistrationId());
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationStatusDto.getRegistrationId(),
				"RegistrationStatusServiceImpl::addRegistrationStatus()::exit");

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * io.mosip.registration.processor.status.service.RegistrationStatusService#
	 * updateRegistrationStatus(java.lang.Object)
	 */
	@Override
	public void updateRegistrationStatus(InternalRegistrationStatusDto registrationStatusDto, String moduleId,
			String moduleName) {
		updateRegistrationStatus(registrationStatusDto, moduleId, moduleName, false);
	}

	@Override
	public void updateRegistrationStatusForWorkflowEngine(InternalRegistrationStatusDto registrationStatusDto, String moduleId,
			String moduleName) {
		updateRegistrationStatus(registrationStatusDto, moduleId, moduleName, true);
	}

	private void updateRegistrationStatus(InternalRegistrationStatusDto registrationStatusDto, String moduleId,
			String moduleName, boolean updateStatusCode) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationStatusDto.getRegistrationId(),
				"RegistrationStatusServiceImpl::updateRegistrationStatus()::entry");
		boolean isTransactionSuccessful = false;
		LogDescription description = new LogDescription();
		String transactionId = generateId();
		String latestTransactionId = getLatestTransactionId(registrationStatusDto.getRegistrationId(),
				registrationStatusDto.getRegistrationType(), registrationStatusDto.getIteration(), registrationStatusDto.getWorkflowInstanceId());
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
			InternalRegistrationStatusDto dto = getRegistrationStatus(registrationStatusDto.getRegistrationId(),
					registrationStatusDto.getRegistrationType(), registrationStatusDto.getIteration(), registrationStatusDto.getWorkflowInstanceId());
			if (dto != null) {
				dto.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
				RegistrationStatusEntity entity = convertDtoToEntity(registrationStatusDto,
						dto.getLastSuccessStageName(), updateStatusCode);
				if (entity.getStatusCode() == null) {
					entity.setStatusCode(dto.getStatusCode());
				}
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

			if(!disableAudit)
				auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, registrationStatusDto.getRegistrationId());

		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationStatusDto.getRegistrationId(),
				"RegistrationStatusServiceImpl::updateRegistrationStatus()::exit");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * io.mosip.registration.processor.status.service.RegistrationStatusService#
	 * getByStatus(java.lang.String)
	 */
	@Override
	public List<InternalRegistrationStatusDto> getByStatus(String status) {

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"RegistrationStatusServiceImpl::getByStatus()::entry");

		try {
			List<RegistrationStatusEntity> registrationStatusEntityList = registrationStatusDao
					.getEnrolmentStatusByStatusCode(status);

			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
					"RegistrationStatusServiceImpl::getByStatus()::exit");
			return convertEntityListToDtoList(registrationStatusEntityList);
		} catch (DataAccessLayerException e) {

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw new TablenotAccessibleException(
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage(), e);
		}

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * io.mosip.registration.processor.status.service.RegistrationStatusService#
	 * getByIds(java.lang.String)
	 */
	@Override
	public List<RegistrationStatusDto> getByIds(List<RegistrationStatusSubRequestDto> requestIds) {

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"RegistrationStatusServiceImpl::getByIds()::entry");

		try {
			List<String> registrationIds = new ArrayList<>();

			for (RegistrationStatusSubRequestDto registrationStatusSubRequestDto : requestIds) {
				registrationIds.add(registrationStatusSubRequestDto.getRegistrationId());
			}
			List<RegistrationStatusEntity> registrationStatusEntityList = registrationStatusDao
					.getByIds(registrationIds);

			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
					"RegistrationStatusServiceImpl::getByIds()::exit");
			return convertEntitiesToDtoListAndGetExternalStatus(registrationStatusEntityList);

		} catch (DataAccessLayerException e) {

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw new TablenotAccessibleException(
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage(), e);
		}
	}

	/**
	 * Convert entity list to dto list and get external status.
	 *
	 * @param entities
	 *            the entities
	 * @return the list
	 */
	private List<RegistrationStatusDto> convertEntitiesToDtoListAndGetExternalStatus(
			List<RegistrationStatusEntity> entities) {
		List<RegistrationStatusDto> list = new ArrayList<>();
		if (entities != null) {
			for (RegistrationStatusEntity entity : entities) {
				list.add(convertEntityToDtoAndGetExternalStatus(entity));
			}

		}
		return list;
	}

	/**
	 * Convert entity to dto and get external status.
	 *
	 * @param entity
	 *            the entity
	 * @return the registration status dto
	 */
	private RegistrationStatusDto convertEntityToDtoAndGetExternalStatus(RegistrationStatusEntity entity) {
		RegistrationStatusDto registrationStatusDto = new RegistrationStatusDto();
		registrationStatusDto.setRegistrationId(entity.getRegId());
		if (entity.getStatusCode() != null) {
			RegistrationExternalStatusCode registrationExternalStatusCode = regexternalstatusUtil
					.getExternalStatus(entity);

			String mappedValue = registrationExternalStatusCode.toString();

			registrationStatusDto.setStatusCode(mappedValue);
		} else {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					entity.getReferenceRegistrationId(),
					PlatformErrorMessages.RPR_RGS_REGISTRATION_STATUS_NOT_EXIST.getMessage());
		}
		return registrationStatusDto;

	}

	@Override
	public List<RegistrationStatusDto> getExternalStatusByIds(
			List<String> requestIds) {

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"RegistrationStatusServiceImpl::getExternalStatusByIds()::entry");

		try {
			List<RegistrationStatusEntity> registrationStatusEntityList = registrationStatusDao
					.getByIds(requestIds);

			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
					"RegistrationStatusServiceImpl::getExternalStatusByIds()::exit");
			return convertEntityListToDtoListAndGetExternalStatus(registrationStatusEntityList);

		} catch (DataAccessLayerException e) {

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw new TablenotAccessibleException(
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage(), e);
		}
	}

	/**
	 * Convert entity list to dto list and get external status.
	 *
	 * @param entities
	 *            the entities
	 * @return the list
	 */
	private List<RegistrationStatusDto> convertEntityListToDtoListAndGetExternalStatus(
			List<RegistrationStatusEntity> entities) {
		List<RegistrationStatusDto> list = new ArrayList<>();
		if (entities != null) {
			List<String> registrationIds = entities.stream().map(e -> e.getRegId()).distinct()
					.collect(Collectors.toList());

			registrationIds.forEach(registrationId -> {
				List<RegistrationStatusEntity> registrationStatusEntities = entities.stream()
						.filter(r -> registrationId.equals(r.getRegId())).collect(Collectors.toList());
				list.add(convertEntityToDtoAndGetExternalStatus(registrationStatusEntities));
			});

		}
		return list;
	}

	/**
	 * Convert entity to dto and get external status.
	 *
	 * @param entities
	 *            the entities
	 * @return the registration status dto
	 */
	private RegistrationStatusDto convertEntityToDtoAndGetExternalStatus(List<RegistrationStatusEntity> entities) {
		RegistrationStatusDto registrationStatusDto = new RegistrationStatusDto();

		Optional<RegistrationStatusEntity> parentEntity = entities.stream()
				.filter(e -> mainProcess.stream().anyMatch(name -> name.equals(e.getRegistrationType()))).findFirst();

		if (parentEntity.isPresent() && parentEntity.get().getStatusCode() != null) {
			if (parentEntity.get().getStatusCode()
					.equals(RegistrationStatusCode.PAUSED_FOR_ADDITIONAL_INFO.toString())) {
				registrationStatusDto = getRegistrationStatusForSubProcess(entities);
			} else {
				registrationStatusDto = getRegistrationStatusForMainProcess(parentEntity.get());
			}
			registrationStatusDto.setRegistrationId(parentEntity.get().getRegId());
		} else {
			if(parentEntity.isPresent()) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					parentEntity.get().getReferenceRegistrationId(),
					PlatformErrorMessages.RPR_RGS_REGISTRATION_STATUS_NOT_EXIST.getMessage());
			}else {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						null,
						PlatformErrorMessages.RPR_RGS_REGISTRATION_STATUS_NOT_EXIST.getMessage());
			}
		}

		return registrationStatusDto;
	}

	private RegistrationStatusDto getRegistrationStatusForSubProcess(List<RegistrationStatusEntity> entities) {

		RegistrationStatusDto registrationStatusDto = new RegistrationStatusDto();

		RegistrationStatusEntity subProcessEntity = entities.stream().filter(e -> e.getCreateDateTime() != null)
				.max(Comparator.comparing(RegistrationStatusEntity::getCreateDateTime)).orElse(null);

		// when child status not present
		if(entities.size() == 1) {
			registrationStatusDto.setStatusCode(RegistrationExternalStatusCode.AWAITING_INFORMATION.toString());
			return registrationStatusDto;
		}

		if (subProcessEntity != null) {
			if (subProcessEntity.getStatusCode().equals(RegistrationStatusCode.PROCESSING.toString())
					|| subProcessEntity.getStatusCode().equals(RegistrationStatusCode.PAUSED.toString())
					|| subProcessEntity.getStatusCode().equals(RegistrationStatusCode.RESUMABLE.toString())
					|| subProcessEntity.getStatusCode().equals(RegistrationStatusCode.REPROCESS.toString())) {
				registrationStatusDto.setStatusCode(RegistrationExternalStatusCode.PROCESSING.toString());

			} else if (subProcessEntity.getStatusCode().equals(RegistrationStatusCode.PROCESSED.toString())
					|| subProcessEntity.getStatusCode().equals(RegistrationStatusCode.FAILED.toString())
					|| subProcessEntity.getStatusCode().equals(RegistrationStatusCode.REPROCESS_FAILED.toString())
					|| subProcessEntity.getStatusCode().equals(RegistrationStatusCode.REJECTED.toString())) {
				registrationStatusDto.setStatusCode(RegistrationExternalStatusCode.AWAITING_INFORMATION.toString());
			} else {
				registrationStatusDto.setStatusCode(RegistrationExternalStatusCode.PROCESSING.toString());
			}
		} else {
			registrationStatusDto.setStatusCode(RegistrationExternalStatusCode.AWAITING_INFORMATION.toString());
		}
		return registrationStatusDto;
	}

	private RegistrationStatusDto getRegistrationStatusForMainProcess(RegistrationStatusEntity entity) {

		RegistrationStatusDto registrationStatusDto = new RegistrationStatusDto();

		if (entity.getStatusCode().equals(RegistrationStatusCode.PROCESSING.toString())
				|| entity.getStatusCode().equals(RegistrationStatusCode.PAUSED.toString())
				|| entity.getStatusCode().equals(RegistrationStatusCode.RESUMABLE.toString())
				|| entity.getStatusCode().equals(RegistrationStatusCode.REPROCESS.toString())) {
			registrationStatusDto.setStatusCode(RegistrationExternalStatusCode.PROCESSING.toString());
		} else if (entity.getStatusCode().equals(RegistrationStatusCode.PROCESSED.toString())) {
			registrationStatusDto.setStatusCode(RegistrationExternalStatusCode.UIN_GENERATED.toString());
		} else if (entity.getStatusCode().equals(RegistrationStatusCode.FAILED.toString())
				|| entity.getStatusCode().equals(RegistrationStatusCode.REPROCESS_FAILED.toString())) {
			registrationStatusDto.setStatusCode(RegistrationExternalStatusCode.REREGISTER.toString());
		} else {
			registrationStatusDto.setStatusCode(RegistrationExternalStatusCode.REJECTED.toString());
		}

		return registrationStatusDto;
	}


	/**
	 * Convert entity list to dto list.
	 *
	 * @param entities
	 *            the entities
	 * @return the list
	 */
	private List<InternalRegistrationStatusDto> convertEntityListToDtoList(List<RegistrationStatusEntity> entities) {
		List<InternalRegistrationStatusDto> list = new ArrayList<>();
		if (entities != null) {
			for (RegistrationStatusEntity entity : entities) {
				list.add(convertEntityToDto(entity));
			}

		}
		return list;
	}

	/**
	 * Convert entity to dto.
	 *
	 * @param entity
	 *            the entity
	 * @return the internal registration status dto
	 */
	private InternalRegistrationStatusDto convertEntityToDto(RegistrationStatusEntity entity) {
		InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId(entity.getRegId());
		registrationStatusDto.setRegistrationType(entity.getRegistrationType());
		registrationStatusDto.setReferenceRegistrationId(entity.getReferenceRegistrationId());
		registrationStatusDto.setStatusCode(entity.getStatusCode());
		registrationStatusDto.setLangCode(entity.getLangCode());
		registrationStatusDto.setStatusComment(entity.getStatusComment());
		registrationStatusDto.setLatestRegistrationTransactionId(entity.getLatestRegistrationTransactionId());
		registrationStatusDto.setIsActive(entity.isActive());
		registrationStatusDto.setCreatedBy(entity.getCreatedBy());
		registrationStatusDto.setCreateDateTime(entity.getCreateDateTime());
		registrationStatusDto.setUpdatedBy(entity.getUpdatedBy());
		registrationStatusDto.setUpdateDateTime(entity.getUpdateDateTime());
		registrationStatusDto.setIsDeleted(entity.isDeleted());
		registrationStatusDto.setDeletedDateTime(entity.getDeletedDateTime());
		registrationStatusDto.setRetryCount(entity.getRetryCount());
		registrationStatusDto.setApplicantType(entity.getApplicantType());
		registrationStatusDto.setReProcessRetryCount(entity.getRegProcessRetryCount());
		registrationStatusDto.setLatestTransactionStatusCode(entity.getLatestTransactionStatusCode());
		registrationStatusDto.setLatestTransactionTypeCode(entity.getLatestTransactionTypeCode());
		registrationStatusDto.setRegistrationStageName(entity.getRegistrationStageName());
		registrationStatusDto.setUpdateDateTime(entity.getUpdateDateTime());
		registrationStatusDto.setResumeTimeStamp(entity.getResumeTimeStamp());
		registrationStatusDto.setDefaultResumeAction(entity.getDefaultResumeAction());
		registrationStatusDto.setPauseRuleIds(entity.getPauseRuleIds());
		registrationStatusDto.setLastSuccessStageName(entity.getLastSuccessStageName());
		registrationStatusDto.setSource(entity.getSource());
		registrationStatusDto.setIteration(entity.getIteration());
		registrationStatusDto.setWorkflowInstanceId(entity.getId().getWorkflowInstanceId());
		registrationStatusDto.setPacketCreateDateTime(entity.getPacketCreatedDateTime());
		return registrationStatusDto;
	}

	/**
	 * Convert dto to entity.
	 *
	 * @param dto
	 *            the dto
	 * @return the registration status entity
	 */
	private RegistrationStatusEntity convertDtoToEntity(InternalRegistrationStatusDto dto, 
			String existingLastSuccessStageName, boolean updateStatusCode) {
		BaseRegistrationPKEntity pk = new BaseRegistrationPKEntity();
		pk.setWorkflowInstanceId(dto.getWorkflowInstanceId());

		RegistrationStatusEntity registrationStatusEntity = new RegistrationStatusEntity();
		registrationStatusEntity.setId(pk);
		registrationStatusEntity.setRegId(dto.getRegistrationId());
		registrationStatusEntity.setIteration(dto.getIteration());
		registrationStatusEntity.setReferenceRegistrationId(dto.getReferenceRegistrationId());
		if (updateStatusCode) {
			registrationStatusEntity.setStatusCode(dto.getStatusCode());
		}
		registrationStatusEntity.setRegistrationType(dto.getRegistrationType());
		registrationStatusEntity.setSource(dto.getSource());
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
		registrationStatusEntity.setPauseRuleIds(dto.getPauseRuleIds());
		if(dto.getLatestTransactionStatusCode().equals(RegistrationTransactionStatusCode.SUCCESS.toString()) 
			|| dto.getLatestTransactionStatusCode().equals(
				RegistrationTransactionStatusCode.PROCESSED.toString()))
			registrationStatusEntity.setLastSuccessStageName(dto.getRegistrationStageName());
		else
			registrationStatusEntity.setLastSuccessStageName(existingLastSuccessStageName);
		registrationStatusEntity.setPacketCreatedDateTime(dto.getPacketCreateDateTime());
		return registrationStatusEntity;
	}

	/**
	 * Gets the latest transaction id.
	 *
	 * @param registrationId
	 *            the registration id
	 * @return the latest transaction id
	 */
	private String getLatestTransactionId(String registrationId, String process, int iteration, String workflowInstanceId) {
		RegistrationStatusEntity entity = registrationStatusDao.find(registrationId, process, iteration, workflowInstanceId);
		return entity != null ? entity.getLatestRegistrationTransactionId() : null;

	}

	/**
	 * Generate id.
	 *
	 * @return the string
	 */
	public String generateId() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Gets the un processed packets.
	 *
	 * @param fetchSize
	 *            the fetch size
	 * @param elapseTime
	 *            the elapse time
	 * @param reprocessCount
	 *            the reprocess count
	 * @param status
	 *            the status
	 * @return the un processed packets
	 */
	public List<InternalRegistrationStatusDto> getUnProcessedPackets(Integer fetchSize, long elapseTime,
			Integer reprocessCount, List<String> status, List<String> excludeStageNames) {

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"RegistrationStatusServiceImpl::getReprocessPacket()::entry");
		try {
			List<RegistrationStatusEntity> entityList = registrationStatusDao.getUnProcessedPackets(fetchSize,
					elapseTime, reprocessCount, status, excludeStageNames);

			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
					"RegistrationStatusServiceImpl::getReprocessPacket()::exit");

			return convertEntityListToDtoList(entityList);

		} catch (DataAccessException | DataAccessLayerException e) {

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw new TablenotAccessibleException(
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.processor.status.service.RegistrationStatusService#
	 * getUnProcessedPacketsCount(long, java.lang.Integer, java.util.List)
	 */
	@Override
	public Integer getUnProcessedPacketsCount(long elapseTime, Integer reprocessCount, List<String> status,
			List<String> excludeStageNames) {

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"RegistrationStatusServiceImpl::getUnProcessedPacketsCount()::entry");
		try {
			int count = registrationStatusDao.getUnProcessedPacketsCount(elapseTime, reprocessCount, status,
				excludeStageNames);

			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
					"RegistrationStatusServiceImpl::getUnProcessedPacketsCount()::exit");

			return count;

		} catch (DataAccessException | DataAccessLayerException e) {

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw new TablenotAccessibleException(
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage(), e);
		}
	}

	
	@Override
	public List<InternalRegistrationStatusDto> getActionablePausedPackets(Integer fetchSize) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"RegistrationStatusServiceImpl::getActionablePausedPackets()::entry");
		try {
			List<RegistrationStatusEntity> entityList = registrationStatusDao.getActionablePausedPackets( fetchSize);

			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
					"RegistrationStatusServiceImpl::getActionablePausedPackets()::exit");

			return convertEntityListToDtoList(entityList);

		} catch (DataAccessException | DataAccessLayerException e) {

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw new TablenotAccessibleException(
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage(), e);
		}
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.processor.status.service.RegistrationStatusService#
	 * checkUinAvailabilityForRid(java.lang.String)
	 */
	@Override
	public Boolean checkUinAvailabilityForRid(String rid) {
		return registrationStatusDao.checkUinAvailabilityForRid(rid);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.processor.status.service.RegistrationStatusService#
	 * getByIdsAndTimestamp(java.util.List)
	 */
	@Override
	public List<InternalRegistrationStatusDto> getByIdsAndTimestamp(List<String> ids) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"RegistrationStatusServiceImpl::getByIdsAndTimestamp()::entry");

		try {
			List<RegistrationStatusEntity> registrationStatusEntityList = registrationStatusDao
					.getByIdsAndTimestamp(ids);

			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
					"RegistrationStatusServiceImpl::getByIdsAndTimestamp()::exit");
			return convertEntityListToDtoList(registrationStatusEntityList);
		} catch (DataAccessLayerException e) {

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw new TablenotAccessibleException(
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage(), e);
		}
	}

	@Override
	public List<InternalRegistrationStatusDto> getResumablePackets(Integer fetchSize) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"RegistrationStatusServiceImpl::getResumablePackets()::entry");
		try {
			List<RegistrationStatusEntity> entityList = registrationStatusDao.getResumablePackets(fetchSize);

			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
					"RegistrationStatusServiceImpl::getResumablePackets()::exit");

			return convertEntityListToDtoList(entityList);

		} catch (DataAccessException | DataAccessLayerException e) {

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw new TablenotAccessibleException(
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage(), e);
		}
	}

	private RegistrationStatusEntity convertDtoToEntityForWorkFlow(InternalRegistrationStatusDto dto) {
		BaseRegistrationPKEntity pk = new BaseRegistrationPKEntity();
		pk.setWorkflowInstanceId(dto.getWorkflowInstanceId());

		RegistrationStatusEntity registrationStatusEntity = new RegistrationStatusEntity();
		registrationStatusEntity.setId(pk);
		registrationStatusEntity.setRegId(dto.getRegistrationId());
		registrationStatusEntity.setRegistrationType(dto.getRegistrationType());
		registrationStatusEntity.setIteration(dto.getIteration());
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
		registrationStatusEntity.setUpdateDateTime(dto.getUpdateDateTime());
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
		registrationStatusEntity.setLatestTransactionTimes(dto.getLatestTransactionTimes());
		registrationStatusEntity.setResumeTimeStamp(dto.getResumeTimeStamp());
		registrationStatusEntity.setDefaultResumeAction(dto.getDefaultResumeAction());
		return registrationStatusEntity;
	}

	@Override
	public void updateRegistrationStatusForWorkflow(InternalRegistrationStatusDto registrationStatusDto,
			String moduleId, String moduleName) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationStatusDto.getRegistrationId(),
				"RegistrationStatusServiceImpl::updateRegistrationStatusForWorkFlow()::entry");
		boolean isTransactionSuccessful = false;
		LogDescription description = new LogDescription();

		try {
			InternalRegistrationStatusDto dto = getRegistrationStatus(registrationStatusDto.getRegistrationId(),
					registrationStatusDto.getRegistrationType(), registrationStatusDto.getIteration(), registrationStatusDto.getWorkflowInstanceId());
			if (dto != null) {
				RegistrationStatusEntity entity = convertDtoToEntityForWorkFlow(registrationStatusDto);
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
				"RegistrationStatusServiceImpl::updateRegistrationStatusForWorkFlow()::exit");

	}
	
}
