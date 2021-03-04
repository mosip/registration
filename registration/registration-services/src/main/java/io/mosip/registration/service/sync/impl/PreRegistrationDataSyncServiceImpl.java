package io.mosip.registration.service.sync.impl;

import java.io.File;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

	@Autowired
	PreRegistrationDataSyncDAO preRegistrationDAO;

	@Autowired
	SyncManager syncManager;

	@Autowired
	private PreRegZipHandlingService preRegZipHandlingService;

	/**
	 * Instance of LOGGER
	 */
	private static final Logger LOGGER = AppConfig.getLogger(PreRegistrationDataSyncServiceImpl.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.sync.PreRegistrationDataSyncService#
	 * getPreRegistrationIds(java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	@Override
	synchronized public ResponseDTO getPreRegistrationIds(String syncJobId) {
		LOGGER.info("REGISTRATION - PRE_REGISTRATION_DATA_SYNC - PRE_REGISTRATION_DATA_SYNC_SERVICE_IMPL",
				RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"Fetching Pre-Registration Id's started");
		ResponseDTO responseDTO = new ResponseDTO();
		if(StringUtils.isEmpty(syncJobId)) {
			setErrorResponse(responseDTO, RegistrationConstants.PRE_REG_TO_GET_ID_ERROR, null);
			return responseDTO;
		}

		/* prepare request DTO to pass on through REST call */
		PreRegistrationDataSyncDTO preRegistrationDataSyncDTO = prepareDataSyncRequestDTO();

		try {

			//Precondition check, proceed only if met, otherwise throws exception
			proceedWithMasterAndKeySync(syncJobId);

			/* REST call to get Pre Registartion Id's */
			LinkedHashMap<String, Object> response = (LinkedHashMap<String, Object>) serviceDelegateUtil
					.post(RegistrationConstants.GET_PRE_REGISTRATION_IDS, preRegistrationDataSyncDTO, syncJobId);
			TypeReference<MainResponseDTO<LinkedHashMap<String, Object>>> ref = new TypeReference<MainResponseDTO<LinkedHashMap<String, Object>>>() {
			};
			MainResponseDTO<LinkedHashMap<String, Object>> mainResponseDTO = new ObjectMapper()
					.readValue(new JSONObject(response).toString(), ref);

			if (isResponseNotEmpty(mainResponseDTO)) {
				PreRegistrationIdsDTO preRegistrationIdsDTO = new ObjectMapper().readValue(
						new JSONObject(mainResponseDTO.getResponse()).toString(), PreRegistrationIdsDTO.class);
				Map<String, String> preRegIds = (Map<String, String>) preRegistrationIdsDTO.getPreRegistrationIds();
				getPreRegistrationPackets(syncJobId, responseDTO, preRegIds);
				return responseDTO;
			}

			if(mainResponseDTO != null && mainResponseDTO.getErrors() != null) {
				//TODO - based on error code instead of error message
				boolean noRecords = mainResponseDTO.getErrors()
						.stream().anyMatch(e -> e.getMessage() != null &&
						e.getMessage().equalsIgnoreCase("Record not found for date range and reg center id"));

				return noRecords ? setSuccessResponse(responseDTO, RegistrationConstants.PRE_REG_SUCCESS_MESSAGE, null) :
						setErrorResponse(responseDTO, RegistrationConstants.PRE_REG_TO_GET_ID_ERROR, null);
			}

		} catch (HttpClientErrorException | ResourceAccessException | HttpServerErrorException
				| RegBaseCheckedException | java.io.IOException exception) {
			LOGGER.error(RegistrationConstants.APPLICATION_NAME,RegistrationConstants.APPLICATION_ID,
					"PRE_REGISTRATION_DATA_SYNC", ExceptionUtils.getStackTrace(exception));
			setErrorResponse(responseDTO, RegistrationConstants.PRE_REG_TO_GET_ID_ERROR, null);
		}


		LOGGER.info("REGISTRATION - PRE_REGISTRATION_DATA_SYNC - PRE_REGISTRATION_DATA_SYNC_SERVICE_IMPL",
				RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"Fetching Pre-Registration Id's ended");
		return responseDTO;
	}

	/**
	 * Gets the pre registration packets.
	 *
	 * @param syncJobId   the sync job id
	 * @param responseDTO the response DTO
	 * @param preRegIds   the pre-registration id's
	 */
	private void getPreRegistrationPackets(String syncJobId, ResponseDTO responseDTO, Map<String, String> preRegIds) {
		ExecutorService executorServiceForPreReg = Executors.newFixedThreadPool(5);
		try {
			LOGGER.info("REGISTRATION - PRE_REGISTRATION_DATA_SYNC - PRE_REGISTRATION_DATA_SYNC_SERVICE_IMPL",
					RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					"Fetching Pre-Registration ID's in parallel mode started");
			/* Get Packets Using pre registration ID's */
			for (Entry<String, String> preRegDetail : preRegIds.entrySet()) {
				executorServiceForPreReg.execute(
						new Runnable() {
								public void run() {
										preRegDetail.setValue(preRegDetail.getValue().contains("Z") ? preRegDetail.getValue() : preRegDetail.getValue() + "Z");
					
										getPreRegistration(responseDTO, preRegDetail.getKey(), syncJobId, Timestamp.from(Instant.parse(preRegDetail.getValue())));
								}
						}
				);
			}
			
			executorServiceForPreReg.shutdown();
			executorServiceForPreReg.awaitTermination(500, TimeUnit.SECONDS);
		} catch (Exception interruptedException) {
			executorServiceForPreReg.shutdown();
			LOGGER.error("REGISTRATION - PRE_REGISTRATION_DATA_SYNC - PRE_REGISTRATION_DATA_SYNC_SERVICE_IMPL",
					RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					"Error Fetching Pre-Registration ID's in parallel mode " + interruptedException);
		}
		LOGGER.info("REGISTRATION - PRE_REGISTRATION_DATA_SYNC - PRE_REGISTRATION_DATA_SYNC_SERVICE_IMPL",
				RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"Fetching Pre-Registration ID's in parallel mode completed");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.sync.PreRegistrationDataSyncService#
	 * getPreRegistration(java.lang.String)
	 */
	@Override
	public ResponseDTO getPreRegistration(String preRegistrationId) {

		LOGGER.info("REGISTRATION - PRE_REGISTRATION_DATA_SYNC - PRE_REGISTRATION_DATA_SYNC_SERVICE_IMPL",
				RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"Fetching Pre-Registration started");

		ResponseDTO responseDTO = new ResponseDTO();

		if (!StringUtils.isEmpty(preRegistrationId)) {
			/** Get Pre Registration Packet */
			getPreRegistration(responseDTO, preRegistrationId, null, null);
		} else {
			LOGGER.error("REGISTRATION - PRE_REGISTRATION_DATA_SYNC - PRE_REGISTRATION_DATA_SYNC_SERVICE_IMPL",
					RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					"The PreRegistrationId is empty");

			/* set Error response */
			setErrorResponse(responseDTO, RegistrationConstants.PRE_REG_TO_GET_PACKET_ERROR, null);
		}

		LOGGER.info("REGISTRATION - PRE_REGISTRATION_DATA_SYNC - PRE_REGISTRATION_DATA_SYNC_SERVICE_IMPL",
				RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"Fetching Pre-Registration completed");

		return responseDTO;
	}

	/**
	 * Gets the pre registration.
	 *
	 * @param responseDTO          the response DTO
	 * @param preRegistrationId    the pre registration id
	 * @param syncJobId            the sync job id
	 * @param lastUpdatedTimeStamp the last updated time stamp
	 * @return the pre registration
	 */
	@SuppressWarnings("unchecked")
	private void getPreRegistration(ResponseDTO responseDTO, String preRegistrationId, String syncJobId,
			Timestamp lastUpdatedTimeStamp) {

		LOGGER.info("REGISTRATION - PRE_REGISTRATION_DATA_SYNC - PRE_REGISTRATION_DATA_SYNC_SERVICE_IMPL",
				RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"Fetching Pre-Registration started");

		/* Check in Database whether required record already exists or not */
		PreRegistrationList preRegistration = preRegistrationDAO.get(preRegistrationId);

		/* Check Network Connectivity */
		boolean isOnline = RegistrationAppHealthCheckUtil.isNetworkAvailable();

		/* check if the packet is not available in db and the machine is offline */
		if (isPacketNotAvailable(preRegistration, isOnline)) {
			setErrorResponse(responseDTO, RegistrationConstants.PRE_REG_PACKET_NETWORK_ERROR, null);
			return;
		}

		boolean isUpdated = false;
		byte[] decryptedPacket = null;
		boolean isFetchFromUi = false;

		isUpdated = isUpdated(lastUpdatedTimeStamp, preRegistration, isUpdated);


		if (syncJobId == null) {
			isFetchFromUi = true;
			syncJobId = RegistrationConstants.JOB_TRIGGER_POINT_USER;

		}

		boolean isJob = (!RegistrationConstants.JOB_TRIGGER_POINT_USER.equals(syncJobId));

		/*
		 * Get Packet From REST call when the packet is updated in the server or always
		 * if its a manual trigger
		 */
		if (isFetchToBeTriggered(isOnline, isUpdated, isJob)) {

			/* prepare request params to pass through URI */
			Map<String, String> requestParamMap = new HashMap<>();
			requestParamMap.put(RegistrationConstants.PRE_REGISTRATION_ID, preRegistrationId);

			String triggerPoint = getTriggerPoint(isJob);

			try {
				/* REST call to get packet */
				LinkedHashMap<String, Object> mainResponseDTO = (LinkedHashMap<String, Object>) serviceDelegateUtil
						.get(RegistrationConstants.GET_PRE_REGISTRATION, requestParamMap, true, syncJobId);

				if (null != mainResponseDTO
						&& null != mainResponseDTO.get(RegistrationConstants.RESPONSE)) {

					PreRegArchiveDTO preRegArchiveDTO = new ObjectMapper().readValue(
							new ObjectMapper().writeValueAsString(
									mainResponseDTO.get(RegistrationConstants.RESPONSE)),
							PreRegArchiveDTO.class);

					decryptedPacket = preRegArchiveDTO.getZipBytes();

					/* Get PreRegistrationDTO by taking packet Information */
					PreRegistrationDTO preRegistrationDTO = preRegZipHandlingService
							.encryptAndSavePreRegPacket(preRegistrationId, decryptedPacket);

					// Transaction
					SyncTransaction syncTransaction = syncManager.createSyncTransaction(
							RegistrationConstants.RETRIEVED_PRE_REG_ID, RegistrationConstants.RETRIEVED_PRE_REG_ID,
							triggerPoint, syncJobId);

					// save in Pre-Reg List
					PreRegistrationList preRegistrationList = preparePreRegistration(syncTransaction,
							preRegistrationDTO, lastUpdatedTimeStamp);

					preRegistrationList.setAppointmentDate(
							DateUtils.parseUTCToDate(preRegArchiveDTO.getAppointmentDate(), "yyyy-MM-dd"));

					if (preRegistration == null) {
						preRegistrationDAO.save(preRegistrationList);
					} else {
						preRegistrationList.setId(preRegistration.getId());
						preRegistrationList.setUpdBy(getUserIdFromSession());
						preRegistrationList.setUpdDtimes(new Timestamp(System.currentTimeMillis()));
						preRegistrationDAO.update(preRegistrationList);
					}
					/* set success response */
					setSuccessResponse(responseDTO, RegistrationConstants.PRE_REG_SUCCESS_MESSAGE, null);

				} else if (preRegistration == null) {
					/*
					 * set error message if the packet is not available both in db as well as the
					 * REST service
					 */
					setErrorResponse(responseDTO, RegistrationConstants.PRE_REG_TO_GET_PACKET_ERROR, null);
					return;
				}

			} catch (HttpClientErrorException | RegBaseCheckedException | java.io.IOException
					| HttpServerErrorException exception) {

				LOGGER.error("REGISTRATION - PRE_REGISTRATION_DATA_SYNC - PRE_REGISTRATION_DATA_SYNC_SERVICE_IMPL",
						RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
						exception.getMessage() + ExceptionUtils.getStackTrace(exception));

				/* set Error response */
				setErrorResponse(responseDTO, RegistrationConstants.PRE_REG_TO_GET_PACKET_ERROR, null);
				return;
			}
		}

		/* Only for Manual Trigger */
		if (isFetchFromUi) {
			try {
				if (isPacketFromLocal(preRegistration, decryptedPacket)) {
					/*
					 * if the packet is already available,read encrypted packet from disk and
					 * decrypt
					 */
					decryptedPacket = preRegZipHandlingService.decryptPreRegPacket(
							preRegistration.getPacketSymmetricKey(),
							FileUtils.readFileToByteArray(FileUtils.getFile(preRegistration.getPacketPath())));
				}

				/* set decrypted packet into Response */
				setPacketToResponse(responseDTO, decryptedPacket, preRegistrationId);

			} catch (IOException exception) {
				LOGGER.error("REGISTRATION - PRE_REGISTRATION_DATA_SYNC - Manual Trigger",
						RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
						exception.getMessage() + ExceptionUtils.getStackTrace(exception));
				setErrorResponse(responseDTO, RegistrationConstants.PRE_REG_TO_GET_PACKET_ERROR, null);
				return;
			} catch (RegBaseUncheckedException exception) {
				LOGGER.error("REGISTRATION - PRE_REGISTRATION_DATA_SYNC - Manual Trigger",
						RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
						exception.getMessage() + ExceptionUtils.getStackTrace(exception));
				setErrorResponse(responseDTO, RegistrationConstants.PRE_REG_TO_GET_PACKET_ERROR, null);
				return;
			}

		}

		LOGGER.info("REGISTRATION - PRE_REGISTRATION_DATA_SYNC - PRE_REGISTRATION_DATA_SYNC_SERVICE_IMPL",
				RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"Get Pre-Registration ended");

	}

	private boolean isUpdated(Timestamp lastUpdatedTimeStamp, PreRegistrationList preRegistration, boolean isUpdated) {
		if (isPacketUpdatedInServer(preRegistration)) {

			isUpdated = (preRegistration.getLastUpdatedPreRegTimeStamp().equals(lastUpdatedTimeStamp));
		}
		return isUpdated;
	}

	/**
	 * Checks if is packet not available.
	 *
	 * @param preRegistration the pre registration
	 * @param isOnline        the is online
	 * @return true, if is packet not available
	 */
	private boolean isPacketNotAvailable(PreRegistrationList preRegistration, boolean isOnline) {
		return !isOnline && preRegistration == null;
	}

	/**
	 * Checks if is packet from local.
	 *
	 * @param preRegistration the pre registration
	 * @param decryptedPacket the decrypted packet
	 * @return true, if is packet from local
	 */
	private boolean isPacketFromLocal(PreRegistrationList preRegistration, byte[] decryptedPacket) {
		return preRegistration != null && decryptedPacket == null;
	}

	/**
	 * Checks if is packet updated in server.
	 *
	 * @param preRegistration the pre registration
	 * @return true, if is packet updated in server
	 */
	private boolean isPacketUpdatedInServer(PreRegistrationList preRegistration) {
		return preRegistration != null && preRegistration.getLastUpdatedPreRegTimeStamp() != null;
	}

	/**
	 * Checks if is fetch to be triggered.
	 *
	 * @param isOnline  the is online
	 * @param isUpdated the is updated
	 * @param isJob     the is job
	 * @return true, if is fetch to be triggered
	 */
	private boolean isFetchToBeTriggered(boolean isOnline, boolean isUpdated, boolean isJob) {
		return isOnline && (!isUpdated || !isJob);
	}

	/**
	 * Gets the trigger point.
	 *
	 * @param isJob the is job
	 * @return the trigger point
	 */
	private String getTriggerPoint(boolean isJob) {
		return isJob ? RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM : getUserIdFromSession();
	}

	/**
	 * Checks if is response not empty.
	 *
	 * @param mainResponseDTO the main response DTO
	 * @return true, if is response not empty
	 */
	private boolean isResponseNotEmpty(MainResponseDTO<LinkedHashMap<String, Object>> mainResponseDTO) {
		return mainResponseDTO != null && mainResponseDTO.getResponse() != null;
	}

	/**
	 * Checks if is packet not empty.
	 *
	 * @param mainResponseDTO the main response DTO
	 * @return true, if is packet not empty
	 */
	private boolean isPacketNotEmpty(MainResponseDTO<LinkedHashMap<String, Object>> mainResponseDTO) {
		return isResponseNotEmpty(mainResponseDTO) && mainResponseDTO.getResponse().get("zip-bytes") != null;
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
		preRegistrationDataSyncRequestDTO.setFromDate(getFromDate(reqTime));
		if (SessionContext.isSessionContextAvailable()) {
			preRegistrationDataSyncRequestDTO.setRegistrationCenterId(
					SessionContext.userContext().getRegistrationCenterDetailDTO().getRegistrationCenterId());
		} else {
			preRegistrationDataSyncRequestDTO.setRegistrationCenterId(getCenterId());
		}
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
	 * @param lastUpdatedTimeStamp the last updated time stamp
	 * @return the pre registration list
	 */
	private PreRegistrationList preparePreRegistration(SyncTransaction syncTransaction,
			PreRegistrationDTO preRegistrationDTO, Timestamp lastUpdatedTimeStamp) {

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
		preRegistrationList.setLastUpdatedPreRegTimeStamp(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));		
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
