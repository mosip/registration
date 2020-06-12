
package io.mosip.registration.mdm.service.impl;

import static io.mosip.registration.constants.LoggerConstants.MOSIP_BIO_DEVICE_MANAGER;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.http.Consts;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dao.impl.RegisteredDeviceDAO;
import io.mosip.registration.dto.json.metadata.DigitalId;
import io.mosip.registration.entity.RegisteredDeviceMaster;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.mdm.constants.MosipBioDeviceConstants;
import io.mosip.registration.mdm.dto.BioDevice;
import io.mosip.registration.mdm.dto.CaptureRequestDto;
import io.mosip.registration.mdm.dto.CaptureResponseBioDto;
import io.mosip.registration.mdm.dto.CaptureResponseDto;
import io.mosip.registration.mdm.dto.DeviceDiscoveryResponsetDto;
import io.mosip.registration.mdm.dto.DeviceInfo;
import io.mosip.registration.mdm.dto.DeviceInfoResponseData;
import io.mosip.registration.mdm.dto.MDMError;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import io.mosip.registration.mdm.dto.RequestDetail;
import io.mosip.registration.mdm.integrator.IMosipBioDeviceIntegrator;
import io.mosip.registration.mdm.integrator.MosipBioDeviceIntegratorImpl;
import io.mosip.registration.mdm.util.MdmRequestResponseBuilder;
import io.mosip.registration.mdm.util.MosioBioDeviceHelperUtil;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;

/**
 * 
 * Handles all the Biometric Devices controls
 * 
 * @author balamurugan.ramamoorthy
 * 
 */
@Component
public class MosipBioDeviceManager {

	@Autowired
	private AuditManagerService auditFactory;

	// @Value("${mdm.host}")
	// private String host;
	//
	// @Value("${mdm.hostProtocol}")
	// private String hostProtocol;
	//
	private int portFrom;

	private int portTo;

	@Autowired
	private MosipBioDeviceIntegratorImpl mosipBioDeviceIntegrator;

	@Autowired
	private RegisteredDeviceDAO registeredDeviceDAO;

	private static Map<String, BioDevice> deviceRegistry = new HashMap<>();

	private static final Logger LOGGER = AppConfig.getLogger(MosipBioDeviceManager.class);

	private ObjectMapper mapper = new ObjectMapper();

	/**
	 * This method will prepare the device registry, device registry contains all
	 * the running biometric devices
	 * <p>
	 * In order to prepare device registry it will loop through the specified ports
	 * and identify on which port any particular biometric device is running
	 * </p>
	 * 
	 * Looks for all the configured ports available and initializes all the
	 * Biometric devices and saves it for future access
	 * 
	 * @throws RegBaseCheckedException
	 *             - generalised exception with errorCode and errorMessage
	 */
	@PostConstruct
	public void init() {
		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
				"Entering init method for preparing device registry");

		portFrom = ApplicationContext.map().get(RegistrationConstants.MDM_START_PORT_RANGE) != null
				? Integer.parseInt((String) ApplicationContext.map().get(RegistrationConstants.MDM_START_PORT_RANGE))
				: 0;
		portTo = ApplicationContext.map().get(RegistrationConstants.MDM_END_PORT_RANGE) != null
				? Integer.parseInt((String) ApplicationContext.map().get(RegistrationConstants.MDM_END_PORT_RANGE))
				: 0;

		if (portFrom != 0) {
			for (int port = portFrom; port <= portTo; port++) {

				final int currentPort = port;

				/* An A-sync task to complete MDS initialization */
				new Thread(() -> {
					try {
						initByPort(currentPort);
					} catch (RuntimeException | RegBaseCheckedException exception) {
						LOGGER.error(LoggerConstants.LOG_SERVICE_DELEGATE_UTIL_GET, APPLICATION_NAME, APPLICATION_ID,
								"Exception while mapping the response : " + exception.getMessage()
										+ ExceptionUtils.getStackTrace(exception));
					}
				}).start();

			}
		}
		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
				"Exit init method for preparing device registry");
	}

	private void initByPort(int port) throws RegBaseCheckedException {
		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID, "Initializing on port : " + port);

		initByPortAndDeviceType(port, null);
	}

	private void initByDeviceType(String constructedDeviceType) throws RegBaseCheckedException {

		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
				"Initializing device : " + constructedDeviceType);
		initByPortAndDeviceType(null, constructedDeviceType);

	}

	/*
	 * Testing the network with method
	 */
	public static boolean checkServiceAvailability(String serviceUrl, String method) {
		HttpUriRequest request = RequestBuilder.create(method).setUri(serviceUrl).build();

		CloseableHttpClient client = HttpClients.createDefault();
		try {
			client.execute(request);
		} catch (Exception exception) {
			return false;
		}
		return true;

	}

	private void initByPortAndDeviceType(Integer availablePort, String deviceType) throws RegBaseCheckedException {

		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
				"Initializing device : " + deviceType + " ,on Port : " + availablePort);

		if (availablePort != null) {

			String url;
			ObjectMapper mapper = new ObjectMapper();

			url = buildUrl(availablePort, MosipBioDeviceConstants.DEVICE_INFO_ENDPOINT);
			/* check if the service is available for the current port */
			if (checkServiceAvailability(url, "MOSIPDINFO")) {
				List<LinkedHashMap<String, String>> deviceInfoResponseDtos = null;
				String response = (String) mosipBioDeviceIntegrator.getDeviceInfo(url, Object[].class);

				LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
						"Device Info Response : " + response);
				try {
					deviceInfoResponseDtos = mapper.readValue(response, List.class);
				} catch (IOException exception) {
					throw new RegBaseCheckedException();
				}

				if (MosioBioDeviceHelperUtil.isListNotEmpty(deviceInfoResponseDtos)) {

					getDeviceInfoResponse(mapper, availablePort, deviceInfoResponseDtos);
				}
			} else {
				LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
						"No device is running at port number " + availablePort);
			}
		} else {
			for (int port = portFrom; port <= portTo; port++) {

				initByPortAndDeviceType(port, deviceType);

			}
		}
	}

	private DeviceInfo getDeviceInfoDecoded(String deviceInfo, ObjectMapper mapper) {
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		Pattern pattern = Pattern.compile(RegistrationConstants.BIOMETRIC_SEPERATOR);
		Matcher matcher = pattern.matcher(deviceInfo);
		String afterMatch = null;
		if (matcher.find()) {
			afterMatch = matcher.group(1);
		}
		try {
			String result = new String(
					Base64.getUrlDecoder().decode(new String(Base64.getUrlDecoder().decode(afterMatch)).getBytes()));
			return (DeviceInfo) (mapper.readValue(result.getBytes(), DeviceInfo.class));
		} catch (Exception exception) {
			LOGGER.error(LoggerConstants.LOG_SERVICE_DELEGATE_UTIL_GET, APPLICATION_NAME, APPLICATION_ID,
					String.format("%s -> Exception while trying to extract the response through regex  %s",
							exception.getMessage() + ExceptionUtils.getStackTrace(exception)));

		}
		return null;

	}

	/**
	 * Gets the device info response.
	 * 
	 * @param mapper
	 * @param deviceInfoResponse
	 *            {@link DeviceInfoResponseData} -Contains the details of a specific
	 *            bio device
	 * @param port
	 *            - The port in which the bio device is active
	 * @param deviceInfoResponseDtos
	 *            - This list will contain the response that we receive after
	 *            finding the device
	 * @return {@link DeviceInfoResponseData}
	 * @throws RegBaseCheckedException
	 */
	private DeviceInfoResponseData getDeviceInfoResponse(ObjectMapper mapper, int port,
			List<LinkedHashMap<String, String>> deviceInfoResponseDtos) throws RegBaseCheckedException {
		DeviceInfoResponseData deviceInfoResponse = null;
		for (LinkedHashMap<String, String> deviceInfoResponseHash : deviceInfoResponseDtos) {

			try {
				deviceInfoResponse = mapper.readValue(mapper.writeValueAsString(deviceInfoResponseHash),
						DeviceInfoResponseData.class);

				DeviceInfo deviceInfo = getDeviceInfoDecoded(deviceInfoResponse.getDeviceInfo(), mapper);

				deviceInfoResponse.setDeviceInfoDecoded(deviceInfo);

				try {
					deviceInfo.setDigitalIdDecoded((DigitalId) (mapper.readValue(
							new String(Base64.getDecoder().decode(deviceInfo.getDigitalId())).getBytes(),
							DigitalId.class)));

				} catch (IOException e) {
					throw new RegBaseCheckedException();
				}
				auditFactory.audit(AuditEvent.MDM_DEVICE_FOUND, Components.MDM_DEVICE_FOUND,
						RegistrationConstants.APPLICATION_NAME,
						AuditReferenceIdTypes.APPLICATION_ID.getReferenceTypeId());

			} catch (IOException exception) {
				LOGGER.error(LoggerConstants.LOG_SERVICE_DELEGATE_UTIL_GET, APPLICATION_NAME, APPLICATION_ID,
						String.format(" Exception while mapping the response ",
								exception.getMessage() + ExceptionUtils.getStackTrace(exception)));
				auditFactory.audit(AuditEvent.MDM_NO_DEVICE_AVAILABLE, Components.MDM_NO_DEVICE_AVAILABLE,
						RegistrationConstants.APPLICATION_NAME,
						AuditReferenceIdTypes.APPLICATION_ID.getReferenceTypeId());

			}

			creationOfBioDeviceObject(deviceInfoResponse, port);
		}
		return deviceInfoResponse;
	}

	/**
	 * This method will save the device details into the device registry
	 *
	 * @param deviceInfoResponse
	 *            the device info response
	 * @param port
	 *            the port number
	 */
	private void creationOfBioDeviceObject(DeviceInfoResponseData deviceInfoResponse, int port) {

		if (deviceInfoResponse != null) {
			DeviceInfo deviceInfo = deviceInfoResponse.getDeviceInfoDecoded();
			BioDevice bioDevice = new BioDevice();
			bioDevice.setRunningPort(port);
			bioDevice.setRunningUrl(getRunningurl());
			bioDevice.setMosipBioDeviceIntegrator(mosipBioDeviceIntegrator);
			bioDevice.setDeviceId(deviceInfo.getDeviceId());
			bioDevice.setFirmWare(deviceInfo.getFirmware());
			bioDevice.setCertification(deviceInfo.getCertification());
			bioDevice.setSerialVersion(deviceInfo.getServiceVersion());
			bioDevice.setSpecVersion(deviceInfo.getSpecVersion());
			bioDevice.setPurpose(deviceInfo.getPurpose());
			bioDevice.setDeviceCode(deviceInfo.getDeviceCode());
			bioDevice.setSerialNumber(deviceInfo.getSerialNo());
			bioDevice.setDigitalId(deviceInfo.getDigitalIdDecoded());
			DigitalId digitalId = deviceInfo.getDigitalIdDecoded();

			List<RegisteredDeviceMaster> registeredDevice = null;
			if (digitalId != null) {
				bioDevice.setDeviceSubType(digitalId.getSubType());
				bioDevice.setDeviceType(digitalId.getType());
				bioDevice.setTimestamp(digitalId.getDateTime());
				bioDevice.setDeviceProviderName(digitalId.getDeviceProvider());
				bioDevice.setDeviceProviderId(digitalId.getDeviceProviderId());
				bioDevice.setDeviceModel(digitalId.getModel());
				bioDevice.setDeviceMake(digitalId.getMake());
				// For the local device validation
				registeredDevice = registeredDeviceDAO.getRegisteredDevices(digitalId.getSerialNo(),
						digitalId.getSerialNo());
			}

			bioDevice.setRegistered(registeredDevice != null ? registeredDevice.size() == 1 ? true : false : false);

			/*
			 * This particular section of code which hardcodes the registered value of the
			 * device to be true needs to be taken care of once the device registration
			 * steps in the db is straigten out
			 */
			String isDeviceValidationEnabled = ((String) ApplicationContext.getInstance().map()
					.get("isDeviceValidationEnabled"));
			if (isDeviceValidationEnabled == null)
				bioDevice.setRegistered(true);
			else
				bioDevice.setRegistered(isDeviceValidationEnabled.equals("NO") ? true : bioDevice.isRegistered());
			LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
					"Adding Device to Registry : " + bioDevice.toString());
			deviceRegistry.put(bioDevice.getDeviceType().toUpperCase() + RegistrationConstants.UNDER_SCORE
					+ bioDevice.getDeviceSubType().toUpperCase(), bioDevice);
		}

	}

	/**
	 * @return the deviceRegistry
	 */
	public static Map<String, BioDevice> getDeviceRegistry() {
		return deviceRegistry;
	}

	private String buildUrl(int port, String endPoint) {
		return getRunningurl() + ":" + port + "/" + endPoint;
	}

	private String getRunningurl() {
		return (String) ApplicationContext.map().get(RegistrationConstants.MDM_HOST_PROTOCOL) + "://"
				+ (String) ApplicationContext.map().get(RegistrationConstants.MDM_HOST);
	}

	/**
	 * Triggers the biometric capture based on the device type and returns the
	 * biometric value from MDM
	 * 
	 * @param deviceType
	 *            - The type of the device
	 * @return CaptureResponseDto - captured biometric values from the device
	 * @throws RegBaseCheckedException
	 *             - generalised exception with errorCode and errorMessage
	 * @throws IOException
	 */
	public CaptureResponseDto regScan(RequestDetail requestDetail) throws RegBaseCheckedException, IOException {

		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
				"scan method calling..." + System.currentTimeMillis());
		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
				"Finding the device from device Registry Started..." + System.currentTimeMillis());
		BioDevice bioDevice = findDeviceToScan(requestDetail.getType());

		if (bioDevice != null) {
			LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
					"Device found in the device registery..." + System.currentTimeMillis());
			bioDevice.checkForSpec();
			if (bioDevice.isSpecVersionValid()) {

				LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
						"If Device Found next capture is being called..." + System.currentTimeMillis());
				if (!bioDevice.isRegistered())
					throw new RegBaseCheckedException("102", "");
				return bioDevice.regCapture(requestDetail);
			}
			throw new RegBaseCheckedException("101", "");
		} else {
			LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
					"Device not found in the device registery - scan" + System.currentTimeMillis());
			return null;
		}

	}

	/**
	 * Triggers the biometric capture based on the device type and returns the
	 * biometric value from MDM
	 * 
	 * @param deviceType
	 *            - The type of the device
	 * @return CaptureResponseDto - captured biometric values from the device
	 * @throws RegBaseCheckedException
	 *             - generalised exception with errorCode and errorMessage
	 * @throws IOException
	 */
	public CaptureResponseDto authScan(RequestDetail requestDetail) throws RegBaseCheckedException, IOException {
		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
				"Entering into Auth Scan Method..." + System.currentTimeMillis());

		BioDevice bioDevice = findDeviceToScan(requestDetail.getType());
		InputStream streaming = stream(requestDetail.getType());
		if (bioDevice != null) {
			LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
					"Device found in the device registery");
			CaptureResponseDto captureResponse = bioDevice.regCapture(requestDetail);
			if (captureResponse.getError().getErrorCode().matches("202|403|404")) {
				streaming.close();

			}
			LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
					"Leaving Auth Scan Method..." + System.currentTimeMillis());
			return captureResponse;

		} else {
			LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
					"Device not found in the device registery - authScan" + System.currentTimeMillis());
			return null;
		}

	}

	private BioDevice findDeviceToScan(String deviceType) throws RegBaseCheckedException {
		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
				"Entering findDeviceToScan method...." + System.currentTimeMillis());

		/*
		 * fetch and store the bio device list from MDM if the device registry does not
		 * contain the requested devices
		 */

		String deviceId = "";

		String constructedDeviceType = constructDeviceType(deviceType);

		if (deviceRegistry.isEmpty() || deviceRegistry.get(constructedDeviceType) == null) {
			initByDeviceType(constructedDeviceType);
		}
		BioDevice bioDevice = deviceRegistry.get(constructedDeviceType);
		if (bioDevice == null)
			return null;

		deviceId = constructedDeviceType.equals("FINGERPRINT_SLAB")
				? deviceType.substring("FINGERPRINT_SLAB".length() + 1, deviceType.length())
				: constructedDeviceType.equals("FINGERPRINT_SINGLE") ? "SINGLE"
						: constructedDeviceType.equals("IRIS_DOUBLE") ? "DOUBLE"
								: constructedDeviceType.equals("FACE_FULL FACE")
										? deviceType.substring("FACE_FULL".length() + 1, deviceType.length())
										: deviceId;

		bioDevice.buildDeviceSubId(deviceId);
		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
				"Leaving findDeviceToScan method...." + System.currentTimeMillis());
		return bioDevice;
	}

	private String constructDeviceType(String deviceType) {

		return deviceType.contains("FINGERPRINT_SLAB") ? "FINGERPRINT_SLAB"
				: deviceType.contains("IRIS_DOUBLE") ? "IRIS_DOUBLE"
						: deviceType.contains("FACE_FULL") ? "FACE_FULL FACE" : deviceType;

	}

	public InputStream stream(String bioType) throws RegBaseCheckedException, IOException {

		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID, "Stream starting for : " + bioType);
		BioDevice bioDevice = null;
		bioDevice = findDeviceToScan(bioType);
		if (bioDevice != null)
			return bioDevice.stream();
		return null;

	}

	/**
	 * This method will return the scanned biometric data
	 * <p>
	 * When the biometric scan will happed the return will contain many detail such
	 * as device code, quality score this method will extract the scanned biometric
	 * from the captured response
	 * </p>
	 * 
	 * 
	 * @param captureResponseDto
	 *            - Response Data object {@link CaptureResponseDto} which contains
	 *            the captured biometrics from MDM
	 * @return byte[] - captured bio image
	 */
	public byte[] getSingleBioValue(CaptureResponseDto captureResponseDto) {
		byte[] capturedByte = null;
		if (null != captureResponseDto && captureResponseDto.getMosipBioDeviceDataResponses() != null
				&& !captureResponseDto.getMosipBioDeviceDataResponses().isEmpty()) {

			CaptureResponseBioDto captureResponseBioDtos = captureResponseDto.getMosipBioDeviceDataResponses().get(0);
			if (null != captureResponseBioDtos && null != captureResponseBioDtos.getCaptureResponseData()) {
				return Base64.getUrlDecoder().decode(captureResponseBioDtos.getCaptureResponseData().getBioValue());
			}
		}
		return capturedByte;
	}

	/**
	 * This method will be used to get the scanned biometric value which will be
	 * returned from the bio service as response
	 * 
	 * @param captureResponseDto
	 *            - Response object which contains the capture biometrics from MDM
	 * @return byte[] - captured bio extract
	 */
	public byte[] getSingleBiometricIsoTemplate(CaptureResponseDto captureResponseDto) {
		byte[] capturedByte = null;
		if (null != captureResponseDto && captureResponseDto.getMosipBioDeviceDataResponses() != null
				&& !captureResponseDto.getMosipBioDeviceDataResponses().isEmpty()) {

			CaptureResponseBioDto captureResponseBioDtos = captureResponseDto.getMosipBioDeviceDataResponses().get(0);
			if (null != captureResponseBioDtos && null != captureResponseBioDtos.getCaptureResponseData()) {
				return Base64.getUrlDecoder().decode(captureResponseBioDtos.getCaptureResponseData().getBioExtract());
			}
		}
		return capturedByte;
	}

	/**
	 * This method will loop through the specified port to find the active devices
	 * at any instant of time
	 * 
	 * @param deviceType
	 *            - type of bio device
	 * @return List - list of device details
	 * @throws RegBaseCheckedException
	 *             - generalized exception with errorCode and errorMessage
	 */
	public List<DeviceDiscoveryResponsetDto> getDeviceDiscovery(String deviceType) throws RegBaseCheckedException {

		List<DeviceDiscoveryResponsetDto> deviceDiscoveryResponsetDtos = null;
		String url;
		for (int port = portFrom; port <= portTo; port++) {

			url = buildUrl(port, MosipBioDeviceConstants.DEVICE_DISCOVERY_ENDPOINT);

			if (RegistrationAppHealthCheckUtil.checkServiceAvailability(url)) {
				deviceDiscoveryResponsetDtos = mosipBioDeviceIntegrator.getDeviceDiscovery(url, deviceType, null);

				auditFactory.audit(AuditEvent.MDM_DEVICE_FOUND, Components.MDM_DEVICE_FOUND,
						RegistrationConstants.APPLICATION_NAME,
						AuditReferenceIdTypes.APPLICATION_ID.getReferenceTypeId());
				break;
			} else {
				LOGGER.debug(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
						"this" + url + " is unavailable");
				auditFactory.audit(AuditEvent.MDM_NO_DEVICE_AVAILABLE, Components.MDM_NO_DEVICE_AVAILABLE,
						RegistrationConstants.APPLICATION_NAME,
						AuditReferenceIdTypes.APPLICATION_ID.getReferenceTypeId());

			}

		}
		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID, "Device discovery completed");

		return deviceDiscoveryResponsetDtos;

	}

	public void register() {

	}

	/**
	 * Used to remove any inactive devices from device registry
	 * 
	 * @param type
	 *            - device type
	 */
	public void deRegister(String type) {
		deviceRegistry.remove(type);
		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
				"Removed" + type + " from Device registry");

	}

	/**
	 * Gets the bio device.
	 *
	 * @param type
	 *            - the type of device
	 * @param modality
	 *            - the modality
	 */
	public void getBioDevice(String type, String modality) {

	}

	public void refreshBioDeviceByDeviceType(String deviceType) throws RegBaseCheckedException {

		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID, "Refreshing device of : " + deviceType);

		BioDevice bioDevice = deviceRegistry.get(constructDeviceType(deviceType));

		if (bioDevice != null) {
			initByPort(bioDevice.getRunningPort());
		}

	}

	public CaptureResponseDto scanModality(MDMRequestDto mdmRequestDto) throws RegBaseCheckedException {
		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
				"scanModality calling..." + System.currentTimeMillis());

		BioDevice bioDevice = findDeviceToScan(mdmRequestDto.getModality());
		if (bioDevice == null)
			throw new RegBaseCheckedException(MDMError.DEVICE_NOT_FOUND.getErrorCode(),
					MDMError.DEVICE_NOT_FOUND.getErrorMessage());

		if (!bioDevice.isRegistered())
			throw new RegBaseCheckedException(MDMError.DEVICE_NOT_REGISTERED.getErrorCode(),
					MDMError.DEVICE_NOT_REGISTERED.getErrorMessage());

		// TODO - support multiple spec versions, choose max version number from
		// specVersions sorted array
		String[] specVersions = bioDevice.getSpecVersion();
		Arrays.sort(specVersions);
		String supportedSpecVersion = (String) ApplicationContext.getInstance().map().get("current_mdm_spec");

		if (!Arrays.asList(specVersions).contains(supportedSpecVersion))
			throw new RegBaseCheckedException(MDMError.UNSUPPORTED_SPEC.getErrorCode(),
					MDMError.UNSUPPORTED_SPEC.getErrorMessage());

		// TODO - need to handle multiple SpecVersion
		CaptureRequestDto requestDto = MdmRequestResponseBuilder.getMDMCaptureRequestDto(supportedSpecVersion,
				bioDevice, mdmRequestDto);
		String url = bioDevice.getRunningUrl() + ":" + bioDevice.getRunningPort() + "/"
				+ MosipBioDeviceConstants.CAPTURE_ENDPOINT;
		CaptureResponseDto responseDto = (CaptureResponseDto) getMdmResponse(url, requestDto,
				mdmRequestDto.getMosipProcess().equals("Registration") ? "RCAPTURE" : "CAPTURE",
				CaptureResponseDto.class);
		try {
			bioDevice.decode(responseDto);
		} catch (IOException e) {
			throw new RegBaseCheckedException(MDMError.PARSE_ERROR.getErrorCode(),
					MDMError.PARSE_ERROR.getErrorMessage() + ExceptionUtils.getStackTrace(e));
		}
		return responseDto;
	}

	// TODO - need to handle multiple SpecVersion
	private Object getMdmResponse(String url, Object requestDto, String requestMethod, Class responseClass)
			throws RegBaseCheckedException {
		Object responseDto = null;
		try {
			String requestBody = mapper.writeValueAsString(requestDto);
			String response = invokeMDMRequest(url, requestBody, requestMethod);
			responseDto = mapper.readValue(response.getBytes(StandardCharsets.UTF_8), responseClass);
		} catch (IOException e) {
			throw new RegBaseCheckedException(MDMError.PARSE_ERROR.getErrorCode(),
					MDMError.PARSE_ERROR.getErrorMessage() + ExceptionUtils.getStackTrace(e));
		}
		return responseDto;
	}

	private String invokeMDMRequest(String url, String requestBody, String requestMethod)
			throws RegBaseCheckedException {
		CloseableHttpClient client = HttpClients.createDefault();
		StringEntity requestEntity = new StringEntity(requestBody, ContentType.create("Content-Type", Consts.UTF_8));
		HttpUriRequest request = RequestBuilder.create(requestMethod).setUri(url).setEntity(requestEntity).build();
		try {
			CloseableHttpResponse response = client.execute(request);
			return EntityUtils.toString(response.getEntity());
		} catch (IOException e) {
			throw new RegBaseCheckedException(MDMError.MDM_REQUEST_FAILED.getErrorCode(),
					MDMError.MDM_REQUEST_FAILED.getErrorMessage() + ExceptionUtils.getStackTrace(e));
		}
	}
}
