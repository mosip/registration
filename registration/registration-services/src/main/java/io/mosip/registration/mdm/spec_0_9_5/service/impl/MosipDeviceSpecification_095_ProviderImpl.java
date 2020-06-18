package io.mosip.registration.mdm.spec_0_9_5.service.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

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
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.packetmanager.dto.BiometricsDto;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.mdm.constants.MosipBioDeviceConstants;
import io.mosip.registration.mdm.dto.CaptureResponseDto;
import io.mosip.registration.mdm.dto.DeviceDiscoveryResponsetDto;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import io.mosip.registration.mdm.dto.MdmBioDevice;
import io.mosip.registration.mdm.integrator.IMosipBioDeviceIntegrator;
import io.mosip.registration.mdm.integrator.MosipDeviceSpecificationProvider;
import io.mosip.registration.mdm.service.impl.MosipBioDeviceManager;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationFactory;
import io.mosip.registration.mdm.spec_0_9_5.dto.request.RCaptureRequestBioDTO;
import io.mosip.registration.mdm.spec_0_9_5.dto.request.RCaptureRequestDTO;
import io.mosip.registration.mdm.spec_0_9_5.dto.request.StreamRequestDTO;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.MdmDeviceInfoResponse;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.RCaptureResponseBiometricsDTO;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.RCaptureResponseDTO;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.RCaptureResponseDataDTO;

@Service
public class MosipDeviceSpecification_095_ProviderImpl implements MosipDeviceSpecificationProvider {

	private static final Logger LOGGER = AppConfig.getLogger(MosipDeviceSpecification_095_ProviderImpl.class);

	private static final String SPEC_VERSION = "0.9.5";

	@Autowired
	private MosipDeviceSpecificationFactory deviceSpecificationFactory;

	@Override
	public String getSpecVersion() {
		return SPEC_VERSION;
	}

	@Override
	public List<MdmBioDevice> getMdmDevices(String deviceInfoResponse) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream stream(MdmBioDevice bioDevice, String modality) throws MalformedURLException, IOException {

		String url = bioDevice.getCallbackId() + MosipBioDeviceConstants.STREAM_ENDPOINT;

		StreamRequestDTO streamRequestDTO = new StreamRequestDTO(bioDevice.getDeviceId(), getDeviceSubId(modality));

		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.setRequestMethod("POST");
		String request = new ObjectMapper().writeValueAsString(streamRequestDTO);

		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());

		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID,
				"Stream Request Started : " + System.currentTimeMillis());
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID, "Stream Request :" + streamRequestDTO.toString());

		wr.writeBytes(request);
		wr.flush();
		wr.close();
		con.setReadTimeout(5000);
		con.connect();
		InputStream urlStream = con.getInputStream();
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID,
				"Leaving into Stream Method.... " + System.currentTimeMillis());
		return urlStream;

	}

	@Override
	public List<BiometricsDto> rCapture(MdmBioDevice bioDevice, MDMRequestDto mdmRequestDto)
			throws JsonParseException, JsonMappingException, ParseException, IOException {

		if (mdmRequestDto.getExceptions() != null) {
			mdmRequestDto.setExceptions(getExceptions(mdmRequestDto.getExceptions()));
		}

		RCaptureRequestDTO rCaptureRequestDTO = getRCaptureRequest(bioDevice, mdmRequestDto);

		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID,
				"Entering into Capture method....." + System.currentTimeMillis());

		String requestBody = null;
		ObjectMapper mapper = new ObjectMapper();
		requestBody = mapper.writeValueAsString(rCaptureRequestDTO);

		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID, "Request for RCapture...." + requestBody);

		CloseableHttpClient client = HttpClients.createDefault();
		StringEntity requestEntity = new StringEntity(requestBody, ContentType.create("Content-Type", Consts.UTF_8));
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID,
				"Bulding capture url...." + System.currentTimeMillis());
		HttpUriRequest request = RequestBuilder.create("RCAPTURE")
				.setUri(bioDevice.getCallbackId() + MosipBioDeviceConstants.CAPTURE_ENDPOINT).setEntity(requestEntity)
				.build();
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID,
				"Requesting capture url...." + System.currentTimeMillis());
		CloseableHttpResponse response = client.execute(request);
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID,
				"Request completed.... " + System.currentTimeMillis());

		String val = EntityUtils.toString(response.getEntity());

		RCaptureResponseDTO captureResponse = mapper.readValue(val.getBytes(StandardCharsets.UTF_8),
				RCaptureResponseDTO.class);
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID,
				"Response Recived.... " + System.currentTimeMillis());

		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID,
				"Response Decode and leaving the method.... " + System.currentTimeMillis());

		List<RCaptureResponseBiometricsDTO> captureResponseBiometricsDTOs = captureResponse.getBiometrics();

		List<BiometricsDto> biometricDTOs = new LinkedList<>();

		for (RCaptureResponseBiometricsDTO rCaptureResponseBiometricsDTO : captureResponseBiometricsDTOs) {

			String payLoad = deviceSpecificationFactory.getPayLoad(rCaptureResponseBiometricsDTO.getData());

			RCaptureResponseDataDTO dataDTO = (RCaptureResponseDataDTO) (mapper
					.readValue(new String(Base64.getUrlDecoder().decode(payLoad)), RCaptureResponseDataDTO.class));

			BiometricsDto biometricDTO = new BiometricsDto(dataDTO.getBioSubType(), dataDTO.getDecodedBioValue(),
					Double.parseDouble(dataDTO.getQualityScore()));
			biometricDTO.setCaptured(true);
			biometricDTO.setModalityName(mdmRequestDto.getModality());
			biometricDTOs.add(biometricDTO);
		}
		return null;
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
			captureRequestBioDTOs.add(new RCaptureRequestBioDTO(bioDevice.getDeviceType(), "1", null,
					mdmRequestDto.getExceptions(), String.valueOf(mdmRequestDto.getRequestedScore()),
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
}
