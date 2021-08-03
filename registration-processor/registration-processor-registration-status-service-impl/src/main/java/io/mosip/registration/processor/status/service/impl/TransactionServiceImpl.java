package io.mosip.registration.processor.status.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
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
import io.mosip.registration.processor.status.repositary.RegistrationRepositary;
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

	private static final String supportedLanguageKey = "mosip.supported-languages";

	@Autowired
	Environment environment;

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
	public List<RegistrationTransactionDto> getTransactionByRegId(String regId, String langCode)
			throws TransactionsUnavailableException, RegTransactionAppException {

		String supportedLanguage = environment.getProperty(supportedLanguageKey);
		List<String> supportedLanguages = supportedLanguage != null ? Arrays.asList(supportedLanguage.split(","))
				: new ArrayList<>();
		if (!supportedLanguages.contains(langCode)) {
			throw new RegTransactionAppException(PlatformErrorMessages.RPR_RTS_INVALID_REQUEST.getCode(),
					PlatformErrorMessages.RPR_RTS_INVALID_REQUEST.getMessage() + " - langCode");
		}
		List<RegistrationTransactionDto> dtoList = new ArrayList<RegistrationTransactionDto>();
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), regId,
				"TransactionServiceImpl::getTransactionByRegId()::entry");
		try {
			List<TransactionEntity> transactionEntityList = transactionRepositary.getTransactionByRegId(regId);
			if (transactionEntityList == null || transactionEntityList.isEmpty()) {
				throw new TransactionsUnavailableException(PlatformErrorMessages.TRANSACTIONS_NOT_AVAILABLE.getCode(),
						PlatformErrorMessages.TRANSACTIONS_NOT_AVAILABLE.getMessage());
			}
			ClassLoader classLoader = getClass().getClassLoader();
			String messagesPropertiesFileName = "globalMessages_" + langCode + ".properties";
			InputStream inputStream = classLoader.getResourceAsStream(messagesPropertiesFileName);
			Properties prop = new Properties();
			InputStreamReader streamReader = new InputStreamReader(inputStream, "UTF-8");
			prop.load(streamReader);
			setStatusComment(transactionEntityList, dtoList, prop);
			streamReader.close();
			inputStream.close();
		} catch (DataAccessLayerException e) {
			throw new TransactionTableNotAccessibleException(
					PlatformErrorMessages.RPR_RGS_TRANSACTION_TABLE_NOT_ACCESSIBLE.getMessage(), e);
		} catch (IOException e) {
			throw new RegTransactionAppException(PlatformErrorMessages.RPR_RTS_UNKNOWN_EXCEPTION.getCode(),
					PlatformErrorMessages.RPR_RTS_UNKNOWN_EXCEPTION.getMessage() + " -->" + e.getMessage());
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), regId,
				"TransactionServiceImpl::getTransactionByRegId()::exit");
		return dtoList;
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
				entity.getSubStatusCode());

	}

	private RegistrationTransactionDto convertEntityToRegistrationTransactionDto(TransactionEntity entity) {
		return new RegistrationTransactionDto(entity.getId(), entity.getRegistrationId(), entity.getTrntypecode(),
				entity.getParentid(), entity.getStatusCode(), entity.getStatusComment(), entity.getCreateDateTime());

	}

	private void setStatusComment(List<TransactionEntity> transactionEntityList,
			List<RegistrationTransactionDto> dtoList, Properties prop) {
		for (TransactionEntity transactionEntity : transactionEntityList) {
			if (transactionEntity.getSubStatusCode() != null && !transactionEntity.getSubStatusCode().isEmpty()) {
				transactionEntity.setStatusComment(prop.getProperty(transactionEntity.getSubStatusCode()));
			}
			if (transactionEntity.getStatusCode() != null && !transactionEntity.getStatusCode().isEmpty()) {
				transactionEntity.setStatusCode(prop.getProperty(transactionEntity.getStatusCode()));
			}
			if (transactionEntity.getTrntypecode() != null && !transactionEntity.getTrntypecode().isEmpty()) {
				transactionEntity.setTrntypecode(prop.getProperty(transactionEntity.getTrntypecode()));
			}
			dtoList.add(convertEntityToRegistrationTransactionDto(transactionEntity));
		}
	}
}
