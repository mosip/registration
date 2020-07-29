package io.mosip.registration.mdm.spec_0_9_5.service.impl;

import static io.mosip.registration.constants.LoggerConstants.MOSIP_BIO_DEVICE_MANAGER;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.Consts;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.packetmanager.dto.BiometricsDto;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.mdm.constants.MosipBioDeviceConstants;
import io.mosip.registration.mdm.dto.Biometric;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import io.mosip.registration.mdm.dto.MdmBioDevice;
import io.mosip.registration.mdm.dto.MdmDeviceInfo;
import io.mosip.registration.mdm.integrator.MosipDeviceSpecificationProvider;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationFactory;
import io.mosip.registration.mdm.spec_0_9_5.dto.request.RCaptureRequestBioDTO;
import io.mosip.registration.mdm.spec_0_9_5.dto.request.RCaptureRequestDTO;
import io.mosip.registration.mdm.spec_0_9_5.dto.request.StreamRequestDTO;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.DigitalId;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.MdmDeviceInfoResponse;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.RCaptureResponseBiometricsDTO;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.RCaptureResponseDTO;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.RCaptureResponseDataDTO;

@Service
public class MosipDeviceSpecification_095_ProviderImpl implements MosipDeviceSpecificationProvider {

	private static final Logger LOGGER = AppConfig.getLogger(MosipDeviceSpecification_095_ProviderImpl.class);

	private static final String SPEC_VERSION = "0.9.5";

	private static final String loggerClassName = "MosipDeviceSpecification_095_ProviderImpl";

	@Autowired
	private MosipDeviceSpecificationFactory deviceSpecificationFactory;

	@Override
	public String getSpecVersion() {
		return SPEC_VERSION;
	}

	@Override
	public List<MdmBioDevice> getMdmDevices(String deviceInfoResponse, int port) {

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"received device info response : " + deviceInfoResponse);

		List<MdmBioDevice> mdmBioDevices = new LinkedList<>();

		List<MdmDeviceInfo> mdmDeviceInfos = new LinkedList<>();

		List<MdmDeviceInfoResponse> deviceInfoResponses;
		try {

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "parsing device info response to 095 dto");

			deviceInfoResponses = (deviceSpecificationFactory.getMapper().readValue(deviceInfoResponse,
					new TypeReference<List<MdmDeviceInfoResponse>>() {
					}));

			for (MdmDeviceInfoResponse mdmDeviceInfoResponse : deviceInfoResponses) {

				if (mdmDeviceInfoResponse.getDeviceInfo() != null && !mdmDeviceInfoResponse.getDeviceInfo().isEmpty()) {
					mdmDeviceInfos.add(getDeviceInfoDecoded(mdmDeviceInfoResponse.getDeviceInfo()));
				}
			}

			for (MdmDeviceInfo deviceInfo : mdmDeviceInfos) {

				MdmBioDevice bioDevice = getBioDevice(deviceInfo);

				if (bioDevice != null) {

					LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "prepared bio Device");

					mdmBioDevices.add(bioDevice);

				}

			}
		} catch (Exception exception) {
			LOGGER.error(LoggerConstants.LOG_SERVICE_DELEGATE_UTIL_GET, APPLICATION_NAME, APPLICATION_ID,
					String.format(" Exception while mapping the response ",
							exception.getMessage() + ExceptionUtils.getStackTrace(exception)));
		}

		return mdmBioDevices;
	}

	@Override
	public InputStream stream(MdmBioDevice bioDevice, String modality) throws RegBaseCheckedException {
		try {
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Started Strema for modality : " + modality);

			String url = bioDevice.getCallbackId() + MosipBioDeviceConstants.STREAM_ENDPOINT;

			StreamRequestDTO streamRequestDTO = new StreamRequestDTO(bioDevice.getDeviceId(), getDeviceSubId(modality));

			HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
			con.setRequestMethod("POST");
			String request = new ObjectMapper().writeValueAsString(streamRequestDTO);

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Request for Stream...." + request);

			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Stream Request Started : " + System.currentTimeMillis());
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Stream Request :" + streamRequestDTO.toString());

			wr.writeBytes(request);
			wr.flush();
			wr.close();
			con.setReadTimeout(5000);
			con.connect();
			InputStream urlStream = con.getInputStream();
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Leaving into Stream Method.... " + System.currentTimeMillis());
			return urlStream;
		} catch (Exception exception) {
			throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_STREAM_ERROR.getErrorCode(),
					RegistrationExceptionConstants.MDS_STREAM_ERROR.getErrorMessage(), exception);

		}
	}

	@Override
	public List<BiometricsDto> rCapture(MdmBioDevice bioDevice, MDMRequestDto mdmRequestDto)
			throws RegBaseCheckedException {

		try {
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Entering into rCapture method for moadlity : " + mdmRequestDto.getModality() + "  ....."
							+ System.currentTimeMillis());

			if (mdmRequestDto.getExceptions() != null) {
				mdmRequestDto.setExceptions(getExceptions(mdmRequestDto.getExceptions()));
			}

			int count = getCount(getDefaultCount(mdmRequestDto.getModality()),
					mdmRequestDto.getExceptions() != null ? mdmRequestDto.getExceptions().length : 0);
			mdmRequestDto.setCount(count);

			RCaptureRequestDTO rCaptureRequestDTO = getRCaptureRequest(bioDevice, mdmRequestDto);

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Entering into Capture method....." + System.currentTimeMillis());

			String requestBody = null;
			ObjectMapper mapper = new ObjectMapper();
			requestBody = mapper.writeValueAsString(rCaptureRequestDTO);

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Request for RCapture...." + requestBody);

			CloseableHttpClient client = HttpClients.createDefault();
			StringEntity requestEntity = new StringEntity(requestBody,
					ContentType.create("Content-Type", Consts.UTF_8));
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Bulding capture url...." + System.currentTimeMillis());
			HttpUriRequest request = RequestBuilder.create("RCAPTURE")
					.setUri(bioDevice.getCallbackId() + MosipBioDeviceConstants.CAPTURE_ENDPOINT)
					.setEntity(requestEntity).build();
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Requesting capture url...." + System.currentTimeMillis());
			CloseableHttpResponse response = client.execute(request);
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Request completed.... " + System.currentTimeMillis());

			String val = EntityUtils.toString(response.getEntity());

			RCaptureResponseDTO captureResponse = mapper.readValue(val.getBytes(StandardCharsets.UTF_8),
					RCaptureResponseDTO.class);
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Response Recived.... " + System.currentTimeMillis());

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Response Decode and leaving the method.... " + System.currentTimeMillis());

			List<RCaptureResponseBiometricsDTO> captureResponseBiometricsDTOs = captureResponse.getBiometrics();

			List<BiometricsDto> biometricDTOs = new LinkedList<>();

			for (RCaptureResponseBiometricsDTO rCaptureResponseBiometricsDTO : captureResponseBiometricsDTOs) {

				LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
						"Getting data payload of biometric" + System.currentTimeMillis());
				if (rCaptureResponseBiometricsDTO.getData() == null
						|| rCaptureResponseBiometricsDTO.getData().isEmpty()) {
					throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_RCAPTURE_ERROR.getErrorCode(),
							RegistrationExceptionConstants.MDS_RCAPTURE_ERROR.getErrorMessage()
									+ " : Data is empty in RCapture " + " error Code  : "
									+ rCaptureResponseBiometricsDTO.getError().getErrorCode() + " error message : "
									+ rCaptureResponseBiometricsDTO.getError().getErrorInfo());
				}
				if (rCaptureResponseBiometricsDTO.getData() != null
						&& !rCaptureResponseBiometricsDTO.getData().isEmpty()) {
					String payLoad = deviceSpecificationFactory.getPayLoad(rCaptureResponseBiometricsDTO.getData());

					RCaptureResponseDataDTO dataDTO = (RCaptureResponseDataDTO) (mapper.readValue(
							new String(Base64.getUrlDecoder().decode(payLoad)), RCaptureResponseDataDTO.class));

					LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
							"Parsed decoded payload" + System.currentTimeMillis());

					if (dataDTO.getTransactionId() == null
							|| !dataDTO.getTransactionId().equalsIgnoreCase(rCaptureRequestDTO.getTransactionId())) {
						throw new RegBaseCheckedException(
								RegistrationExceptionConstants.MDS_RCAPTURE_ERROR.getErrorCode(),
								RegistrationExceptionConstants.MDS_RCAPTURE_ERROR.getErrorMessage()
										+ " : RCapture TransactionId Mismatch : " + " request transaction id is : "
										+ rCaptureRequestDTO.getTransactionId() + " and response transactionId is :"
										+ dataDTO.getTransactionId());
					}

					if (rCaptureResponseBiometricsDTO.getSpecVersion() == null || !rCaptureResponseBiometricsDTO
							.getSpecVersion().equalsIgnoreCase(rCaptureRequestDTO.getSpecVersion())) {
						throw new RegBaseCheckedException(
								RegistrationExceptionConstants.MDS_RCAPTURE_ERROR.getErrorCode(),
								RegistrationExceptionConstants.MDS_RCAPTURE_ERROR.getErrorMessage()
										+ " : RCapture spec version Mismatch : " + " request spec version is : "
										+ rCaptureRequestDTO.getSpecVersion() + " and response spec version is :"
										+ rCaptureResponseBiometricsDTO.getSpecVersion());
					}
					String uiAttribute = Biometric.getUiSchemaAttributeName(dataDTO.getBioSubType(), SPEC_VERSION);
					BiometricsDto biometricDTO = new BiometricsDto(uiAttribute, dataDTO.getDecodedBioValue(),
							Double.parseDouble(dataDTO.getQualityScore()));
					biometricDTO.setCaptured(true);
					biometricDTO.setModalityName(mdmRequestDto.getModality());
					biometricDTOs.add(biometricDTO);
				}
			}
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"rCapture Completed" + System.currentTimeMillis());

			return biometricDTOs;
		} catch (Exception exception) {
			throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_RCAPTURE_ERROR.getErrorCode(),
					RegistrationExceptionConstants.MDS_RCAPTURE_ERROR.getErrorMessage(), exception);
		}
	}

	private String[] getExceptions(String[] exceptions) {

		if (exceptions != null) {
			for (int index = 0; index < exceptions.length; index++) {
				exceptions[index] = io.mosip.registration.mdm.dto.Biometric
						.getmdmRequestAttributeName(exceptions[index], SPEC_VERSION);
			}

		}

		return exceptions;

	}

	private RCaptureRequestDTO getRCaptureRequest(MdmBioDevice bioDevice, MDMRequestDto mdmRequestDto)
			throws JsonParseException, JsonMappingException, IOException {

		RCaptureRequestDTO rCaptureRequestDTO = null;

		if (bioDevice != null) {
			List<RCaptureRequestBioDTO> captureRequestBioDTOs = new LinkedList<>();
			captureRequestBioDTOs.add(
					new RCaptureRequestBioDTO(bioDevice.getDeviceType(), Integer.toString(mdmRequestDto.getCount()),
							null, mdmRequestDto.getExceptions(), String.valueOf(mdmRequestDto.getRequestedScore()),
							bioDevice.getDeviceId(), getDeviceSubId(mdmRequestDto.getModality()), null));

			rCaptureRequestDTO = new RCaptureRequestDTO(mdmRequestDto.getEnvironment(), "Registration", "0.9.5",
					String.valueOf(mdmRequestDto.getTimeout()),
					LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).toString(),
					String.valueOf(deviceSpecificationFactory.generateID()), captureRequestBioDTOs, null);
		}

		return rCaptureRequestDTO;
	}

	private String getDeviceSubId(String modality) {
		modality = modality.toLowerCase();

		return modality.contains("left") ? "1"
				: modality.contains("right") ? "2"
						: (modality.contains("double") || modality.contains("thumbs") || modality.contains("two")) ? "3"
								: modality.contains("face") ? "0" : "0";
	}

	private MdmDeviceInfo getDeviceInfoDecoded(String deviceInfo) {
		try {
			String result = new String(
					Base64.getUrlDecoder().decode(deviceSpecificationFactory.getPayLoad(deviceInfo)));
			return (MdmDeviceInfo) (deviceSpecificationFactory.getMapper().readValue(result, MdmDeviceInfo.class));
		} catch (Exception exception) {
			LOGGER.error(LoggerConstants.LOG_SERVICE_DELEGATE_UTIL_GET, APPLICATION_NAME, APPLICATION_ID,
					String.format("%s -> Exception while trying to extract the response through regex  %s",
							exception.getMessage() + ExceptionUtils.getStackTrace(exception)));

		}
		return null;

	}

	private MdmBioDevice getBioDevice(MdmDeviceInfo deviceInfo)
			throws JsonParseException, JsonMappingException, IOException, RegBaseCheckedException {

		MdmBioDevice bioDevice = null;

		if (deviceInfo != null) {

			DigitalId digitalId = getDigitalId(deviceInfo.getDigitalId());

			bioDevice = new MdmBioDevice();
			bioDevice.setDeviceId(deviceInfo.getDeviceId());
			bioDevice.setFirmWare(deviceInfo.getFirmware());
			bioDevice.setCertification(deviceInfo.getCertification());
			bioDevice.setSerialVersion(deviceInfo.getServiceVersion());
			bioDevice.setSpecVersion(deviceSpecificationFactory.getLatestSpecVersion(deviceInfo.getSpecVersion()));
			bioDevice.setPurpose(deviceInfo.getPurpose());
			bioDevice.setDeviceCode(deviceInfo.getDeviceCode());

			bioDevice.setDeviceSubType(digitalId.getDeviceSubType());
			bioDevice.setDeviceType(digitalId.getType());
			bioDevice.setTimestamp(digitalId.getDateTime());
			bioDevice.setDeviceProviderName(digitalId.getDeviceProvider());
			bioDevice.setDeviceProviderId(digitalId.getDeviceProviderId());
			bioDevice.setDeviceModel(digitalId.getModel());
			bioDevice.setDeviceMake(digitalId.getMake());
			bioDevice.setSerialNumber(digitalId.getSerialNo());
			bioDevice.setCallbackId(deviceInfo.getCallbackId());
		}

		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID, "Adding Device to Registry : ");
		return bioDevice;
	}

	private DigitalId getDigitalId(String digitalId)
			throws JsonParseException, JsonMappingException, IOException, RegBaseCheckedException {
		return (DigitalId) (deviceSpecificationFactory.getMapper().readValue(
				new String(Base64.getUrlDecoder().decode(deviceSpecificationFactory.getPayLoad(digitalId))),
				DigitalId.class));

	}

	private int getDefaultCount(String modality) {
		int defaultCount = 1;
		if (modality != null) {
			switch (modality) {
			case RegistrationConstants.FACE_FULLFACE:
				defaultCount = 1;
				break;
			case RegistrationConstants.IRIS_DOUBLE:
				defaultCount = 2;
				break;
			case RegistrationConstants.FINGERPRINT_SLAB_RIGHT:
				defaultCount = 4;
				break;
			case RegistrationConstants.FINGERPRINT_SLAB_LEFT:
				defaultCount = 4;
				break;
			case RegistrationConstants.FINGERPRINT_SLAB_THUMBS:
				defaultCount = 2;
				break;
			}
		}
		return defaultCount;
	}

	private int getCount(int defaultCount, int exceptionsCount) {
		return defaultCount - exceptionsCount;
	}
}
