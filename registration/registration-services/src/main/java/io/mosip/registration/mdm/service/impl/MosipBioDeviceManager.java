
package io.mosip.registration.mdm.service.impl;

import static io.mosip.registration.constants.LoggerConstants.MOSIP_BIO_DEVICE_MANAGER;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
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
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.mdm.constants.MosipBioDeviceConstants;
import io.mosip.registration.mdm.dto.BioDevice;
import io.mosip.registration.mdm.dto.CaptureResponseBioDto;
import io.mosip.registration.mdm.dto.CaptureResponseDto;
import io.mosip.registration.mdm.dto.DeviceDiscoveryResponsetDto;
import io.mosip.registration.mdm.dto.DeviceInfo;
import io.mosip.registration.mdm.dto.DeviceInfoResponseData;
import io.mosip.registration.mdm.dto.RequestDetail;
import io.mosip.registration.mdm.integrator.IMosipBioDeviceIntegrator;
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

	@Value("${mdm.host}")
	private String host;

	@Value("${mdm.hostProtocol}")
	private String hostProtocol;

	@Value("${mdm.portRangeFrom}")
	private int portFrom;

	@Value("${mdm.portRangeTo}")
	private int portTo;

	@Autowired
	private IMosipBioDeviceIntegrator mosipBioDeviceIntegrator;
	
	@Autowired
	private RegisteredDeviceDAO registeredDeviceDAO;
	
	private static Map<String, BioDevice> deviceRegistry = new HashMap<>();

	private static final Logger LOGGER = AppConfig.getLogger(MosipBioDeviceManager.class);
	
	ObjectMapper mapper = new ObjectMapper();

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
	 * @throws RegBaseCheckedException - generalised exception with errorCode and
	 *                                 errorMessage
	 */
	@SuppressWarnings("unchecked")
	public void init() throws RegBaseCheckedException {
		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
				"Entering init method for preparing device registry");

		for (int port = portFrom; port <= portTo; port++) {

			try {
				initByPort(port);
			} catch (RegBaseCheckedException exception) {
				LOGGER.error(LoggerConstants.LOG_SERVICE_DELEGATE_UTIL_GET, APPLICATION_NAME, APPLICATION_ID,
						String.format("Exception while mapping the response",
								exception.getMessage() + ExceptionUtils.getStackTrace(exception)));
			}

		}
		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
				"Exit init method for preparing device registry");
	}

	private void initByPort(int port) throws RegBaseCheckedException {
		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID, "Initializing on port : " + port);

	}

	private void initByDeviceType(String constructedDeviceType) throws RegBaseCheckedException {

		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
				"Initializing device : " + constructedDeviceType);
		initByPortAndDeviceType(null, constructedDeviceType);

	}

	private void initByPortAndDeviceType(Integer availablePort, String deviceType) throws RegBaseCheckedException {

		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
				"Initializing device : " + deviceType + " ,on Port : " + availablePort);

		if (availablePort != null) {

			String url;
			ObjectMapper mapper = new ObjectMapper();

			url = buildUrl(availablePort, MosipBioDeviceConstants.DEVICE_INFO_ENDPOINT);
			/* check if the service is available for the current port */
			if (RegistrationAppHealthCheckUtil.checkServiceAvailability(url)) {
				List<LinkedHashMap<String, String>> deviceInfoResponseDtos = null;
				String response = (String) mosipBioDeviceIntegrator.getDeviceInfo(url, Object[].class);
				try {
					deviceInfoResponseDtos = mapper.readValue(response, List.class);
				} catch (IOException exception) {
					 throw new RegBaseCheckedException();
				}

				if (MosioBioDeviceHelperUtil.isListNotEmpty(deviceInfoResponseDtos)) {

					getDeviceInfoResponse(mapper, availablePort, deviceInfoResponseDtos, deviceType);
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
	 * @param deviceInfoResponse     {@link DeviceInfoResponseData} -Contains the
	 *                               details of a specific bio device
	 * @param port                   - The port in which the bio device is active
	 * @param deviceInfoResponseDtos - This list will contain the response that we
	 *                               receive after finding the device
	 * @return {@link DeviceInfoResponseData}
	 * @throws RegBaseCheckedException 
	 */
	private DeviceInfoResponseData getDeviceInfoResponse(ObjectMapper mapper, int port,
			List<LinkedHashMap<String, String>> deviceInfoResponseDtos, String deviceType) throws RegBaseCheckedException {
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

			creationOfBioDeviceObject(deviceInfoResponse, port, deviceType);
		}
		return deviceInfoResponse;
	}

	/**
	 * This method will save the device details into the device registry
	 *
	 * @param deviceInfoResponse the device info response
	 * @param port               the port number
	 */
	private void creationOfBioDeviceObject(DeviceInfoResponseData deviceInfoResponse, int port, String deviceType) {

		if (deviceInfoResponse != null && deviceType != null) {
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

			if (digitalId != null) {
				bioDevice.setDeviceSubType(digitalId.getSubType());
				bioDevice.setDeviceType(digitalId.getType());
				bioDevice.setTimestamp(digitalId.getDateTime());
				bioDevice.setDeviceProviderName(digitalId.getDeviceProvider());
				bioDevice.setDeviceProviderId(digitalId.getDeviceProviderId());
				bioDevice.setDeviceModel(digitalId.getModel());
				bioDevice.setDeviceMake(digitalId.getMake());

			}
			
			bioDevice.setRegistered(registeredDeviceDAO.getRegisteredDevices(deviceInfo.getDeviceCode(),digitalId.getSerialNo()).size()==1?true:false);
			String isDeviceValidationEnabled = ((String)ApplicationContext.getInstance().map().get("isDeviceValidationEnabled"));
			if(isDeviceValidationEnabled==null)
				bioDevice.setRegistered(true);
			else
				bioDevice.setRegistered("".equals("NO")?true:bioDevice.isRegistered());
			bioDevice.checkForSpec();
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
		return hostProtocol + "://" + host;
	}

	/**
	 * Triggers the biometric capture based on the device type and returns the
	 * biometric value from MDM
	 * 
	 * @param deviceType - The type of the device
	 * @return CaptureResponseDto - captured biometric values from the device
	 * @throws RegBaseCheckedException - generalised exception with errorCode and
	 *                                 errorMessage
	 * @throws IOException
	 */
	public CaptureResponseDto regScan(RequestDetail requestDetail)
			throws RegBaseCheckedException, IOException {

		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
				"scan method calling..."+  System.currentTimeMillis());
		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
				"Finding the device from device Registry Started..."+  System.currentTimeMillis());
		BioDevice bioDevice = findDeviceToScan(requestDetail.getType());

		if (bioDevice != null) {
			LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
					"Device found in the device registery..."+  System.currentTimeMillis());
			
			if (bioDevice.isSpecVersionValid()) {
				
				LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
						"If Device Found next capture is being called..."+  System.currentTimeMillis());
				if(!bioDevice.isRegistered())
					throw new RegBaseCheckedException("102", "");
				return bioDevice.regCapture(requestDetail);
			}
			throw new RegBaseCheckedException("101", "");
		} else {
			LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
					"Device not found in the device registery - scan"+  System.currentTimeMillis());
			return null;
		}

	}

	/**
	 * Triggers the biometric capture based on the device type and returns the
	 * biometric value from MDM
	 * 
	 * @param deviceType - The type of the device
	 * @return CaptureResponseDto - captured biometric values from the device
	 * @throws RegBaseCheckedException - generalised exception with errorCode and
	 *                                 errorMessage
	 * @throws IOException
	 */
	public CaptureResponseDto authScan(RequestDetail requestDetail) throws RegBaseCheckedException, IOException {
		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
				"Entering into Auth Scan Method..."+ System.currentTimeMillis());	

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
					"Leaving Auth Scan Method..."+ System.currentTimeMillis());
			return captureResponse;

		} else {
			LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
					"Device not found in the device registery - authScan" + System.currentTimeMillis());
			return null;
		}

	}

	private BioDevice findDeviceToScan(String deviceType) throws RegBaseCheckedException {
		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID, "Entering findDeviceToScan method...." + System.currentTimeMillis());

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
		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID, "Leaving findDeviceToScan method...." + System.currentTimeMillis());
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
	 * @param captureResponseDto - Response Data object {@link CaptureResponseDto}
	 *                           which contains the captured biometrics from MDM
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
	 * @param captureResponseDto - Response object which contains the capture
	 *                           biometrics from MDM
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
	 * @param deviceType - type of bio device
	 * @return List - list of device details
	 * @throws RegBaseCheckedException - generalized exception with errorCode and
	 *                                 errorMessage
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
	 * @param type - device type
	 */
	public void deRegister(String type) {
		deviceRegistry.remove(type);
		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID,
				"Removed" + type + " from Device registry");

	}

	/**
	 * Gets the bio device.
	 *
	 * @param type     - the type of device
	 * @param modality - the modality
	 */
	public void getBioDevice(String type, String modality) {

	}

	public void refreshBioDeviceByDeviceType(String deviceType) throws RegBaseCheckedException {

		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID, "Refreshing device of : " + deviceType);

		BioDevice bioDevice = deviceRegistry.get(constructDeviceType(deviceType));

		if (bioDevice != null) {
			initByPortAndDeviceType(bioDevice.getRunningPort(), deviceType);
		}

	}
}
