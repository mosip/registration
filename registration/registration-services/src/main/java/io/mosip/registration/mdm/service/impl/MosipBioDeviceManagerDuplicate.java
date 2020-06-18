
package io.mosip.registration.mdm.service.impl;

import static io.mosip.registration.constants.LoggerConstants.MOSIP_BIO_DEVICE_INTEGERATOR;
import static io.mosip.registration.constants.LoggerConstants.MOSIP_BIO_DEVICE_MANAGER;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.packetmanager.dto.BiometricsDto;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dao.impl.RegisteredDeviceDAO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.mdm.MdmDeviceInfo;
import io.mosip.registration.mdm.constants.MosipBioDeviceConstants;
import io.mosip.registration.mdm.dto.BioDevice;
import io.mosip.registration.mdm.dto.Biometric;
import io.mosip.registration.mdm.dto.CaptureResponseDto;
import io.mosip.registration.mdm.dto.DeviceDiscoveryResponsetDto;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import io.mosip.registration.mdm.dto.MdmBioDevice;
import io.mosip.registration.mdm.dto.RequestDetail;
import io.mosip.registration.mdm.spec_0_9_2.service.impl.MDM_092_IntegratorImpl;
import io.mosip.registration.mdm.spec_0_9_5.dto.request.RCaptureRequestBioDTO;
import io.mosip.registration.mdm.spec_0_9_5.dto.request.RCaptureRequestDTO;
import io.mosip.registration.mdm.spec_0_9_5.dto.request.StreamRequestDTO;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.DigitalId;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.MdmDeviceInfoResponse;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.RCaptureResponseBiometricsDTO;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.RCaptureResponseDataDTO;
import io.mosip.registration.mdm.spec_0_9_5.service.impl.MDM_095_IntegratorImpl;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;

/**
 * 
 * Handles all the Biometric Devices controls
 * 
 * @author balamurugan.ramamoorthy
 * 
 */
@Component
public class MosipBioDeviceManagerDuplicate {

	@Autowired
	private AuditManagerService auditFactory;

	//
	private int portFrom;

	private int portTo;

	private static Map<String, BioDevice> deviceRegistry = new HashMap<>();

	private static final Logger LOGGER = AppConfig.getLogger(MosipBioDeviceManagerDuplicate.class);

	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private MDM_092_IntegratorImpl mdm_092_IntegratorImpl;

	@Autowired
	private MDM_095_IntegratorImpl mdm_095_IntegratorImpl;

	/** Key is modality value is (specVersion, MdmBioDevice) */
	private static Map<String, MdmBioDevice> deviceInfoMap = new LinkedHashMap<>();

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

	public void initByPort(Integer availablePort) throws RegBaseCheckedException {

		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
				"Initializing device " + " on Port : " + availablePort);

		if (availablePort != null) {

			String url;

			url = buildUrl(availablePort, MosipBioDeviceConstants.DEVICE_INFO_ENDPOINT);
			/* check if the service is available for the current port */
			if (checkServiceAvailability(url, "MOSIPDINFO")) {

				List<MdmDeviceInfo> deviceInfoResponseList = getDeviceInfo(url);

				for (MdmDeviceInfo deviceInfo : deviceInfoResponseList) {

					try {

						MdmBioDevice bioDevice = getBioDevice(deviceInfo, availablePort);

						for (String specVersion : deviceInfo.getSpecVersion()) {

							// Add to Device Info Map
							addToDeviceInfoMap(specVersion, getDeviceType(bioDevice.getDeviceType()).toLowerCase(),
									getDeviceSubType(bioDevice.getDeviceSubType()), bioDevice);
						}
					} catch (Exception exception) {
						LOGGER.error(LoggerConstants.LOG_SERVICE_DELEGATE_UTIL_GET, APPLICATION_NAME, APPLICATION_ID,
								String.format(" Exception while mapping the response ",
										exception.getMessage() + ExceptionUtils.getStackTrace(exception)));
					}

				}
			} else {
				LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
						"No device is running at port number " + availablePort);
			}

		} else {
			for (int port = portFrom; port <= portTo; port++) {

				initByPort(port);

			}
		}
	}

	private MdmBioDevice getBioDevice(MdmDeviceInfo deviceInfo, int port)
			throws JsonParseException, JsonMappingException, IOException {

		MdmBioDevice bioDevice = new MdmBioDevice();

		if (deviceInfo != null) {

			DigitalId digitalId = getDigitalId(deviceInfo.getDigitalId());

			bioDevice.setRunningPort(port);
			bioDevice.setDeviceId(deviceInfo.getDeviceId());
			bioDevice.setFirmWare(deviceInfo.getFirmware());
			bioDevice.setCertification(deviceInfo.getCertification());
			bioDevice.setSerialVersion(deviceInfo.getServiceVersion());
			bioDevice.setSpecVersion(getLatestSpecVersion(deviceInfo.getSpecVersion()));
			bioDevice.setPurpose(deviceInfo.getPurpose());
			bioDevice.setDeviceCode(deviceInfo.getDeviceCode());

			bioDevice.setDeviceSubType(digitalId.getDeviceSubType());
			bioDevice.setDeviceType(digitalId.getType());
			bioDevice.setTimestamp(digitalId.getDateTime());
			bioDevice.setDeviceProviderName(digitalId.getDeviceProvider());
			bioDevice.setDeviceProviderId(digitalId.getDeviceProviderId());
			bioDevice.setDeviceModel(digitalId.getModel());
			bioDevice.setDeviceMake(digitalId.getMake());

			bioDevice.setCallbackId(deviceInfo.getCallbackId());

		}

		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID, "Adding Device to Registry : ");
		return bioDevice;
	}

	private DigitalId getDigitalId(String digitalId) throws JsonParseException, JsonMappingException, IOException {
		return (DigitalId) (mapper.readValue(new String(Base64.getUrlDecoder().decode(getPayLoad(digitalId))),
				DigitalId.class));

	}

	private void addToDeviceInfoMap(String specVersion, String type, String subType, MdmBioDevice bioDevice) {

		String key = String.format("%s_%s", type.toLowerCase(), subType.toLowerCase());

		if (deviceInfoMap.containsKey(key)) {
			MdmBioDevice mdmBioDevice = deviceInfoMap.get(key);

			if (specVersion.equals(getLatestVersion(specVersion, mdmBioDevice.getSpecVersion()))) {
				deviceInfoMap.put(key, bioDevice);
			}
		} else {
			deviceInfoMap.put(key, bioDevice);
		}

	}

	private String getDeviceType(String type) {

		type = type.toLowerCase();

		if (type.contains("finger") || type.contains("fir")) {
			return SingleType.FINGER.value();
		}
		if (type.contains("iris") || type.contains("iir")) {
			return SingleType.IRIS.value();
		}
		if (type.contains("face")) {
			return SingleType.FACE.value();
		}
		return null;
	}

	private String getDeviceSubType(String subType) {

		subType = subType.toLowerCase();

		if (subType.contains("slab") || subType.contains("slap")) {
			return "slab";
		}
		if (subType.contains("single")) {
			return "single";
		}
		if (subType.contains("double")) {
			return "double";
		}
		if (subType.contains("face")) {
			return "face";
		}
		return null;
	}

	private MdmDeviceInfo getDeviceInfoDecoded(String deviceInfo) {
		try {
			String result = new String(Base64.getUrlDecoder().decode(getPayLoad(deviceInfo)));
			return (MdmDeviceInfo) (mapper.readValue(result, MdmDeviceInfo.class));
		} catch (Exception exception) {
			LOGGER.error(LoggerConstants.LOG_SERVICE_DELEGATE_UTIL_GET, APPLICATION_NAME, APPLICATION_ID,
					String.format("%s -> Exception while trying to extract the response through regex  %s",
							exception.getMessage() + ExceptionUtils.getStackTrace(exception)));

		}
		return null;

	}

	private String getPayLoad(String data) {

		String payLoad = null;
		Pattern pattern = Pattern.compile(RegistrationConstants.BIOMETRIC_SEPERATOR);
		Matcher matcher = pattern.matcher(data);
		if (matcher.find()) {
			payLoad = matcher.group(1);
		}

		return payLoad;
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
		return "http" + "://" + "127.0.0.1";
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
			initByPort(null);
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

		return deviceType.contains("FINGERPRINT_SLAB") ? "FINGER_SLAB"
				: deviceType.contains("IRIS_DOUBLE") ? "IRIS_DOUBLE"
						: deviceType.contains("FACE_FULL") ? "FACE_FULL FACE" : deviceType;

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
				// deviceDiscoveryResponsetDtos =
				// mosipBioDeviceIntegrator.getDeviceDiscovery(url, deviceType, null);

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

	private List<MdmDeviceInfo> getDeviceInfo(String url) {
		LOGGER.info(MOSIP_BIO_DEVICE_INTEGERATOR, APPLICATION_NAME, APPLICATION_ID, "Getting the device info");

		HttpUriRequest request = RequestBuilder.create("MOSIPDINFO").setUri(url).build();
		CloseableHttpClient client = HttpClients.createDefault();
		CloseableHttpResponse clientResponse = null;
		String response = null;
		List<MdmDeviceInfo> mdmDeviceInfos = new LinkedList<>();

		try {
			clientResponse = client.execute(request);
			response = EntityUtils.toString(clientResponse.getEntity());

			List<MdmDeviceInfoResponse> deviceInfoResponses = (mapper.readValue(response,
					new TypeReference<List<MdmDeviceInfoResponse>>() {
					}));

			for (MdmDeviceInfoResponse deviceInfoResponse : deviceInfoResponses) {

				if (deviceInfoResponse.getDeviceInfo() != null && !deviceInfoResponse.getDeviceInfo().isEmpty()) {
					mdmDeviceInfos.add(getDeviceInfoDecoded(deviceInfoResponse.getDeviceInfo()));
				}
			}

		} catch (IOException exception) {
			LOGGER.error(MOSIP_BIO_DEVICE_INTEGERATOR, APPLICATION_NAME, APPLICATION_ID,
					String.format(
							"%s -> Exception while initializing Fingerprint Capture page for user registration  %s",
							RegistrationConstants.USER_REG_FINGERPRINT_PAGE_LOAD_EXP,
							exception.getMessage() + ExceptionUtils.getStackTrace(exception)));

		}

		return mdmDeviceInfos;
	}

	public InputStream getStream(String modality) throws MalformedURLException, IOException {
		MdmBioDevice bioDevice = getDeviceInfoByModality(modality);

		if (bioDevice != null) {

			String url = bioDevice.getCallbackId() + MosipBioDeviceConstants.STREAM_ENDPOINT;
			switch (bioDevice.getSpecVersion()) {
			case "0.9.5":
				return mdm_095_IntegratorImpl.stream(url,
						new StreamRequestDTO(bioDevice.getDeviceId(), getDeviceSubId(modality)));
			case "0.9.2":
				return null;
			}
		}
		return null;
	}

	public List<BiometricsDto> getRCaptureBiometrics(MDMRequestDto rCaptureRequest)
			throws JsonParseException, JsonMappingException, IOException {

		List<BiometricsDto> biometricDTOs = new LinkedList<>();
		MdmBioDevice bioDevice = getDeviceInfoByModality(rCaptureRequest.getModality());

		if (bioDevice != null) {

			String url = bioDevice.getCallbackId() + MosipBioDeviceConstants.CAPTURE_ENDPOINT;

			switch (bioDevice.getSpecVersion()) {
			case "0.9.5":
				List<RCaptureResponseBiometricsDTO> captureResponseBiometricsDTOs = (List<RCaptureResponseBiometricsDTO>) mdm_095_IntegratorImpl
						.rCapture(url, get_095_RCaptureRequest(bioDevice, rCaptureRequest));
				for (RCaptureResponseBiometricsDTO rCaptureResponseBiometricsDTO : captureResponseBiometricsDTOs) {

					String payLoad = getPayLoad(rCaptureResponseBiometricsDTO.getData());

					RCaptureResponseDataDTO dataDTO = (RCaptureResponseDataDTO) (mapper.readValue(
							new String(Base64.getUrlDecoder().decode(payLoad)), RCaptureResponseDataDTO.class));

					BiometricsDto biometricDTO = new BiometricsDto(dataDTO.getBioSubType(),
							dataDTO.getDecodedBioValue(), Double.parseDouble(dataDTO.getQualityScore()));
					biometricDTO.setCaptured(true);
					biometricDTO.setModalityName(bioDevice.getDeviceType());
					biometricDTOs.add(biometricDTO);
				}
			case "0.9.2":
				break;
			}
		}
		return biometricDTOs;
	}

	private RCaptureRequestDTO get_095_RCaptureRequest(MdmBioDevice bioDevice, MDMRequestDto rCaptureRequest)
			throws JsonParseException, JsonMappingException, IOException {

		List<RCaptureRequestBioDTO> captureRequestBioDTOs = new LinkedList<>();
		captureRequestBioDTOs.add(new RCaptureRequestBioDTO(bioDevice.getDeviceType(), "1", null,
				rCaptureRequest.getExceptions(), String.valueOf(rCaptureRequest.getRequestedScore()),
				bioDevice.getDeviceId(), getDeviceSubId(rCaptureRequest.getModality()), null));

		RCaptureRequestDTO rCaptureRequestDTO = new RCaptureRequestDTO(rCaptureRequest.getEnvironment(), "Registration",
				"0.9.5", String.valueOf(rCaptureRequest.getTimeout()),
				LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).toString(),
				String.valueOf(generateID()), captureRequestBioDTOs, null);

		return rCaptureRequestDTO;
	}

	private static long generateID() {

		Random rnd = new Random();
		char[] digits = new char[10];
		digits[0] = (char) (rnd.nextInt(9) + '1');
		for (int i = 1; i < digits.length; i++) {
			digits[i] = (char) (rnd.nextInt(10) + '0');
		}
		return Long.parseLong(new String(digits));
	}

	private String getDeviceSubId(String modality) {
		modality = modality.toLowerCase();

		return modality.contains("left") ? "1"
				: modality.contains("right") ? "2"
						: (modality.contains("double") || modality.contains("thumbs") || modality.contains("two")) ? "3"
								: modality.contains("face") ? "0" : "0";
	}

	private String getLatestSpecVersion(String[] specVersion) {

		String latestSpecVersion = specVersion[0];
		for (int index = 1; index < specVersion.length; index++) {

			latestSpecVersion = getLatestVersion(latestSpecVersion, specVersion[index]);
		}
		return latestSpecVersion;
	}

	private MdmBioDevice getDeviceInfoByModality(String modality) {

		String key = String.format("%s_%s", getDeviceType(modality).toLowerCase(),
				getDeviceSubType(modality).toLowerCase());

		if (deviceInfoMap.containsKey(key)) {
			return deviceInfoMap.get(key);
		} else {
			try {
				initByPort(null);
				if (deviceInfoMap.containsKey(key)) {
					return deviceInfoMap.get(key);
				}
			} catch (RegBaseCheckedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		return null;
	}

	private String getDeviceIdByModality(String modality) {

		String deviceId = null;
		MdmBioDevice bioDevice = getDeviceInfoByModality(modality);

		if (bioDevice != null) {

			deviceId = bioDevice.getDeviceId();
		}

		return deviceId;
	}

	private String getLatestVersion(String version1, String version2) {

		if (version1.equalsIgnoreCase(version2)) {
			return version1;
		}
		int version1Num = 0, version2Num = 0;

		for (int index = 0, limit = 0; (index < version1.length() || limit < version2.length());) {

			while (index < version1.length() && version1.charAt(index) != '.') {
				version1Num = version1Num * 10 + (version1.charAt(index) - '0');
				index++;
			}

			while (limit < version2.length() && version2.charAt(limit) != '.') {
				version2Num = version2Num * 10 + (version2.charAt(limit) - '0');
				limit++;
			}

			if (version1Num > version2Num)
				return version1;
			if (version2Num > version1Num)
				return version2;

			version1Num = version2Num = 0;
			index++;
			limit++;
		}
		return version1;
	}

	public String getLatestSpecVersion() {

		return getLatestSpecVersion(Biometric.getAvailableSpecVersions().toArray(new String[0]));

	}

	public String getSpecVersionByModality(String modality) {

		MdmBioDevice bioDevice = getDeviceInfoByModality(modality);

		if (bioDevice != null) {
			return bioDevice.getSpecVersion();
		}
		return null;

	}
}
