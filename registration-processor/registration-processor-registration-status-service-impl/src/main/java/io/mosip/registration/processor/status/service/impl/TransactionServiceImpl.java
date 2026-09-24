package io.mosip.registration.processor.status.service.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.status.entity.TrackerEntity;
import io.mosip.registration.processor.status.repositary.TrackerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.status.dto.RegistrationTransactionDto;
import io.mosip.registration.processor.status.dto.TransactionDto;
import io.mosip.registration.processor.status.entity.TransactionEntity;
import io.mosip.registration.processor.status.exception.RegTransactionAppException;
import io.mosip.registration.processor.status.exception.TransactionTableNotAccessibleException;
import io.mosip.registration.processor.status.exception.TransactionsUnavailableException;
import io.mosip.registration.processor.status.repositary.TransactionRepository;
import io.mosip.registration.processor.status.service.TransactionService;

/**
 * The Class TransactionServiceImpl.
 */
@Service
public class TransactionServiceImpl implements TransactionService<TransactionDto> {

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(TransactionServiceImpl.class);

	/** The transaction repositary. */
	@Autowired
	TransactionRepository<TransactionEntity, String> transactionRepositary;

	@Autowired
	TrackerRepository trackerRepository;



	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.status.service.TransactionService#
	 * addRegistrationTransaction(java.lang.Object)
	 */
	@Override
	public TransactionEntity addRegistrationTransaction(TransactionDto transactionStatusDto) {
		try {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
					transactionStatusDto.getRegistrationId(),
					"TransactionServiceImpl::addRegistrationTransaction()::entry");
			TransactionEntity entity = convertDtoToEntity(transactionStatusDto);
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
					transactionStatusDto.getRegistrationId(),
					"TransactionServiceImpl::addRegistrationTransaction()::exit");
			return transactionRepositary.save(entity);
		} catch (DataAccessLayerException e) {
			throw new TransactionTableNotAccessibleException(
					PlatformErrorMessages.RPR_RGS_TRANSACTION_TABLE_NOT_ACCESSIBLE.getMessage(), e);
		}

	}

	/**
	 * Convert dto to entity.
	 *
	 * @param dto
	 *            the dto
	 * @return the transaction entity
	 */
	private TransactionEntity convertDtoToEntity(TransactionDto dto) {
		TransactionEntity transcationEntity = new TransactionEntity(dto.getTransactionId(), dto.getRegistrationId(),
				dto.getParentid(), dto.getTrntypecode(), dto.getSubStatusCode(), dto.getStatusCode(),
				dto.getStatusComment());
		transcationEntity.setRemarks(dto.getRemarks());
		transcationEntity.setStatusComment(dto.getStatusComment());
		transcationEntity.setCreatedBy("MOSIP_SYSTEM");
		transcationEntity.setCreateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		transcationEntity.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		transcationEntity.setLangCode("eng");
		transcationEntity.setReferenceId(dto.getReferenceId());
		transcationEntity.setReferenceIdType(dto.getReferenceIdType());
		transcationEntity.setTransactionFlowId(dto.getTransactionFlowId());
		return transcationEntity;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.status.service.TransactionService#
	 * getTransactionByRegIdAndStatusCode(java.lang.String, java.lang.String)
	 */
	@Override
	public TransactionDto getTransactionByRegIdAndStatusCode(String regId, String statusCode) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), regId,
				"TransactionServiceImpl::addRegistrationTransaction()::entry");
		TransactionDto dto = null;
		List<TransactionEntity> transactionEntityList = transactionRepositary.getTransactionByRegIdAndStatusCode(regId,
				statusCode);
		if (!CollectionUtils.isEmpty(transactionEntityList)) {
			dto = convertEntityToDto(transactionEntityList.get(0));
		}

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), regId,
				"TransactionServiceImpl::addRegistrationTransaction()::exit");
		return dto;
	}

	@Override
	public List<RegistrationTransactionDto> getTransactionByRegId(String regId)
			throws TransactionsUnavailableException, RegTransactionAppException {

		List<RegistrationTransactionDto> dtoList = new ArrayList<RegistrationTransactionDto>();
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), regId,
				"TransactionServiceImpl::getTransactionByRegId()::entry");
		try {
			List<TransactionEntity> transactionEntityList = transactionRepositary.getTransactionByRegId(regId);
			if (transactionEntityList == null || transactionEntityList.isEmpty()) {
				throw new TransactionsUnavailableException(PlatformErrorMessages.TRANSACTIONS_NOT_AVAILABLE.getCode(),
						PlatformErrorMessages.TRANSACTIONS_NOT_AVAILABLE.getMessage());
			}
			for (TransactionEntity transactionEntity : transactionEntityList) {
				dtoList.add(convertEntityToRegistrationTransactionDto(transactionEntity));
			}
		} catch (DataAccessLayerException e) {
			throw new TransactionTableNotAccessibleException(
					PlatformErrorMessages.RPR_RGS_TRANSACTION_TABLE_NOT_ACCESSIBLE.getMessage(), e);
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), regId,
				"TransactionServiceImpl::getTransactionByRegId()::exit");
		return dtoList;
	}

	@Override
	public TrackerEntity isTransactionExist(String regId, String transactionId, String latestTrnFlowId) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), regId,
				"TransactionServiceImpl::isTransactionExist()::entry");
		TrackerEntity entity = trackerRepository.findByRegIdAndTransactionIdAndFlowId(regId, transactionId, latestTrnFlowId);
		regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), regId,
				"TransactionServiceImpl::isTransactionExist()::Record Found");
		if(entity == null) {
			entity = new TrackerEntity();
			entity.setRegistrationId(regId);
			entity.setTransactionId(transactionId);
			entity.setTransactionFlowId(latestTrnFlowId);
			entity.setStatusCode(RegistrationTransactionStatusCode.IN_PROGRESS.toString());
			entity.setCreateDateTime(LocalDateTime.now());
			trackerRepository.save(entity);
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), regId,
					"TransactionServiceImpl::isTransactionExist()::Writing Record");

			entity.setStatusCode(RegistrationTransactionStatusCode.PROCESSING.toString());
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), regId,
				"TransactionServiceImpl::isTransactionExist()::exist");

		return entity;
	}

	@Override
	public TrackerEntity updateTransactionComplete(String transactionId, String StatusCode) throws TransactionsUnavailableException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), transactionId,
				"TransactionServiceImpl::updateTransactionComplete()::entry");
		TrackerEntity entity = trackerRepository.findByTransactionId(transactionId);
		if(entity != null) {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), transactionId,
					"TransactionServiceImpl::updateTransactionComplete()::Entity Found Updating same for Transaction Id " + transactionId);
			entity.setStatusCode(StatusCode);
			entity.setUpdatedBy("MOSIP");
			entity.setUpdateDateTime(LocalDateTime.now());
			trackerRepository.save(entity);
		} else {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), transactionId,
					"TransactionServiceImpl::updateTransactionComplete()::Entity not Found for Transaction Id" + transactionId);
			throw new TransactionsUnavailableException(PlatformErrorMessages.RPR_PGS_NO_RECORDS_EXCEPTION.getCode(), "Record Not Found for the Transaction Id : " + transactionId);
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), transactionId,
				"TransactionServiceImpl::updateTransactionComplete()::exist");

		return entity;
	}

	/**
	 * Convert entity to dto.
	 *
	 * @param entity
	 *            the entity
	 * @return the transaction dto
	 */
	private TransactionDto convertEntityToDto(TransactionEntity entity) {
		return new TransactionDto(entity.getId(), entity.getRegistrationId(), entity.getParentid(),
				entity.getTrntypecode(), entity.getRemarks(), entity.getStatusCode(), entity.getStatusComment(),
				entity.getSubStatusCode(), entity.getTransactionFlowId());

	}

	private RegistrationTransactionDto convertEntityToRegistrationTransactionDto(TransactionEntity entity) {
		return new RegistrationTransactionDto(entity.getId(), entity.getRegistrationId(), entity.getTrntypecode(),
				entity.getParentid(), entity.getStatusCode(), entity.getSubStatusCode(), entity.getStatusComment(),
				entity.getCreateDateTime(), entity.getTransactionFlowId());
	}
}
