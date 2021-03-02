package io.mosip.registration.service;

import static io.mosip.registration.constants.LoggerConstants.*;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.registration.constants.PreConditionChecks;
import io.mosip.registration.dao.RegistrationCenterDAO;
import io.mosip.registration.entity.CenterMachine;
import io.mosip.registration.entity.MachineMaster;
import io.mosip.registration.exception.PreConditionCheckException;
import io.mosip.registration.repositories.CenterMachineRepository;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.service.operator.UserDetailService;
import io.mosip.registration.service.remap.CenterMachineReMapService;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.mosip.commons.packet.constants.Biometric;
import io.mosip.commons.packet.constants.PacketManagerConstants;
import io.mosip.kernel.core.cbeffutil.entity.BDBInfo;
import io.mosip.kernel.core.cbeffutil.entity.BIR;
import io.mosip.kernel.core.cbeffutil.entity.BIRInfo;
import io.mosip.kernel.core.cbeffutil.entity.BIRVersion;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.ProcessedLevelType;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.PurposeType;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.QualityType;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.RegistryIDType;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleAnySubtypeType;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.FileUtils;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.core.util.exception.JsonMappingException;
import io.mosip.kernel.core.util.exception.JsonParseException;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.DeviceTypes;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.MachineMappingDAO;
import io.mosip.registration.dao.UserOnboardDAO;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.PacketStatusDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.RegistrationDataDto;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.entity.Registration;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.service.config.GlobalParamService;
import io.mosip.registration.service.template.impl.NotificationServiceImpl;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;

/**
 * This is a base class for service package. The common functionality across the
 * 'services' classes are implemented in this class to inherit this property at
 * the required extended classes.
 * 
 */
@Service
public class BaseService {

	/**
	 * Instance of LOGGER
	 */
	private static final Logger LOGGER = AppConfig.getLogger(NotificationServiceImpl.class);

	private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat(TIMESTAMP_FORMAT);

	/**
	 * serviceDelegateUtil which processes the HTTPRequestDTO requests
	 */
	@Autowired
	protected ServiceDelegateUtil serviceDelegateUtil;

	@Autowired
	private MachineMappingDAO machineMappingDAO;

	@Autowired
	private UserOnboardDAO userOnboardDAO;

	@Autowired
	private GlobalParamService globalParamService;

	@Autowired
	private UserDetailService userDetailService;

	@Autowired
	private CenterMachineReMapService centerMachineReMapService;

	@Autowired
	private RegistrationCenterDAO registrationCenterDAO;

	@Autowired
	private MachineMasterRepository machineMasterRepository;

	@Autowired
	private CenterMachineRepository centerMachineRepository;

	/**
	 * create success response.
	 *
	 * @param responseDTO the response DTO
	 * @param message     the message
	 * @param attributes  the attributes
	 * @return ResponseDTO returns the responseDTO after creating appropriate
	 *         success response and mapping to it
	 */
	public ResponseDTO setSuccessResponse(ResponseDTO responseDTO, String message, Map<String, Object> attributes) {

		/** Success Response */
		SuccessResponseDTO successResponseDTO = new SuccessResponseDTO();

		successResponseDTO.setMessage(message);
		successResponseDTO.setCode(RegistrationConstants.ALERT_INFORMATION);

		/** Adding attributes to success response */
		successResponseDTO.setOtherAttributes(attributes);

		responseDTO.setSuccessResponseDTO(successResponseDTO);
		return responseDTO;
	}

	/**
	 * create error response.
	 *
	 * @param response   the response
	 * @param message    the message
	 * @param attributes the attributes
	 * @return ResponseDTO returns the responseDTO after creating appropriate error
	 *         response and mapping to it
	 */
	protected ResponseDTO setErrorResponse(final ResponseDTO response, final String message,
			final Map<String, Object> attributes) {

		/** Create list of Error Response */
		List<ErrorResponseDTO> errorResponses = (response.getErrorResponseDTOs() != null)
				? response.getErrorResponseDTOs()
				: new LinkedList<>();

		/** Error response */
		ErrorResponseDTO errorResponse = new ErrorResponseDTO();

		errorResponse.setCode(RegistrationConstants.ERROR);
		errorResponse.setMessage(message);

		errorResponse.setOtherAttributes(attributes);

		errorResponses.add(errorResponse);

		/** Adding list of error responses to response */
		response.setErrorResponseDTOs(errorResponses);
		return response;

	}

	/**
	 * Get User Id using session context.
	 *
	 * @return user id
	 */
	public String getUserIdFromSession() {
		String userId = null;
		if (SessionContext.isSessionContextAvailable()) {
			userId = SessionContext.userId();
			if (userId.equals(RegistrationConstants.AUDIT_DEFAULT_USER)) {
				userId = RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM;
			}
		} else {
			userId = RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM;
		}
		return userId;
	}


	/**
	 * Checks if is null.
	 *
	 * @param list the list
	 * @return true, if is null
	 */
	public boolean isNull(List<?> list) {
		/* Check Whether the list is Null or not */
		return list == null;

	}

	/**
	 * Checks if is empty.
	 *
	 * @param list the list
	 * @return true, if is empty
	 */
	public boolean isEmpty(List<?> list) {
		/* Check Whether the list is empty or not */
		return list.isEmpty();
	}

	/**
	 * Gets the station id.
	 *
	 * @return the station id
	 */
	public String getStationId() {
		String machineName = RegistrationSystemPropertiesChecker.getMachineId();
		MachineMaster machineMaster = machineMasterRepository.findByNameIgnoreCaseAndRegMachineSpecIdLangCode(machineName.toLowerCase(),
				ApplicationContext.applicationLanguage());

		if(machineMaster != null && machineMaster.getRegMachineSpecId().getId() != null && machineMaster.getIsActive())
			return machineMaster.getRegMachineSpecId().getId();
		return null;
	}

	/**
	 * Gets the center id.
	 *
	 * @return the center id
	 */
	public String getCenterId() {
		String stationId = getStationId();
		if (stationId != null) {
			CenterMachine centerMachine = centerMachineRepository.findByCenterMachineIdMachineId(stationId);
			return centerMachine != null && centerMachine.getIsActive() ?
					centerMachine.getCenterMachineId().getRegCenterId() : null;
		}
		return null;
	}

	/**
	 * Gets the center id.
	 *
	 * @param stationId the station id
	 * @return the center id
	 */
	public String getCenterId(@NonNull String stationId) {
		CenterMachine centerMachine = centerMachineRepository.findByCenterMachineIdMachineId(stationId);
		return centerMachine != null && registrationCenterDAO.isMachineCenterActive(stationId) ?
				centerMachine.getCenterMachineId().getRegCenterId() : null;
	}

	/**
	 * Get Global Param configuration value.
	 *
	 * @param key the name
	 * @return value
	 */
	public String getGlobalConfigValueOf(String key) {

		String val = null;
		if (key != null) {
			ApplicationContext.getInstance();
			// Check application map
			if (ApplicationContext.map().isEmpty() || ApplicationContext.map().get(key) == null) {

				// Load Global params if application map is empty
				ApplicationContext.setApplicationMap(globalParamService.getGlobalParams());
			}

			// Get Value of global param
			val = (String) ApplicationContext.map().get(key);
		}
		return val;
	}

	/**
	 * Conversion of Registration to Packet Status DTO.
	 *
	 * @param registration the registration
	 * @return the packet status DTO
	 */
	public PacketStatusDTO packetStatusDtoPreperation(Registration registration) {
		PacketStatusDTO statusDTO = new PacketStatusDTO();
		statusDTO.setFileName(registration.getId());
		statusDTO.setPacketClientStatus(registration.getClientStatusCode());
		statusDTO.setPacketPath(registration.getAckFilename());
		statusDTO.setPacketServerStatus(registration.getServerStatusCode());
		statusDTO.setUploadStatus(registration.getFileUploadStatus());
		statusDTO.setPacketStatus(registration.getStatusCode());
		statusDTO.setSupervisorStatus(registration.getClientStatusCode());
		statusDTO.setSupervisorComments(registration.getClientStatusComments());

		try {
			if (registration.getAdditionalInfo() != null) {
				String additionalInfo = new String(registration.getAdditionalInfo());
				RegistrationDataDto registrationDataDto = (RegistrationDataDto) JsonUtils
						.jsonStringToJavaObject(RegistrationDataDto.class, additionalInfo);
				statusDTO.setName(registrationDataDto.getName());
				statusDTO.setPhone(registrationDataDto.getPhone());
				statusDTO.setEmail(registrationDataDto.getEmail());
			}
		} catch (JsonParseException | JsonMappingException | io.mosip.kernel.core.exception.IOException exception) {
			LOGGER.error("REGISTRATION_BASE_SERVICE", APPLICATION_NAME, APPLICATION_ID,
					exception.getMessage() + ExceptionUtils.getStackTrace(exception));
		}

		try (FileInputStream fis = new FileInputStream(FileUtils.getFile(registration.getAckFilename().replace(
				RegistrationConstants.ACKNOWLEDGEMENT_FILE_EXTENSION, RegistrationConstants.ZIP_FILE_EXTENSION)))) {
			byte[] byteArray = new byte[(int) fis.available()];
			fis.read(byteArray);
			statusDTO.setPacketHash(HMACUtils2.digestAsPlainText(byteArray));
			statusDTO.setPacketSize(BigInteger.valueOf(byteArray.length));

		} catch (IOException | NoSuchAlgorithmException ioException) {
			LOGGER.error("REGISTRATION_BASE_SERVICE", APPLICATION_NAME, APPLICATION_ID,
					ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
		}

		return statusDTO;
	}

	/*
	 * public static void setBaseGlobalMap(Map<String, Object> map) { applicationMap
	 * = map; }
	 * 
	 * public static Map<String, Object> getBaseGlobalMap() { return applicationMap;
	 * }
	 */

	/**
	 * Registration date conversion.
	 *
	 * @param timestamp the timestamp
	 * @return the string
	 */
	protected String regDateConversion(Timestamp timestamp) {

		DateFormat dateFormat = new SimpleDateFormat(RegistrationConstants.EOD_PROCESS_DATE_FORMAT);
		Date date = new Date(timestamp.getTime());
		return dateFormat.format(date);
	}

	protected String regDateTimeConversion(String time) {
		try {
			String formattedTime = Timestamp.valueOf(time).toLocalDateTime()
					.format(DateTimeFormatter.ofPattern(RegistrationConstants.UTC_PATTERN));
			LocalDateTime dateTime = DateUtils.parseUTCToLocalDateTime(formattedTime);
			return dateTime.format(DateTimeFormatter.ofPattern(RegistrationConstants.TEMPLATE_DATE_FORMAT));
		} catch (RuntimeException exception) {
			LOGGER.error("REGISTRATION - ALERT - BASE_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(exception));
			return time + RegistrationConstants.UTC_APPENDER;
		}
	}

	protected boolean isNull(String val) {
		return (val == null || val.equalsIgnoreCase("NULL"));
	}

	/**
	 * Common method to throw {@link RegBaseCheckedException} based on the
	 * {@link RegistrationExceptionConstants} enum passed as parameter. Extracts the
	 * error code and error message from the enum parameter.
	 * 
	 * @param exceptionEnum the enum of {@link RegistrationExceptionConstants}
	 *                      containing the error code and error message to be thrown
	 * @throws RegBaseCheckedException the checked exception
	 */
	protected void throwRegBaseCheckedException(RegistrationExceptionConstants exceptionEnum)
			throws RegBaseCheckedException {
		throw new RegBaseCheckedException(exceptionEnum.getErrorCode(), exceptionEnum.getErrorMessage());
	}

	/**
	 * Validates the input {@link List} is either <code>null</code> or empty
	 * 
	 * @param listToBeValidated the {@link List} object to be validated
	 * @return <code>true</code> if {@link List} is either <code>null</code> or
	 *         empty, else <code>false</code>
	 */
	protected boolean isListEmpty(List<?> listToBeValidated) {
		return listToBeValidated == null || listToBeValidated.isEmpty();
	}

	/**
	 * Validates the input {@link Set} is either <code>null</code> or empty
	 * 
	 * @param setToBeValidated the {@link Set} object to be validated
	 * @return <code>true</code> if {@link Set} is either <code>null</code> or
	 *         empty, else <code>false</code>
	 */
	protected boolean isSetEmpty(Set<?> setToBeValidated) {
		return setToBeValidated == null || setToBeValidated.isEmpty();
	}

	/**
	 * Validates the input {@link String} is either <code>null</code> or empty
	 * 
	 * @param stringToBeValidated the {@link String} object to be validated
	 * @return <code>true</code> if input {@link String} is either <code>null</code>
	 *         or empty, else <code>false</code>
	 */
	protected boolean isStringEmpty(String stringToBeValidated) {
		return stringToBeValidated == null || stringToBeValidated.isEmpty();
	}

	/**
	 * Validates the input {@link Map} is either <code>null</code> or empty
	 * 
	 * @param mapToBeValidated the {@link Map} object to be validated
	 * @return <code>true</code> if {@link Map} is either <code>null</code> or
	 *         empty, else <code>false</code>
	 */
	protected boolean isMapEmpty(Map<?, ?> mapToBeValidated) {
		return mapToBeValidated == null || mapToBeValidated.isEmpty();
	}

	/**
	 * Validates the input byte array is either <code>null</code> or empty
	 * 
	 * @param byteArrayToBeValidated the byte array to be validated
	 * @return <code>true</code> if byte array is either <code>null</code> or empty,
	 *         else <code>false</code>
	 */
	protected boolean isByteArrayEmpty(byte[] byteArrayToBeValidated) {
		return byteArrayToBeValidated == null || byteArrayToBeValidated.length == 0;
	}

	/**
	 * Validates if the error code of the input {@link Exception} is same of the
	 * error code of Auth Token Empty
	 * 
	 * @param exception the {@link Exception} to be validated
	 * @return <code>true</code> if error code is same as Auth Token empty
	 */
	protected boolean isAuthTokenEmptyException(Exception exception) {
		return exception instanceof RegBaseCheckedException
				&& RegistrationExceptionConstants.AUTH_TOKEN_COOKIE_NOT_FOUND.getErrorCode()
						.equals(((RegBaseCheckedException) exception).getErrorCode());
	}

	/**
	 * Validates if the error code of the input {@link ResponseDTO} is same of the
	 * error code of Auth Token Empty
	 * 
	 * @param responseDTO the {@link ResponseDTO} to be validated
	 * @return <code>true</code> if error code is same as Auth Token empty
	 */
	protected boolean isAuthTokenEmptyError(ResponseDTO responseDTO) {
		boolean isAuthTokenEmptyError = false;
		if (responseDTO != null && responseDTO.getErrorResponseDTOs() != null
				&& !responseDTO.getErrorResponseDTOs().isEmpty()) {
			isAuthTokenEmptyError = RegistrationExceptionConstants.AUTH_TOKEN_COOKIE_NOT_FOUND.getErrorCode()
					.equals(responseDTO.getErrorResponseDTOs().get(0).getMessage());
		}

		return isAuthTokenEmptyError;
	}

	/*public static boolean isChild() {

		return (boolean) SessionContext.map().get(RegistrationConstants.IS_Child);
	}*/

	/**
	 * Gets the registration DTO from session.
	 *
	 * @return the registration DTO from session
	 */
	protected RegistrationDTO getRegistrationDTOFromSession() {
		return (RegistrationDTO) SessionContext.map().get(RegistrationConstants.REGISTRATION_DATA);
	}

	/**
	 * Check of update UIN whether only demo update or bio includes.
	 *
	 * @return true or false of biometric update
	 */
	/*protected boolean isUpdateUinNonBioMetric() {
		return getRegistrationDTOFromSession().isUpdateUINNonBiometric();
	}*/

	public BIR buildBir(String bioAttribute, long qualityScore, byte[] iso, ProcessedLevelType processedLevelType) {

		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Building BIR for captured biometrics to pass them for quality check with SDK");

		SingleType singleType = Biometric.getSingleTypeByAttribute(bioAttribute);

		RegistryIDType birFormat = new RegistryIDType();
		birFormat.setOrganization(PacketManagerConstants.CBEFF_DEFAULT_FORMAT_ORG);
		birFormat.setType(String.valueOf(Biometric.getFormatType(singleType)));

		RegistryIDType birAlgorithm = new RegistryIDType();
		birAlgorithm.setOrganization(PacketManagerConstants.CBEFF_DEFAULT_ALG_ORG);
		birAlgorithm.setType(PacketManagerConstants.CBEFF_DEFAULT_ALG_TYPE);

		QualityType qualityType = new QualityType();
		qualityType.setAlgorithm(birAlgorithm);
		qualityType.setScore(qualityScore);

		return new BIR.BIRBuilder().withBdb(iso)
				.withVersion(new BIRVersion.BIRVersionBuilder().withMajor(1).withMinor(1).build())
				.withCbeffversion(new BIRVersion.BIRVersionBuilder().withMajor(1).withMinor(1).build())
				.withBirInfo(new BIRInfo.BIRInfoBuilder().withIntegrity(false).build())
				.withBdbInfo(new BDBInfo.BDBInfoBuilder().withFormat(birFormat).withQuality(qualityType)
						.withType(Arrays.asList(singleType)).withSubtype(getSubTypes(singleType, bioAttribute))
						.withPurpose(PurposeType.IDENTIFY).withLevel(processedLevelType)
						.withCreationDate(LocalDateTime.now(ZoneId.of("UTC"))).withIndex(UUID.randomUUID().toString())
						.build())
				.build();

	}

	public BIR buildBir(BiometricsDto biometricsDto) {
		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Building BIR for captured biometrics to pass them for quality check with SDK");

		SingleType singleType = Biometric.getSingleTypeByAttribute(biometricsDto.getBioAttribute());

		RegistryIDType birFormat = new RegistryIDType();
		birFormat.setOrganization(PacketManagerConstants.CBEFF_DEFAULT_FORMAT_ORG);
		birFormat.setType(String.valueOf(Biometric.getFormatType(singleType)));

		RegistryIDType birAlgorithm = new RegistryIDType();
		birAlgorithm.setOrganization(PacketManagerConstants.CBEFF_DEFAULT_ALG_ORG);
		birAlgorithm.setType(PacketManagerConstants.CBEFF_DEFAULT_ALG_TYPE);

		QualityType qualityType = new QualityType();
		qualityType.setAlgorithm(birAlgorithm);
		qualityType.setScore((long) biometricsDto.getQualityScore());

		return new BIR.BIRBuilder().withBdb(biometricsDto.getAttributeISO())
				.withVersion(new BIRVersion.BIRVersionBuilder().withMajor(1).withMinor(1).build())
				.withCbeffversion(new BIRVersion.BIRVersionBuilder().withMajor(1).withMinor(1).build())
				.withBirInfo(new BIRInfo.BIRInfoBuilder().withIntegrity(false).build())
				.withBdbInfo(new BDBInfo.BDBInfoBuilder().withFormat(birFormat).withQuality(qualityType)
						.withType(Arrays.asList(singleType))
						.withSubtype(getSubTypes(singleType, biometricsDto.getBioAttribute()))
						.withPurpose(PurposeType.IDENTIFY).withLevel(ProcessedLevelType.RAW)
						.withCreationDate(LocalDateTime.now(ZoneId.of("UTC"))).withIndex(UUID.randomUUID().toString())
						.build())
				.build();
	}

	private List<String> getSubTypes(SingleType singleType, String bioAttribute) {
		List<String> subtypes = new LinkedList<>();
		switch (singleType) {
		case FINGER:
			subtypes.add(bioAttribute.contains("left") ? SingleAnySubtypeType.LEFT.value()
					: SingleAnySubtypeType.RIGHT.value());
			if (bioAttribute.toLowerCase().contains("thumb"))
				subtypes.add(SingleAnySubtypeType.THUMB.value());
			else {
				String val = bioAttribute.toLowerCase().replace("left", "").replace("right", "");
				subtypes.add(SingleAnySubtypeType.fromValue(StringUtils.capitalizeFirstLetter(val).concat("Finger"))
						.value());
			}
			break;
		case IRIS:
			subtypes.add(bioAttribute.contains("left") ? SingleAnySubtypeType.LEFT.value()
					: SingleAnySubtypeType.RIGHT.value());
			break;
		case FACE:
			subtypes.add(SingleType.FACE.value());
			break;
		default:
			break;
		}
		return subtypes;
	}

	/**
	 * Converts string to java.sql.Timestamp
	 *
	 * @param time
	 * @return
	 * @throws RegBaseCheckedException
	 */
	public Timestamp getTimestamp(String time) throws RegBaseCheckedException {
		try {
			Date date = simpleDateFormat.parse(time);
			Timestamp timestamp = new Timestamp(date.getTime());
			return timestamp;
		} catch (ParseException e) {
			LOGGER.error("", APPLICATION_NAME, APPLICATION_ID, e.getMessage());
		}
		throw new RegBaseCheckedException(RegistrationConstants.SYNC_TRANSACTION_RUNTIME_EXCEPTION,
				"Failed to parse lastSyncTime from server : " + time);
	}

	public ResponseDTO getHttpResponseErrors(ResponseDTO responseDTO, LinkedHashMap<String, Object> httpResponse) {
		List<ErrorResponseDTO> erResponseDTOs = new ArrayList<>();
		ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO();
		errorResponseDTO.setCode(RegistrationConstants.ERRORS);
		String errorMessage = RegistrationConstants.API_CALL_FAILED;
		if (httpResponse != null && httpResponse.get(RegistrationConstants.ERRORS) != null) {
			List<HashMap<String, String>> errors = (List<HashMap<String, String>>) httpResponse
					.get(RegistrationConstants.ERRORS);

			// TODO Commented as getting error
//			LOGGER.error("Response Errors >>>> {}", errors);
			errorMessage = errors.isEmpty() ? RegistrationConstants.API_CALL_FAILED
					: errors.get(0).get(RegistrationConstants.ERROR_MSG);
		}
		errorResponseDTO.setMessage(errorMessage);
		erResponseDTOs.add(errorResponseDTO);
		responseDTO.setErrorResponseDTOs(erResponseDTOs);
		return responseDTO;
	}

	public void commonPreConditionChecks(String action) throws PreConditionCheckException {
		if(!RegistrationAppHealthCheckUtil.isNetworkAvailable())
			throw new PreConditionCheckException(PreConditionChecks.NO_CONNECTION.name(),
					action + " forbidden as User is inactive");

		if(SessionContext.isSessionContextAvailable() &&
				!userDetailService.isValidUser(SessionContext.userId()) && !isInitialSync())
			throw new PreConditionCheckException(PreConditionChecks.USER_INACTIVE.name(),
					action + " forbidden as User is inactive");
	}

	public void proceedWithMasterAndKeySync(String jobId) throws PreConditionCheckException {
		commonPreConditionChecks("Sync");

		//Donot validate pre-conditions as its initial sync
		if(isInitialSync()) {
			LOGGER.warn("", APPLICATION_NAME, APPLICATION_ID, "Ignoring pre-checks as its Initial sync");
			return;
		}

		//check if remap is in progress
		if(centerMachineReMapService.isMachineRemapped())
			throw new PreConditionCheckException(PreConditionChecks.MARKED_FOR_REMAP.name(),
					"Sync forbidden as machine is marked for center remap");

		String machineId = getStationId();
		if(RegistrationConstants.OPT_TO_REG_PDS_J00003.equals(jobId) && machineId == null)
			throw new PreConditionCheckException(PreConditionChecks.MACHINE_INACTIVE.name(),
					"Pre-reg data sync action forbidden as machine is inactive");

		//check regcenter table for center status
		//if center is inactive, sync is not allowed
		if(!registrationCenterDAO.isMachineCenterActive(machineId))
			throw new PreConditionCheckException(PreConditionChecks.CENTER_INACTIVE.name(),
					"Pre-reg data sync action forbidden as center is inactive");
	}

	public void proceedWithPacketSync() throws PreConditionCheckException {
		commonPreConditionChecks("Packet Sync");
	}

	public void proceedWithMachineCenterRemap() throws PreConditionCheckException {
		commonPreConditionChecks("Center Remap");
	}

	public void proceedWithSoftwareUpdate() throws PreConditionCheckException {
		commonPreConditionChecks("Software update");

		if(centerMachineReMapService.isMachineRemapped())
			throw new PreConditionCheckException(PreConditionChecks.MARKED_FOR_REMAP.name(),
					"Software update forbidden as machine is marked for center remap");

		//TODO - check if this shld be allowed when machine / center is inactive
	}

	public void proceedWithOperatorOnboard() throws PreConditionCheckException {
		commonPreConditionChecks("Onboarding");

		if(centerMachineReMapService.isMachineRemapped())
			throw new PreConditionCheckException(PreConditionChecks.MARKED_FOR_REMAP.name(),
					"Onboarding forbidden as machine is marked for center remap");

		//RegistrationUIConstants.CENTER_MACHINE_INACTIVE
		String machineId = getStationId();
		if(machineId == null)
			throw new PreConditionCheckException(PreConditionChecks.MACHINE_INACTIVE.name(),
					"Onboarding action forbidden as machine is inactive");


		if(!registrationCenterDAO.isMachineCenterActive(machineId))
			throw new PreConditionCheckException(PreConditionChecks.CENTER_INACTIVE.name(),
					"Onboarding action forbidden as center is inactive");
	}

	public void proceedWithRegistration() throws PreConditionCheckException {
		commonPreConditionChecks("Registration");

		if(centerMachineReMapService.isMachineRemapped())
			throw new PreConditionCheckException(PreConditionChecks.MARKED_FOR_REMAP.name(),
					"Registration forbidden as machine is marked for center remap");

		//RegistrationUIConstants.CENTER_MACHINE_INACTIVE
		String machineId = getStationId();
		if(machineId == null)
			throw new PreConditionCheckException(PreConditionChecks.MACHINE_INACTIVE.name(),
					"Registration forbidden as machine is inactive");

		if(!registrationCenterDAO.isMachineCenterActive(machineId))
			throw new PreConditionCheckException(PreConditionChecks.CENTER_INACTIVE.name(),
					"Registration forbidden as center is inactive");
	}

	/**
	 * Checks if this is initial launch
	 * @return
	 */
	public boolean isInitialSync() {
		return RegistrationConstants.ENABLE.equalsIgnoreCase(getGlobalConfigValueOf(RegistrationConstants.INITIAL_SETUP));
	}

}
