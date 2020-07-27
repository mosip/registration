
package io.mosip.registration.mdm.service.impl;

import static io.mosip.registration.constants.LoggerConstants.MOSIP_BIO_DEVICE_INTEGERATOR;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dao.impl.RegisteredDeviceDAO;
import io.mosip.registration.entity.RegisteredDeviceMaster;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.mdm.constants.MosipBioDeviceConstants;
import io.mosip.registration.mdm.dto.Biometric;
import io.mosip.registration.mdm.dto.DeviceDiscoveryResponsetDto;
import io.mosip.registration.mdm.dto.MdmBioDevice;
import io.mosip.registration.mdm.integrator.MosipDeviceSpecificationProvider;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;

/**
 * 
 * Handles all the Biometric Devices controls
 * 
 * @author balamurugan.ramamoorthy
 * 
 */
@Component
public class MosipDeviceSpecificationFactory {

	@Autowired
	private AuditManagerService auditFactory;

	//
	private int portFrom;

	private int portTo;

	@Value("${mosip.registration.mdm.default.portRangeFrom}")
	private int defaultMDSPortFrom;

	@Value("${mosip.registration.mdm.default.portRangeTo}")
	private int defaultMDSPortTo;
	private static final Logger LOGGER = AppConfig.getLogger(MosipDeviceSpecificationFactory.class);

	private ObjectMapper mapper = new ObjectMapper();

	private static final String loggerClassName = "MosipDeviceSpecificationFactory";

	public ObjectMapper getMapper() {
		return mapper;
	}

	public void setMapper(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	@Autowired
	private List<MosipDeviceSpecificationProvider> deviceSpecificationProviders;

	/** Key is modality value is (specVersion, MdmBioDevice) */
	private static Map<String, MdmBioDevice> deviceInfoMap = new LinkedHashMap<>();

	@Autowired
	private RegisteredDeviceDAO registeredDeviceDAO;

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
		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"Entering init method for preparing device registry");

		portFrom = getPortFrom();
		portTo = getPortTo();

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"Checking device info from port : " + portFrom + " to port : " + portTo);
		if (portFrom != 0) {
			for (int port = portFrom; port <= portTo; port++) {

				final int currentPort = port;

				/* An A-sync task to complete MDS initialization */
				new Thread(() -> {
					try {
						initByPort(currentPort);
					} catch (RuntimeException exception) {
						LOGGER.error(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
								"Exception while mapping the response : " + exception.getMessage()
										+ ExceptionUtils.getStackTrace(exception));
					}
				}).start();

			}
		}
		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"Exit init method for preparing device registry");
	}

	private int getPortTo() {

		if (ApplicationContext.map().get(RegistrationConstants.MDM_END_PORT_RANGE) != null) {
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Found port To configuration in application context map");

			try {
				return Integer
						.parseInt((String) ApplicationContext.map().get(RegistrationConstants.MDM_END_PORT_RANGE));
			} catch (RuntimeException runtimeException) {
				LOGGER.error(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
						"Exception while mapping the response : " + runtimeException.getMessage()
								+ ExceptionUtils.getStackTrace(runtimeException));
				LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
						"Found port To configuration in application context map but exception while parsing to integer, returning default");
				return defaultMDSPortTo;
			}
		} else {
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Not Found port To configuration in application context map so intializing default  value");

			return defaultMDSPortTo;
		}
	}

	private int getPortFrom() {
		if (ApplicationContext.map().get(RegistrationConstants.MDM_START_PORT_RANGE) != null) {
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Found port from configuration in application context map");

			try {
				return Integer
						.parseInt((String) ApplicationContext.map().get(RegistrationConstants.MDM_START_PORT_RANGE));
			} catch (RuntimeException runtimeException) {
				LOGGER.error(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
						"Exception while mapping the response : " + runtimeException.getMessage()
								+ ExceptionUtils.getStackTrace(runtimeException));
				LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
						"Found port From configuration in application context map but exception while parsing to integer, returning default value");
				return defaultMDSPortFrom;
			}
		} else {
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Not Found port from configuration in application context map so intializing default  value");

			return defaultMDSPortFrom;
		}
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

	public void initByPort(Integer availablePort) {

		LOGGER.debug(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"Initializing device " + " on Port : " + availablePort);

		if (availablePort != null && availablePort != 0) {

			LOGGER.debug(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Initializing device " + " on Port : " + availablePort);

			String url;

			url = buildUrl(availablePort, MosipBioDeviceConstants.DEVICE_INFO_ENDPOINT);

			LOGGER.debug(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Checking device info on url : " + url);

			/* check if the service is available for the current port */
			if (checkServiceAvailability(url, "MOSIPDINFO")) {

				try {
					String deviceInfoResponse = getDeviceInfoResponse(url);

					for (MosipDeviceSpecificationProvider deviceSpecificationProvider : deviceSpecificationProviders) {

						LOGGER.debug(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
								"Decoding deice info response with provider : " + deviceSpecificationProvider);

						List<MdmBioDevice> mdmBioDevices = deviceSpecificationProvider.getMdmDevices(deviceInfoResponse,
								availablePort);
						for (MdmBioDevice bioDevice : mdmBioDevices) {

							if (bioDevice != null) {

								LOGGER.debug(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
										"Checking for device registratrion : " + bioDevice.getDeviceCode());

								List<RegisteredDeviceMaster> registeredDevices = registeredDeviceDAO
										.getRegisteredDevices(bioDevice.getDeviceCode(), bioDevice.getSerialNumber());

								if (registeredDevices != null && !registeredDevices.isEmpty()) {
									LOGGER.debug(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
											"Device Registration found : " + bioDevice.getDeviceCode());

									// Add to Device Info Map
									addToDeviceInfoMap(getDeviceType(bioDevice.getDeviceType()).toLowerCase(),
											getDeviceSubType(bioDevice.getDeviceSubType()), bioDevice);
								}
							}
						}
					}
				} catch (RuntimeException runtimeException) {
					LOGGER.error(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
							runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));

				}

			}

			else {
				LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
						"No device is running at port number " + availablePort);
			}

		} else {
			portFrom = getPortFrom();

			portTo = getPortTo();

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Checking device info from port : " + portFrom + " to port : " + portTo);
			ExecutorService executorService = Executors.newFixedThreadPool(5);

			for (int port = portFrom; port <= portTo; port++) {

				int currentPort = port;
				executorService.execute(new Runnable() {
					public void run() {

						initByPort(currentPort);

					}
				});

			}

			try {
				executorService.shutdown();
				executorService.awaitTermination(500, TimeUnit.SECONDS);
			} catch (Exception exception) {
				executorService.shutdown();
				LOGGER.error(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
						exception.getMessage() + ExceptionUtils.getStackTrace(exception));

			}
		}
	}

	private void addToDeviceInfoMap(String type, String subType, MdmBioDevice bioDevice) {

		String key = String.format("%s_%s", type.toLowerCase(), subType.toLowerCase());

		deviceInfoMap.put(key, bioDevice);

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

	public String getPayLoad(String data) throws RegBaseCheckedException {

		if (data == null || data.isEmpty()) {
			throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_JWT_INVALID.getErrorCode(),
					RegistrationExceptionConstants.MDS_JWT_INVALID.getErrorMessage());
		}
		String payLoad = null;
		Pattern pattern = Pattern.compile(RegistrationConstants.BIOMETRIC_SEPERATOR);
		Matcher matcher = pattern.matcher(data);
		if (matcher.find()) {
			payLoad = matcher.group(1);
		}

		if (payLoad == null) {
			throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_PAYLOAD_EMPTY.getErrorCode(),
					RegistrationExceptionConstants.MDS_PAYLOAD_EMPTY.getErrorMessage());
		}
		return payLoad;
	}

	public String buildUrl(int port, String endPoint) {
		return getRunningurl() + ":" + port + "/" + endPoint;
	}

	private String getRunningurl() {
		return "http" + "://" + "127.0.0.1";
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
				LOGGER.debug(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "this" + url + " is unavailable");
				auditFactory.audit(AuditEvent.MDM_NO_DEVICE_AVAILABLE, Components.MDM_NO_DEVICE_AVAILABLE,
						RegistrationConstants.APPLICATION_NAME,
						AuditReferenceIdTypes.APPLICATION_ID.getReferenceTypeId());

			}

		}
		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Device discovery completed");

		return deviceDiscoveryResponsetDtos;

	}

	private String getDeviceInfoResponse(String url) {

		HttpUriRequest request = RequestBuilder.create("MOSIPDINFO").setUri(url).build();
		CloseableHttpClient client = HttpClients.createDefault();
		CloseableHttpResponse clientResponse = null;
		String response = null;

		try {
			clientResponse = client.execute(request);
			response = EntityUtils.toString(clientResponse.getEntity());
		} catch (IOException exception) {
			LOGGER.error(MOSIP_BIO_DEVICE_INTEGERATOR, APPLICATION_NAME, APPLICATION_ID,
					String.format(
							"%s -> Exception while initializing Fingerprint Capture page for user registration  %s",
							RegistrationConstants.USER_REG_FINGERPRINT_PAGE_LOAD_EXP,
							exception.getMessage() + ExceptionUtils.getStackTrace(exception)));

		}

		return response;

	}

	public static long generateID() {

		Random rnd = new Random();
		char[] digits = new char[10];
		digits[0] = (char) (rnd.nextInt(9) + '1');
		for (int i = 1; i < digits.length; i++) {
			digits[i] = (char) (rnd.nextInt(10) + '0');
		}
		return Long.parseLong(new String(digits));
	}

	public String getLatestSpecVersion(String[] specVersion) {

		String latestSpecVersion = null;
		if (specVersion != null && specVersion.length > 0) {
			latestSpecVersion = specVersion[0];
			for (int index = 1; index < specVersion.length; index++) {

				latestSpecVersion = getLatestVersion(latestSpecVersion, specVersion[index]);
			}

			if (getMdsProvider(deviceSpecificationProviders, latestSpecVersion) == null) {
				List<String> specVersions = Arrays.asList(specVersion);

				specVersions.remove(latestSpecVersion);

				if (!specVersions.isEmpty()) {
					latestSpecVersion = getLatestSpecVersion(specVersions.toArray(new String[0]));
				}
			}

		}

		return latestSpecVersion;
	}

	public MdmBioDevice getDeviceInfoByModality(String modality) throws RegBaseCheckedException {

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

				LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Bio Device not found for modality : "
						+ modality + "  " + System.currentTimeMillis() + modality);
				throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_BIODEVICE_NOT_FOUND.getErrorCode(),
						RegistrationExceptionConstants.MDS_BIODEVICE_NOT_FOUND.getErrorMessage());

			} catch (RegBaseCheckedException exception) {

				throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_BIODEVICE_NOT_FOUND.getErrorCode(),
						RegistrationExceptionConstants.MDS_BIODEVICE_NOT_FOUND.getErrorMessage(), exception);

			}

		}

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

	public String getSpecVersionByModality(String modality) throws RegBaseCheckedException {

		MdmBioDevice bioDevice = getDeviceInfoByModality(modality);

		if (bioDevice != null) {
			return bioDevice.getSpecVersion();
		}
		return null;

	}

	public MosipDeviceSpecificationProvider getMdsProvider(String specVersion) throws RegBaseCheckedException {

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"Finding MosipDeviceSpecificationProvider for spec version : " + specVersion);

		MosipDeviceSpecificationProvider deviceSpecificationProvider = getMdsProvider(deviceSpecificationProviders,
				specVersion);

		if (deviceSpecificationProvider == null) {
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"MosipDeviceSpecificationProvider not found for spec version : " + specVersion);
			throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_PROVIDER_NOT_FOUND.getErrorCode(),
					RegistrationExceptionConstants.MDS_PROVIDER_NOT_FOUND.getErrorMessage());
		}
		return deviceSpecificationProvider;
	}

	private MosipDeviceSpecificationProvider getMdsProvider(
			List<MosipDeviceSpecificationProvider> deviceSpecificationProviders, String specVersion) {

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"Finding MosipDeviceSpecificationProvider for spec version : " + specVersion + " in providers : "
						+ deviceSpecificationProviders);

		MosipDeviceSpecificationProvider deviceSpecificationProvider = null;

		if (deviceSpecificationProviders != null) {
			// Get Implemented provider
			for (MosipDeviceSpecificationProvider provider : deviceSpecificationProviders) {
				if (provider.getSpecVersion().equals(specVersion)) {
					deviceSpecificationProvider = provider;
					break;
				}
			}
		}
		return deviceSpecificationProvider;
	}

	public static Map<String, MdmBioDevice> getDeviceRegistryInfo() {

		return deviceInfoMap;
	}

}
