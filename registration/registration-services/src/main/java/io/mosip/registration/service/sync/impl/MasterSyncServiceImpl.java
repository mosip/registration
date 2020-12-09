package io.mosip.registration.service.sync.impl;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_MASTER_SYNC;
import static io.mosip.registration.constants.LoggerConstants.LOG_REG_SCHEMA_SYNC;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.registration.dao.DynamicFieldDAO;
import io.mosip.registration.dto.mastersync.*;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.core.type.TypeReference;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dao.IdentitySchemaDao;
import io.mosip.registration.dao.MachineMappingDAO;
import io.mosip.registration.dao.MasterSyncDao;
import io.mosip.registration.dto.IndividualTypeDto;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.response.SchemaDto;
import io.mosip.registration.dto.response.SyncDataResponseDto;
import io.mosip.registration.entity.BiometricAttribute;
import io.mosip.registration.entity.BlacklistedWords;
import io.mosip.registration.entity.DocumentType;
import io.mosip.registration.entity.Gender;
import io.mosip.registration.entity.IndividualType;
import io.mosip.registration.entity.Location;
import io.mosip.registration.entity.ReasonCategory;
import io.mosip.registration.entity.ReasonList;
import io.mosip.registration.entity.SyncControl;
import io.mosip.registration.entity.SyncTransaction;
import io.mosip.registration.entity.ValidDocument;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.jobs.SyncManager;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.config.GlobalParamService;
import io.mosip.registration.service.remap.CenterMachineReMapService;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import io.mosip.registration.util.mastersync.MapperUtils;

/**
 * It makes call to the external 'MASTER Sync' services to download the master
 * data which are relevant to center specific by passing the center id or mac
 * address or machine id. Once download the data, it stores the information into
 * the DB for further processing. If center remapping found from the sync
 * response object, it invokes this 'CenterMachineReMapService' object to
 * initiate the center remapping related activities. During the process, the
 * required informations are updated into the audit table for further tracking.
 * 
 * @author Sreekar Chukka
 * @since 1.0.0
 *
 */
@Service
public class MasterSyncServiceImpl extends BaseService implements MasterSyncService {

	private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat(TIMESTAMP_FORMAT);

	/**
	 * The SncTransactionManagerImpl, which Have the functionalities to get the job
	 * and to create sync transaction
	 */
	@Autowired
	protected SyncManager syncManager;

	/** Object for masterSyncDao class. */
	@Autowired
	private MasterSyncDao masterSyncDao;

	/** The machine mapping DAO. */
	@Autowired
	private MachineMappingDAO machineMappingDAO;

	/** The global param service. */
	@Autowired
	private GlobalParamService globalParamService;

	/** The audit factory. */
	@Autowired
	private AuditManagerService auditFactory;

	/** The center machine re map service. */
	@Autowired
	private CenterMachineReMapService centerMachineReMapService;

	@Autowired
	private IdentitySchemaDao identitySchemaDao;

	@Autowired
	private DynamicFieldDAO dynamicFieldDAO;

	@Autowired
	private ClientCryptoFacade clientCryptoFacade;

	/** Object for Logger. */
	private static final Logger LOGGER = AppConfig.getLogger(MasterSyncServiceImpl.class);

	/**
	 * It invokes the Master Sync service to download the required information from
	 * external services if the system is online. Once download, the data would be
	 * updated into the DB for further process.
	 *
	 * @param masterSyncDtls the master sync details
	 * @param triggerPoint   from where the call has been initiated [Either : user
	 *                       or system]
	 * @return success or failure status as Response DTO.
	 * @throws RegBaseCheckedException
	 */
	@Override
	public ResponseDTO getMasterSync(String masterSyncDtls, String triggerPoint) throws RegBaseCheckedException {
		LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID, "Initiating the Master Sync");
		if (masterSyncFieldsValidate(masterSyncDtls, triggerPoint)) {

			ResponseDTO responseDto = syncClientSettings(masterSyncDtls, triggerPoint,
					getRequestParamsForClientSettingsSync(masterSyncDtls, null));

			return responseDto;

		} else {
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					"masterSyncDtls/triggerPoint is mandatory...");
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_MASTER_SYNC_SERVICE_IMPL.getErrorCode(),
					RegistrationExceptionConstants.REG_MASTER_SYNC_SERVICE_IMPL.getErrorMessage());
		}

	}

	/**
	 * It invokes the external 'Master Sync' service to download the required center
	 * specific information from MOSIP server if the system is online. Once
	 * download, the data would be updated into the DB for further process.
	 *
	 * @param masterSyncDtls the master sync details
	 * @param triggerPoint   from where the call has been initiated [Either : user
	 *                       or system]
	 * @param keyIndex       This is the key index provided by the MOSIP server post
	 *                       submission of local TPM public key. Based on this key
	 *                       the MOSIP server would identify the client and send the
	 *                       sync response accordingly.
	 * @return the master sync Success or failure status is wrapped in ResponseDTO.
	 * @throws RegBaseCheckedException
	 */
	@Override
	public ResponseDTO getMasterSync(String masterSyncDtls, String triggerPoint, String keyIndex)
			throws RegBaseCheckedException {
		LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
				"Initiating the Master Sync for initial setup");
		if (masterSyncFieldsValidateWithIndex(masterSyncDtls, triggerPoint, keyIndex)) {
			ResponseDTO responseDTO = syncClientSettings(masterSyncDtls, triggerPoint,
					getRequestParamsForClientSettingsSync(masterSyncDtls, keyIndex));
			return responseDTO;
		} else {
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					"masterSyncDtls/triggerPoint/keyIndex is mandatory...");
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_MASTER_SYNC_SERVICE_IMPL.getErrorCode(),
					RegistrationExceptionConstants.REG_MASTER_SYNC_SERVICE_IMPL.getErrorMessage());
		}
	}

	/**
	 * Find location or region by hierarchy code.
	 *
	 * @param hierarchyLevel the hierarchy code
	 * @param langCode      the lang code
	 * @return the list holds the Location data to be displayed in the UI.
	 * @throws RegBaseCheckedException
	 */
	@Override
	public List<GenericDto> findLocationByHierarchyCode(int hierarchyLevel, String langCode)
			throws RegBaseCheckedException {

		List<GenericDto> locationDto = new ArrayList<>();
		List<Location> masterLocation = masterSyncDao.findLocationByLangCode(hierarchyLevel, langCode);

		for (Location masLocation : masterLocation) {
			GenericDto location = new GenericDto();
			location.setCode(masLocation.getCode());
			location.setName(masLocation.getName());
			location.setLangCode(masLocation.getLangCode());
			locationDto.add(location);
		}
		return locationDto;
	}

	/**
	 * Find proviance by hierarchy code.
	 *
	 * @param code     the code
	 * @param langCode the lang code
	 * @return the list holds the Province data to be displayed in the UI.
	 * @throws RegBaseCheckedException
	 */
	@Override
	public List<GenericDto> findProvianceByHierarchyCode(String code, String langCode) throws RegBaseCheckedException {

		List<GenericDto> locationDto = new ArrayList<>();
		if (codeAndlangCodeNullCheck(code, langCode)) {
			List<Location> masterLocation = masterSyncDao.findLocationByParentLocCode(code, langCode);

			for (Location masLocation : masterLocation) {
				GenericDto location = new GenericDto();
				location.setCode(masLocation.getCode());
				location.setName(masLocation.getName());
				location.setLangCode(masLocation.getLangCode());
				locationDto.add(location);
			}
		} else {
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					RegistrationConstants.CODE_AND_LANG_CODE_MANDATORY);
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_MASTER_SYNC_SERVICE_IMPL_CODE_AND_LANGCODE.getErrorCode(),
					RegistrationExceptionConstants.REG_MASTER_SYNC_SERVICE_IMPL_CODE_AND_LANGCODE.getErrorMessage());
		}
		return locationDto;
	}

	/**
	 * Gets all the reasons for rejection that to be selected during EOD approval
	 * process.
	 *
	 * @param langCode the lang code
	 * @return the all reasons
	 * @throws RegBaseCheckedException
	 */
	@Override
	public List<ReasonListDto> getAllReasonsList(String langCode) throws RegBaseCheckedException {

		List<ReasonListDto> reasonListResponse = new ArrayList<>();
		if (langCodeNullCheck(langCode)) {
			List<String> resonCantCode = new ArrayList<>();
			// Fetting Reason Category
			List<ReasonCategory> masterReasonCatogery = masterSyncDao.getAllReasonCatogery(langCode);
			if (masterReasonCatogery != null && !masterReasonCatogery.isEmpty()) {
				masterReasonCatogery.forEach(reason -> {
					resonCantCode.add(reason.getCode());
				});
			}
			// Fetching reason list based on lang_Code and rsncat_code
			List<ReasonList> masterReasonList = masterSyncDao.getReasonList(langCode, resonCantCode);
			masterReasonList.forEach(reasonList -> {
				ReasonListDto reasonListDto = new ReasonListDto();
				reasonListDto.setCode(reasonList.getCode());
				reasonListDto.setName(reasonList.getName());
				reasonListDto.setRsnCatCode(reasonList.getRsnCatCode());
				reasonListDto.setLangCode(reasonList.getLangCode());
				reasonListResponse.add(reasonListDto);
			});
		} else {
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					RegistrationConstants.LANG_CODE_MANDATORY);
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_MASTER_SYNC_SERVICE_IMPL_LANGCODE.getErrorCode(),
					RegistrationExceptionConstants.REG_MASTER_SYNC_SERVICE_IMPL_LANGCODE.getErrorMessage());
		}
		return reasonListResponse;

	}

	/**
	 * Gets all the black listed words that shouldn't be allowed while capturing
	 * demographic information from user.
	 *
	 * @param langCode the lang code
	 * @return the all black listed words
	 * @throws RegBaseCheckedException
	 */
	@Override
	public List<BlacklistedWordsDto> getAllBlackListedWords(String langCode) throws RegBaseCheckedException {

		List<BlacklistedWordsDto> blackWords = new ArrayList<>();
		if (langCodeNullCheck(langCode)) {
			List<BlacklistedWords> blackListedWords = masterSyncDao.getBlackListedWords(langCode);

			blackListedWords.forEach(blackList -> {

				BlacklistedWordsDto words = new BlacklistedWordsDto();
				words.setDescription(blackList.getDescription());
				words.setLangCode(blackList.getLangCode());
				words.setWord(blackList.getWord());
				blackWords.add(words);

			});
		} else {
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					RegistrationConstants.LANG_CODE_MANDATORY);
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_MASTER_SYNC_SERVICE_IMPL_LANGCODE.getErrorCode(),
					RegistrationExceptionConstants.REG_MASTER_SYNC_SERVICE_IMPL_LANGCODE.getErrorMessage());
		}
		return blackWords;
	}

	/**
	 * Gets the gender details.
	 *
	 * @param langCode the lang code
	 * @return the gender dtls
	 * @throws RegBaseCheckedException
	 */
	@Override
	public List<GenericDto> getGenderDtls(String langCode) throws RegBaseCheckedException {
		List<GenericDto> gendetDtoList = new ArrayList<>();
		if (langCodeNullCheck(langCode)) {
			List<Gender> masterDocuments = masterSyncDao.getGenderDtls(langCode);
			masterDocuments.forEach(gender -> {
				GenericDto comboBox = new GenericDto();
				comboBox.setCode(gender.getCode().trim());
				comboBox.setName(gender.getGenderName().trim());
				comboBox.setLangCode(gender.getLangCode());
				gendetDtoList.add(comboBox);
			});
		} else {
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					RegistrationConstants.LANG_CODE_MANDATORY);
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_MASTER_SYNC_SERVICE_IMPL_LANGCODE.getErrorCode(),
					RegistrationExceptionConstants.REG_MASTER_SYNC_SERVICE_IMPL_LANGCODE.getErrorMessage());
		}
		return gendetDtoList;
	}

	/**
	 * Gets all the document categories from db that to be displayed in the UI
	 * dropdown.
	 *
	 * @param docCode  the doc code
	 * @param langCode the lang code
	 * @return all the document categories
	 * @throws RegBaseCheckedException
	 */
	@Override
	public List<DocumentCategoryDto> getDocumentCategories(String docCode, String langCode)
			throws RegBaseCheckedException {
		List<String> validDocuments = new ArrayList<>();
		List<DocumentCategoryDto> documentsDTO = new ArrayList<>();
		if (codeAndlangCodeNullCheck(docCode, langCode)) {
			List<ValidDocument> masterValidDocuments = masterSyncDao.getValidDocumets(docCode);
			masterValidDocuments.forEach(docs -> {
				validDocuments.add(docs.getDocTypeCode());
			});

			List<DocumentType> masterDocuments = masterSyncDao.getDocumentTypes(validDocuments, langCode);

			masterDocuments.forEach(document -> {

				DocumentCategoryDto documents = new DocumentCategoryDto();
				documents.setCode(document.getCode());
				documents.setDescription(document.getDescription());
				documents.setLangCode(document.getLangCode());
				documents.setName(document.getName());
				documentsDTO.add(documents);

			});
		} else {
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					RegistrationConstants.CODE_AND_LANG_CODE_MANDATORY);
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_MASTER_SYNC_SERVICE_IMPL_CODE_AND_LANGCODE.getErrorCode(),
					RegistrationExceptionConstants.REG_MASTER_SYNC_SERVICE_IMPL_CODE_AND_LANGCODE.getErrorMessage());
		}
		return documentsDTO;
	}


	/**
	 * Gets the individual type.
	 * @param langCode the lang code
	 * @return the individual type
	 * @throws RegBaseCheckedException
	 */
	@Override
	public List<GenericDto> getIndividualType(String langCode) throws RegBaseCheckedException {
		List<GenericDto> listOfIndividualDTO = new ArrayList<>();

		List<IndividualType> masterDocuments = masterSyncDao.getIndividulType(langCode);

		masterDocuments.forEach(individual -> {
			GenericDto individualDto = new GenericDto();
			individualDto.setName(individual.getName());
			individualDto.setCode(individual.getIndividualTypeId().getCode());
			individualDto.setLangCode(individual.getIndividualTypeId().getLangCode());
			listOfIndividualDTO.add(individualDto);
		});
		return listOfIndividualDTO;
	}

	@Override
	public List<GenericDto> getDynamicField(String fieldName, String langCode) throws RegBaseCheckedException {
		List<GenericDto> fieldValues = new ArrayList<>();
		List<DynamicFieldValueDto> syncedValues = dynamicFieldDAO.getDynamicFieldValues(fieldName, langCode);

		if (syncedValues != null) {
			for (DynamicFieldValueDto valueDto : syncedValues) {
				if (valueDto.isActive()) {
					GenericDto genericDto = new GenericDto();
					genericDto.setName(valueDto.getValue());
					genericDto.setCode(valueDto.getCode());
					genericDto.setLangCode(langCode);
					fieldValues.add(genericDto);
				}
			}
		}
		return fieldValues;
	}

	@Override
	public List<GenericDto> getFieldValues(String fieldName, String langCode) throws RegBaseCheckedException {
		switch (fieldName) {
			case "gender": return  getGenderDtls(langCode);
			case "residenceStatus" : return getIndividualType(langCode);
			default: return getDynamicField(fieldName, langCode);
		}
	}

	/**
	 * Gets the biometric type.
	 *
	 * @param langCode the lang code
	 * @return the biometric type
	 * @throws RegBaseCheckedException the reg base checked exception
	 */
	public List<BiometricAttributeDto> getBiometricType(String langCode) throws RegBaseCheckedException {
		List<BiometricAttributeDto> listOfbiometricAttributeDTO = new ArrayList<>();
		if (langCodeNullCheck(langCode)) {
			List<String> biometricType = new LinkedList<>(
					Arrays.asList(RegistrationConstants.FNR, RegistrationConstants.IRS));

			if (RegistrationConstants.DISABLE.equalsIgnoreCase(
					String.valueOf(ApplicationContext.map().get(RegistrationConstants.FINGERPRINT_DISABLE_FLAG)))) {
				biometricType.remove(RegistrationConstants.FNR);
			} else if (RegistrationConstants.DISABLE.equalsIgnoreCase(
					String.valueOf(ApplicationContext.map().get(RegistrationConstants.IRIS_DISABLE_FLAG)))) {
				biometricType.remove(RegistrationConstants.IRS);
			}

			List<BiometricAttribute> masterBiometrics = masterSyncDao.getBiometricType(langCode, biometricType);

			masterBiometrics.forEach(biometrics -> {
				BiometricAttributeDto biometricsDto = new BiometricAttributeDto();
				biometricsDto.setName(biometrics.getName());
				biometricsDto.setCode(biometrics.getCode());
				biometricsDto.setBiometricTypeCode(biometrics.getBiometricTypeCode());
				biometricsDto.setLangCode(biometrics.getLangCode());
				listOfbiometricAttributeDTO.add(biometricsDto);
			});
		} else {
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					RegistrationConstants.LANG_CODE_MANDATORY);
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_MASTER_SYNC_SERVICE_IMPL_LANGCODE.getErrorCode(),
					RegistrationExceptionConstants.REG_MASTER_SYNC_SERVICE_IMPL_LANGCODE.getErrorMessage());
		}
		return listOfbiometricAttributeDTO;

	}

	/**
	 * Error msg.
	 *
	 * @param responseMap
	 * @return the string
	 */
	@SuppressWarnings("unchecked")
	private String errorMsg(LinkedHashMap<String, Object> responseMap) {
		LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID, "Logging error message....");
		String errorMsg = RegistrationConstants.MASTER_SYNC + "-" + RegistrationConstants.MASTER_SYNC_FAILURE_MSG;
		if (null != responseMap && responseMap.size() > 0) {
			List<LinkedHashMap<String, Object>> errorMap = (List<LinkedHashMap<String, Object>>) responseMap
					.get(RegistrationConstants.ERRORS);
			if (null != errorMap.get(0).get(RegistrationConstants.ERROR_MSG)) {
				errorMsg = (String) errorMap.get(0).get(RegistrationConstants.ERROR_MSG);
			}
		}
		LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID, errorMsg);
		return errorMsg;

	}

	/**
	 * Master sync fields validate with index.
	 *
	 * @param masterSyncDtls the master sync dtls
	 * @param triggerPoint   the trigger point
	 * @param keyIndex       the key index
	 * @return true, if successful
	 */
	private boolean masterSyncFieldsValidateWithIndex(String masterSyncDtls, String triggerPoint, String keyIndex) {

		if (StringUtils.isEmpty(masterSyncDtls)) {
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					"masterSyncDtls is missing it is a mandatory field.");
			return false;
		} else if (StringUtils.isEmpty(triggerPoint)) {
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					"triggerPoint is missing it is a mandatory field.");
			return false;
		} else if (StringUtils.isEmpty(keyIndex)) {
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					"keyIndex is missing it is a mandatory field.");
			return false;
		} else {
			return true;
		}

	}

	/**
	 * Master sync fields validate.
	 *
	 * @param masterSyncDtls the master sync dtls
	 * @param triggerPoint   the trigger point
	 * @return true, if successful
	 */
	private boolean masterSyncFieldsValidate(String masterSyncDtls, String triggerPoint) {

		if (StringUtils.isEmpty(masterSyncDtls)) {
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					"masterSyncDtls is missing it is a mandatory field.");
			return false;
		} else if (StringUtils.isEmpty(triggerPoint)) {
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					"triggerPoint is missing it is a mandatory field.");
			return false;
		} else {
			return true;
		}

	}

	/**
	 * Lang code null check.
	 *
	 * @param langCode the language code
	 * @return true, if successful
	 */
	private boolean langCodeNullCheck(String langCode) {
		if (StringUtils.isEmpty(langCode)) {
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					"language code is missing it is a mandatory field.");
			return false;
		} else {
			return true;
		}

	}

	private boolean codeAndlangCodeNullCheck(String code, String langCode) {

		if (StringUtils.isEmpty(code)) {
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					"code is missing it is a mandatory field.");
			return false;
		} else if (StringUtils.isEmpty(langCode)) {
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					"language code is missing it is a mandatory field.");
			return false;
		} else {
			return true;
		}

	}

	/**
	 * collects request params required for client settings sync.
	 * 
	 * @param masterSyncDtls
	 * @param keyIndex
	 * @return
	 * @throws RegBaseCheckedException
	 */
	private Map<String, String> getRequestParamsForClientSettingsSync(String masterSyncDtls, String keyIndex)
			throws RegBaseCheckedException {
		Map<String, String> requestParamMap = new HashMap<String, String>();

		if (keyIndex == null) {
			String machineName = null;
			try {
				machineName = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				throwRegBaseCheckedException(
						RegistrationExceptionConstants.REG_MASTER_SYNC_SERVICE_IMPL_NO_MACHINE_NAME);
			}
			String keyIndexBasedOnMachineName = machineMappingDAO.getKeyIndexByMachineName(machineName);
			requestParamMap.put(RegistrationConstants.KEY_INDEX.toLowerCase(), keyIndexBasedOnMachineName);
		} else
			requestParamMap.put(RegistrationConstants.KEY_INDEX.toLowerCase(),
					CryptoUtil.computeFingerPrint(clientCryptoFacade.getClientSecurity().getEncryptionPublicPart(), null));

		// getting Last Sync date from Data from sync table
		SyncControl masterSyncDetails = masterSyncDao.syncJobDetails(masterSyncDtls);
		if (masterSyncDetails != null) {
			requestParamMap.put(RegistrationConstants.MASTER_DATA_LASTUPDTAE, DateUtils.formatToISOString(
					LocalDateTime.ofInstant(masterSyncDetails.getLastSyncDtimes().toInstant(), ZoneOffset.ofHours(0))));
		}

		String registrationCenterId = getCenterId();
		if (registrationCenterId != null)
			requestParamMap.put(RegistrationConstants.MASTER_CENTER_PARAM, registrationCenterId);

		return requestParamMap;
	}

	/**
	 * Method gets all the client settings from syncdata-service and saves data in
	 * local DB. Also updates last sync time as per response in syncControl
	 * 
	 * @param masterSyncDtls
	 * @param triggerPoint
	 * @param requestParam
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private ResponseDTO syncClientSettings(String masterSyncDtls, String triggerPoint,
			Map<String, String> requestParam) {
		LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID, "Client settings sync started.....");

		ResponseDTO responseDTO = new ResponseDTO();
		LinkedHashMap<String, Object> masterSyncResponse = null;

		try {
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID, new JSONObject(requestParam).toString());

			if (RegistrationAppHealthCheckUtil.isNetworkAvailable()) {

				masterSyncResponse = (LinkedHashMap<String, Object>) serviceDelegateUtil.get(RegistrationConstants.MASTER_VALIDATOR_SERVICE_NAME,
						requestParam, true, triggerPoint);

				if (null != masterSyncResponse.get(RegistrationConstants.RESPONSE)) {
					saveClientSettings(masterSyncDtls, triggerPoint, masterSyncResponse, responseDTO);

					globalParamService.update(RegistrationConstants.MACHINE_CENTER_REMAP_FLAG,
							RegistrationConstants.FALSE);
				} else if (null != masterSyncResponse.get(RegistrationConstants.ERRORS)
						&& isMachineRemapped(masterSyncResponse))
					initiateMachineCenterRemapping(masterSyncResponse, responseDTO);
				else
					setErrorResponse(responseDTO, errorMsg(masterSyncResponse), null);
			} else
				setErrorResponse(responseDTO, RegistrationConstants.NO_INTERNET, null);

		} catch (Exception e) {
			LOGGER.error(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					e.getMessage() + ExceptionUtils.getStackTrace(e));
			setErrorResponse(responseDTO, RegistrationConstants.MASTER_SYNC_FAILURE_MSG, null);
		}

		LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
				"Client settings sync completed :: " + responseDTO.toString());
		return responseDTO;
	}

	private void saveClientSettings(String masterSyncDtls, String triggerPoint,
			LinkedHashMap<String, Object> masterSyncResponse, ResponseDTO responseDTO) throws Exception {
		LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID, "save Client Settings started...");
		String jsonString = MapperUtils.convertObjectToJsonString(masterSyncResponse.get(RegistrationConstants.RESPONSE));
		SyncDataResponseDto syncDataResponseDto = MapperUtils.convertJSONStringToDto(jsonString,
				new TypeReference<SyncDataResponseDto>() {});

		String response = masterSyncDao.saveSyncData(syncDataResponseDto);

		if (response.equals(RegistrationConstants.SUCCESS)) {
			setSuccessResponse(responseDTO, RegistrationConstants.MASTER_SYNC_SUCCESS, null);
			SyncTransaction syncTransaction = syncManager.createSyncTransaction(
					RegistrationConstants.JOB_EXECUTION_SUCCESS, RegistrationConstants.JOB_EXECUTION_SUCCESS,
					triggerPoint, masterSyncDtls);
			syncManager.updateClientSettingLastSyncTime(syncTransaction,
					getTimestamp(syncDataResponseDto.getLastSyncTime()));
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					"Save Client Settings completed successfully.");
		} else
			setErrorResponse(responseDTO, RegistrationConstants.MASTER_SYNC_FAILURE_MSG, null);
	}

	private void initiateMachineCenterRemapping(LinkedHashMap<String, Object> masterSyncResponse,
			ResponseDTO responseDTO) {
		LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID, "Machine center remapping ...");

		auditFactory.audit(AuditEvent.MACHINE_REMAPPED, Components.CENTER_MACHINE_REMAP,
				RegistrationConstants.APPLICATION_NAME, AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
		globalParamService.update(RegistrationConstants.MACHINE_CENTER_REMAP_FLAG, RegistrationConstants.TRUE);
		centerMachineReMapService.startRemapProcess();

		setSuccessResponse(responseDTO,
				(String) globalParamService.getGlobalParams().get(RegistrationConstants.INITIAL_SETUP),
				masterSyncResponse);
	}

	/**
	 * Converts string to java.sql.Timestamp
	 * 
	 * @param time
	 * @return
	 * @throws RegBaseCheckedException
	 */
	private Timestamp getTimestamp(String time) throws RegBaseCheckedException {
		try {
			Date date = simpleDateFormat.parse(time);
			Timestamp timestamp = new Timestamp(date.getTime());
			return timestamp;
		} catch (ParseException e) {
			LOGGER.error(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID, e.getMessage());
		}
		throw new RegBaseCheckedException(RegistrationConstants.SYNC_TRANSACTION_RUNTIME_EXCEPTION,
				"Failed to parse lastSyncTime from server : " + time);
	}

	@SuppressWarnings("unchecked")
	public ResponseDTO syncSchema(String triggerPoint) throws RegBaseCheckedException {
		LOGGER.info(LOG_REG_SCHEMA_SYNC, APPLICATION_NAME, APPLICATION_ID, "ID Schema sync started .....");

		ResponseDTO responseDTO = new ResponseDTO();
		LinkedHashMap<String, Object> syncResponse = null;

		if (RegistrationAppHealthCheckUtil.isNetworkAvailable()) {
			try {
				syncResponse = (LinkedHashMap<String, Object>) serviceDelegateUtil.get(
						RegistrationConstants.ID_SCHEMA_SYNC_SERVICE, new HashMap<String, String>(), true,
						triggerPoint);

				if (null != syncResponse.get(RegistrationConstants.RESPONSE)) {
					LOGGER.info(LOG_REG_SCHEMA_SYNC, APPLICATION_NAME, APPLICATION_ID,
							"ID Schema sync fetched from server.");

					String jsonString = MapperUtils
							.convertObjectToJsonString(syncResponse.get(RegistrationConstants.RESPONSE));
					SchemaDto schemaDto = MapperUtils.convertJSONStringToDto(jsonString,
							new TypeReference<SchemaDto>() {
							});

					identitySchemaDao.createIdentitySchema(schemaDto);
					setSuccessResponse(responseDTO, RegistrationConstants.SUCCESS, null);
				} else
					setErrorResponse(responseDTO, errorMsg(syncResponse), null);

			} catch (HttpClientErrorException | IOException e) {
				LOGGER.error(LOG_REG_SCHEMA_SYNC, APPLICATION_NAME, APPLICATION_ID, ExceptionUtils.getStackTrace(e));
				setErrorResponse(responseDTO, ExceptionUtils.getStackTrace(e), null);
			}
		} else
			setErrorResponse(responseDTO, RegistrationConstants.NO_INTERNET, null);

		return responseDTO;
	}

	private boolean isMachineRemapped(LinkedHashMap<String, Object> syncReponse) {

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> errorList = (List<Map<String, Object>>) syncReponse.get(RegistrationConstants.ERRORS);

		if (errorList != null && !errorList.isEmpty())
			return "KER-SNC-149".equalsIgnoreCase((String) errorList.get(0).get("errorCode"));

		return false;
	}
}
