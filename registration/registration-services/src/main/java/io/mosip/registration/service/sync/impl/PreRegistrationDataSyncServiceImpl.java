package io.mosip.registration.service.sync.impl;

import java.io.File;
import java.net.SocketTimeoutException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.NonNull;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.IOException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.FileUtils;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.PreRegistrationDataSyncDAO;
import io.mosip.registration.dto.MainResponseDTO;
import io.mosip.registration.dto.PreRegArchiveDTO;
import io.mosip.registration.dto.PreRegistrationDTO;
import io.mosip.registration.dto.PreRegistrationDataSyncDTO;
import io.mosip.registration.dto.PreRegistrationDataSyncRequestDTO;
import io.mosip.registration.dto.PreRegistrationIdsDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.entity.PreRegistrationList;
import io.mosip.registration.entity.SyncTransaction;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.jobs.SyncManager;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.external.PreRegZipHandlingService;
import io.mosip.registration.service.sync.PreRegistrationDataSyncService;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;

import javax.annotation.PreDestroy;

/**
 * Implementation for {@link PreRegistrationDataSyncService}
 * 
 * It interfaces with external pre-registration data sync services and download
 * the packets based on the date range and packet id then store it into the
 * local machine in encrypted format. It also maintain the records in local
 * database along with the key used for encryption.
 * 
 * This is invoked from job scheduler and new registration demographic screen.
 * Job scheduler - download the pre-registration packets between the date range
 * based on value configured in the properties. New Registration screen -
 * download a particular packet from MOSIP server if online connectivity exists,
 * otherwise use the packet from local file system.
 * 
 * @author YASWANTH S
 * @since 1.0.0
 */
@Service
public class PreRegistrationDataSyncServiceImpl extends BaseService implements PreRegistrationDataSyncService {

	/**
	 * Instance of LOGGER
	 */
	private static final Logger LOGGER = AppConfig.getLogger(PreRegistrationDataSyncServiceImpl.class);

	@Autowired
	PreRegistrationDataSyncDAO preRegistrationDAO;

	@Autowired
	SyncManager syncManager;

	@Autowired
	private PreRegZipHandlingService preRegZipHandlingService;

	ExecutorService executorServiceForPreReg = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	@PreDestroy
	public void destroy() {
		try {
			executorServiceForPreReg.shutdown();
			executorServiceForPreReg.awaitTermination(500, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOGGER.error("Failed to shutdown pre-reg executor service", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.sync.PreRegistrationDataSyncService#
	 * getPreRegistrationIds(java.lang.String)
	 */
	@Override
	public ResponseDTO getPreRegistrationIds(@NonNull String syncJobId) {
		LOGGER.info("Fetching Pre-Registration Id's started, syncJobId : {}", syncJobId);
		ResponseDTO responseDTO = new ResponseDTO();
		boolean noRecordsError = false;

		try {
			//Precondition check, proceed only if met, otherwise throws exception
			proceedWithMasterAndKeySync(syncJobId);

			/* REST call to get Pre Registartion Id's */
			LinkedHashMap<String, Object> response = (LinkedHashMap<String, Object>) serviceDelegateUtil.post(RegistrationConstants.GET_PRE_REGISTRATION_IDS,
					prepareDataSyncRequestDTO(), syncJobId);

			MainResponseDTO<PreRegistrationIdsDTO> mainResponseDTO = new ObjectMapper()
					.convertValue(response, new TypeReference<MainResponseDTO<PreRegistrationIdsDTO>>(){});

			//pre-rids received
			if (mainResponseDTO != null && mainResponseDTO.getResponse() != null) {
				PreRegistrationIdsDTO preRegistrationIdsDTO = new ObjectMapper().readValue(
						new JSONObject(mainResponseDTO.getResponse()).toString(), PreRegistrationIdsDTO.class);
				Map<String, String> preRegIds = (Map<String, String>) preRegistrationIdsDTO.getPreRegistrationIds();
				getPreRegistrationPackets(preRegIds);
				LOGGER.info("Fetching Pre-Registration data ended successfully");
				return responseDTO;
			}

			if(mainResponseDTO != null && mainResponseDTO.getErrors() != null &&
					mainResponseDTO.getErrors().stream().anyMatch(e -> e.getErrorCode() != null && e.getErrorCode().equals("PRG_BOOK_RCI_032"))) {
				return setSuccessResponse(responseDTO, RegistrationConstants.PRE_REG_SUCCESS_MESSAGE, null);
			}

		} catch (HttpClientErrorException | ResourceAccessException | HttpServerErrorException
				| RegBaseCheckedException | java.io.IOException exception) {
			LOGGER.error("PRE_REGISTRATION_DATA_SYNC", exception);
		}

		setErrorResponse(responseDTO, RegistrationConstants.PRE_REG_TO_GET_ID_ERROR, null);
		return responseDTO;
	}

	/**
	 * Gets the pre registration packets.
	 *
	 * @param preRegIds   the pre-registration id's
	 */
	private void getPreRegistrationPackets(Map<String, String> preRegIds) {
		LOGGER.info("Fetching Pre-Registration ID's in parallel mode started");
		/* Get Packets Using pre registration ID's */
		for (Entry<String, String> preRegDetail : preRegIds.entrySet()) {
			try {
				executorServiceForPreReg.execute(
						new Runnable() {
							public void run() {
								//TODO - Need to inform pre-reg team to correct date format
								preRegDetail.setValue(preRegDetail.getValue().endsWith("Z") ? preRegDetail.getValue() : preRegDetail.getValue() + "Z");
								getPreRegistration(preRegDetail.getKey(), Timestamp.from(Instant.parse(preRegDetail.getValue())));
							}
						}
				);
			} catch (Exception ex) {
				LOGGER.error("Failed to fetch pre-reg packet", ex);
			}
		}
		LOGGER.info("Added Pre-Registration packet fetch task in parallel mode completed");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.sync.PreRegistrationDataSyncService#
	 * getPreRegistration(java.lang.String)
	 */
	@Override
	public ResponseDTO getPreRegistration(@NonNull String preRegistrationId, boolean forceDownload) {
		ResponseDTO responseDTO = new ResponseDTO();
		try {
			PreRegistrationList preRegistration = preRegistrationDAO.get(preRegistrationId);
			preRegistration = getPreRegistration(preRegistrationId, preRegistration == null ? null :
					forceDownload ? null : preRegistration.getLastUpdatedPreRegTimeStamp());

			if (preRegistration != null) {
				byte[] decryptedPacket = preRegZipHandlingService.decryptPreRegPacket(
						preRegistration.getPacketSymmetricKey(),
						FileUtils.readFileToByteArray(FileUtils.getFile(preRegistration.getPacketPath())));
				setPacketToResponse(responseDTO, decryptedPacket, preRegistrationId);
				return responseDTO;
			}
		} catch (Exception e) {
			LOGGER.error("Failed to fetch pre-reg packet", e);
		}
		setErrorResponse(responseDTO, RegistrationConstants.PRE_REG_TO_GET_PACKET_ERROR, null);
		return responseDTO;
	}

	private PreRegistrationList getPreRegistration(String preRegistrationId, Timestamp lastUpdatedTimeStamp) {
		LOGGER.info("Fetching Pre-Registration started for {}", preRegistrationId);
		PreRegistrationList preRegistration = null;
		try {
			/* Check in Database whether required record already exists or not */
			preRegistration = preRegistrationDAO.get(preRegistrationId);
			if(preRegistration == null) {
				LOGGER.info("Pre-Registration ID is not present downloading {}", preRegistrationId);
				return downloadAndSavePacket(preRegistration, preRegistrationId, lastUpdatedTimeStamp);
			}

			if(lastUpdatedTimeStamp == null ||
					preRegistration.getLastUpdatedPreRegTimeStamp().before(lastUpdatedTimeStamp)) {
				LOGGER.info("Pre-Registration ID is not up-to-date downloading {}", preRegistrationId);
				return downloadAndSavePacket(preRegistration, preRegistrationId, lastUpdatedTimeStamp);
			}

		} catch (Exception exception) {
			LOGGER.error(preRegistrationId, exception);
		}
		return preRegistration;
	}

	private PreRegistrationList downloadAndSavePacket(PreRegistrationList preRegistration, @NonNull String preRegistrationId,
			 Timestamp lastUpdatedTimeStamp) throws Exception {
		Map<String, String> requestParamMap = new HashMap<>();
		requestParamMap.put(RegistrationConstants.PRE_REGISTRATION_ID, preRegistrationId);
		LOGGER.debug("Downloading pre-reg packet {}", requestParamMap);

		LinkedHashMap<String, Object> response = (LinkedHashMap<String, Object>) serviceDelegateUtil.get(RegistrationConstants.GET_PRE_REGISTRATION,
				requestParamMap, true,	RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM);
		MainResponseDTO<PreRegArchiveDTO> mainResponseDTO = new ObjectMapper()
				.convertValue(response, new TypeReference<MainResponseDTO<PreRegArchiveDTO>>() {});

		//successfully downloaded pre-reg packet
		if(mainResponseDTO.getResponse() != null && mainResponseDTO.getResponse().getZipBytes() != null) {
			PreRegistrationDTO preRegistrationDTO = preRegZipHandlingService
					.encryptAndSavePreRegPacket(preRegistrationId, mainResponseDTO.getResponse().getZipBytes());

			// Transaction
			SyncTransaction syncTransaction = syncManager.createSyncTransaction(
					RegistrationConstants.RETRIEVED_PRE_REG_ID, RegistrationConstants.RETRIEVED_PRE_REG_ID,
					RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM, RegistrationConstants.OPT_TO_REG_PDS_J00003);

			// save in Pre-Reg List
			PreRegistrationList preRegistrationList = preparePreRegistration(syncTransaction, preRegistrationDTO);
			preRegistrationList.setAppointmentDate(DateUtils.parseUTCToDate(mainResponseDTO.getResponse().getAppointmentDate(),
					"yyyy-MM-dd"));

			preRegistrationList.setLastUpdatedPreRegTimeStamp(lastUpdatedTimeStamp == null ?
					Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()) : lastUpdatedTimeStamp);

			if (preRegistration == null) {
				preRegistration = preRegistrationDAO.save(preRegistrationList);
			} else {
				preRegistrationList.setId(preRegistration.getId());
				preRegistrationList.setUpdBy(getUserIdFromSession());
				preRegistrationList.setUpdDtimes(new Timestamp(System.currentTimeMillis()));
				preRegistration = preRegistrationDAO.update(preRegistrationList);
			}
		}
		return preRegistration;
	}


	/**
	 * Sets the packet to response.
	 *
	 * @param responseDTO       the response DTO
	 * @param decryptedPacket   the decrypted packet
	 * @param preRegistrationId the pre registration id
	 */
	@SuppressWarnings("unused")
	private void setPacketToResponse(ResponseDTO responseDTO, byte[] decryptedPacket, String preRegistrationId) {

		try {
			/* create attributes */
			RegistrationDTO registrationDTO = preRegZipHandlingService.extractPreRegZipFile(decryptedPacket);
			registrationDTO.setPreRegistrationId(preRegistrationId);
			Map<String, Object> attributes = new WeakHashMap<>();
			attributes.put("registrationDto", registrationDTO);
			setSuccessResponse(responseDTO, RegistrationConstants.PRE_REG_SUCCESS_MESSAGE, attributes);
		} catch (RegBaseCheckedException exception) {
			LOGGER.info("REGISTRATION - PRE_REGISTRATION_DATA_SYNC - PRE_REGISTRATION_DATA_SYNC_SERVICE_IMPL",
					RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					exception.getMessage());
			setErrorResponse(responseDTO, RegistrationConstants.PRE_REG_TO_GET_PACKET_ERROR, null);
		}

	}

	/**
	 * Prepare data sync request DTO.
	 *
	 * @return the pre registration data sync DTO
	 */
	private PreRegistrationDataSyncDTO prepareDataSyncRequestDTO() {

		// prepare required DTO to send through API
		PreRegistrationDataSyncDTO preRegistrationDataSyncDTO = new PreRegistrationDataSyncDTO();

		Timestamp reqTime = new Timestamp(System.currentTimeMillis());
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

		preRegistrationDataSyncDTO.setId(RegistrationConstants.PRE_REGISTRATION_DUMMY_ID);
		preRegistrationDataSyncDTO.setRequesttime(dateFormat.format(reqTime));
		preRegistrationDataSyncDTO.setVersion(RegistrationConstants.VER);

		PreRegistrationDataSyncRequestDTO preRegistrationDataSyncRequestDTO = new PreRegistrationDataSyncRequestDTO();
		if (SessionContext.isSessionContextAvailable()) {
			preRegistrationDataSyncRequestDTO.setRegistrationCenterId(
					SessionContext.userContext().getRegistrationCenterDetailDTO().getRegistrationCenterId());
		} else {
			preRegistrationDataSyncRequestDTO.setRegistrationCenterId(getCenterId());
		}
		preRegistrationDataSyncRequestDTO.setFromDate(getFromDate(reqTime));
		preRegistrationDataSyncRequestDTO.setToDate(getToDate(reqTime));
		//preRegistrationDataSyncRequestDTO.setUserId(getUserIdFromSession());

		preRegistrationDataSyncDTO.setDataSyncRequestDto(preRegistrationDataSyncRequestDTO);

		return preRegistrationDataSyncDTO;

	}

	/**
	 * Gets the to date.
	 *
	 * @param reqTime the req time
	 * @return to date
	 */
	private String getToDate(Timestamp reqTime) {

		Calendar cal = Calendar.getInstance();
		cal.setTime(reqTime);
		cal.add(Calendar.DATE,
				Integer.parseInt(String.valueOf(getGlobalConfigValueOf(RegistrationConstants.PRE_REG_DAYS_LIMIT))));

		/** To-Date */
		return formatDate(cal);

	}

	/**
	 * Format date.
	 *
	 * @param cal the cal
	 * @return the string
	 */
	private String formatDate(Calendar cal) {
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");// dd/MM/yyyy
		Date toDate = cal.getTime();

		/** To-Date */
		return sdfDate.format(toDate);
	}

	/**
	 * Gets the from date.
	 *
	 * @param reqTime the req time
	 * @return the from date
	 */
	private String getFromDate(Timestamp reqTime) {

		Calendar cal = Calendar.getInstance();
		cal.setTime(reqTime);

		return formatDate(cal);
	}

	/**
	 * Prepare pre registration.
	 *
	 * @param syncTransaction      the sync transaction
	 * @param preRegistrationDTO   the pre registration DTO
	 * @return the pre registration list
	 */
	private PreRegistrationList preparePreRegistration(SyncTransaction syncTransaction,
			PreRegistrationDTO preRegistrationDTO) {

		PreRegistrationList preRegistrationList = new PreRegistrationList();

		preRegistrationList.setId(UUID.randomUUID().toString());
		preRegistrationList.setPreRegId(preRegistrationDTO.getPreRegId());
		// preRegistrationList.setAppointmentDate(preRegistrationDTO.getAppointmentDate());
		preRegistrationList.setPacketSymmetricKey(preRegistrationDTO.getSymmetricKey());
		preRegistrationList.setStatusCode(syncTransaction.getStatusCode());
		preRegistrationList.setStatusComment(syncTransaction.getStatusComment());
		preRegistrationList.setPacketPath(preRegistrationDTO.getPacketPath());
		preRegistrationList.setsJobId(syncTransaction.getSyncJobId());
		preRegistrationList.setSynctrnId(syncTransaction.getId());
		preRegistrationList.setLangCode(syncTransaction.getLangCode());
		preRegistrationList.setIsActive(true);
		preRegistrationList.setIsDeleted(false);
		preRegistrationList.setCrBy(syncTransaction.getCrBy());
		preRegistrationList.setCrDtime(new Timestamp(System.currentTimeMillis()));
		return preRegistrationList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.sync.PreRegistrationDataSyncService#
	 * fetchAndDeleteRecords()
	 */
	public synchronized ResponseDTO fetchAndDeleteRecords() {

		LOGGER.info(
				"REGISTRATION - PRE_REGISTRATION_DATA_DELETION_RECORD_FETCH_STARTED - PRE_REGISTRATION_DATA_SYNC_SERVICE_IMPL",
				RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"Fetching the records started");

		ResponseDTO responseDTO = new ResponseDTO();
		if (getGlobalConfigValueOf(RegistrationConstants.PRE_REG_DELETION_CONFIGURED_DAYS) != null) {

			// Set the Date 15 days before the current date
			Calendar startCal = Calendar.getInstance();
			startCal.add(Calendar.DATE, -(Integer
					.parseInt(getGlobalConfigValueOf(RegistrationConstants.PRE_REG_DELETION_CONFIGURED_DAYS))));

			Date startDate = Date.from(startCal.toInstant());

			// fetch the records that needs to be deleted
			List<PreRegistrationList> preRegList = preRegistrationDAO.fetchRecordsToBeDeleted(startDate);

			LOGGER.info(
					"REGISTRATION - PRE_REGISTRATION_DATA_DELETION_RECORD_FETCH_ENDED - PRE_REGISTRATION_DATA_SYNC_SERVICE_IMPL",
					RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					"Fetching the records ended");

			deletePreRegRecords(responseDTO, preRegList);
		}

		return responseDTO;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.sync.PreRegistrationDataSyncService#
	 * deletePreRegRecords(io.mosip.registration.dto.ResponseDTO, java.util.List)
	 */
	public void deletePreRegRecords(ResponseDTO responseDTO, final List<PreRegistrationList> preRegList) {
		LOGGER.info("REGISTRATION - PRE_REGISTRATION_DATA_DELETION_STARTED - PRE_REGISTRATION_DATA_SYNC_SERVICE_IMPL",
				RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"Deletion of records started");

		if (!isNull(preRegList) && !isEmpty(preRegList)) {

			/* Registartions to be deleted */
			List<PreRegistrationList> preRegistartionsToBeDeletedList = new LinkedList<>();

			for (PreRegistrationList preRegRecord : preRegList) {

				if (null != preRegRecord) {
					/* Get File to be deleted from pre registartion */
					File preRegPacket = FileUtils.getFile(preRegRecord.getPacketPath());
					if (preRegPacket.exists() && preRegPacket.delete()) {
						preRegistartionsToBeDeletedList.add(preRegRecord);
					}}

			}

			if (!isEmpty(preRegistartionsToBeDeletedList)) {
				deleteRecords(responseDTO, preRegistartionsToBeDeletedList);
			} else {
				/* Set Error response */
				setErrorResponse(responseDTO, RegistrationConstants.PRE_REG_DELETE_FAILURE, null);

			}
		} else {
			setSuccessResponse(responseDTO, RegistrationConstants.PRE_REG_DELETE_SUCCESS, null);
		}

		LOGGER.info("REGISTRATION - PRE_REGISTRATION_DATA_DELETION_ENDED - PRE_REGISTRATION_DATA_SYNC_SERVICE_IMPL",
				RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"Deletion of records Ended");

	}

	/**
	 * Delete records.
	 *
	 * @param responseDTO the response DTO
	 * @param preRegList  the pre reg list
	 * @return the response DTO
	 */
	private ResponseDTO deleteRecords(ResponseDTO responseDTO, List<PreRegistrationList> preRegList) {

		LOGGER.info(
				"REGISTRATION - PRE_REGISTRATION_DATA_DELETION_UPDATE_STARTED - PRE_REGISTRATION_DATA_SYNC_SERVICE_IMPL",
				RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"deleted records started");

		try {

			/* Delete All Pre Registartions which were under to be deleted state */
			preRegistrationDAO.deleteAll(preRegList);

			/* Set Success Response */
			setSuccessResponse(responseDTO, RegistrationConstants.PRE_REG_DELETE_SUCCESS, null);
		} catch (RuntimeException runtimeException) {
			LOGGER.info("REGISTRATION - PRE_REGISTRATION_DELETE - PRE_REGISTRATION_DATA_SYNC_SERVICE_IMPL",
					RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					runtimeException.getMessage());
			/* Set Error response */
			setErrorResponse(responseDTO, RegistrationConstants.PRE_REG_DELETE_FAILURE, null);
		}

		LOGGER.info(
				"REGISTRATION - PRE_REGISTRATION_DATA_DELETION_UPDATE_ENDED - PRE_REGISTRATION_DATA_SYNC_SERVICE_IMPL",
				RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID, "deleted records ended");

		return responseDTO;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.sync.PreRegistrationDataSyncService#
	 * getPreRegistrationRecordForDeletion(java.lang.String)
	 */
	public PreRegistrationList getPreRegistrationRecordForDeletion(String preRegistrationId) {
		LOGGER.info(
				"REGISTRATION - PRE_REGISTRATION_DATA_DELETION_UPDATE_STARTED - PRE_REGISTRATION_DATA_SYNC_SERVICE_IMPL",
				RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"Fetching pre registration records for deletion");
		if (StringUtils.isEmpty(preRegistrationId)) {
			LOGGER.error("REGISTRATION - PRE_REGISTRATION_DATA_SYNC - PRE_REGISTRATION_DATA_SYNC_SERVICE_IMPL",
					RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					"The PreRegistrationId is empty");
		}
		return preRegistrationDAO.get(preRegistrationId);
	}

	/* (non-Javadoc)
	 * @see io.mosip.registration.service.sync.PreRegistrationDataSyncService#lastPreRegPacketDownloadedTime()
	 */
	@Override
	public Timestamp getLastPreRegPacketDownloadedTime() {
		return preRegistrationDAO.getLastPreRegPacketDownloadedTime();
	}

}
