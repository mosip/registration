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
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.mdm.dto.CaptureResponseDto;
import io.mosip.registration.mdm.dto.DeviceDiscoveryResponsetDto;
import io.mosip.registration.mdm.integrator.IMosipBioDeviceIntegrator;
import io.mosip.registration.mdm.service.impl.MosipBioDeviceManager;
import io.mosip.registration.mdm.spec_0_9_5.dto.request.RCaptureRequestDTO;
import io.mosip.registration.mdm.spec_0_9_5.dto.request.StreamRequestDTO;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.MdmDeviceInfoResponse;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.RCaptureResponseBiometricsDTO;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.RCaptureResponseDTO;

@Service
public class MDM_095_IntegratorImpl implements IMosipBioDeviceIntegrator {

	private static final Logger LOGGER = AppConfig.getLogger(MosipBioDeviceManager.class);

	@Override
	public Object getDeviceInfo(String url, Class<?> responseType) throws RegBaseCheckedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<DeviceDiscoveryResponsetDto> getDeviceDiscovery(String url, String deviceType, Class<?> responseType)
			throws RegBaseCheckedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CaptureResponseDto capture(String url, Object request, Class<?> responseType)
			throws RegBaseCheckedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CaptureResponseDto getFrame() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CaptureResponseDto forceCapture() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CaptureResponseDto responseParsing() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean jwsValidation(String jwsResponse) throws RegBaseCheckedException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object decodeRCaptureData(String data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object rCapture(String url, Object registrationCaptureRequestDto)
			throws JsonParseException, JsonMappingException, ParseException, IOException {
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID,
				"Entering into Capture method....." + System.currentTimeMillis());

		RCaptureRequestDTO rCaptureRequestDTO = (RCaptureRequestDTO) registrationCaptureRequestDto;
		String requestBody = null;
		ObjectMapper mapper = new ObjectMapper();
		requestBody = mapper.writeValueAsString(rCaptureRequestDTO);
		
		CloseableHttpClient client = HttpClients.createDefault();
		StringEntity requestEntity = new StringEntity(requestBody, ContentType.create("Content-Type", Consts.UTF_8));
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID,
				"Bulding capture url...." + System.currentTimeMillis());
		HttpUriRequest request = RequestBuilder.create("RCAPTURE").setUri(url).setEntity(requestEntity).build();
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
		return captureResponse.getBiometrics();

	}

	@Override
	public InputStream stream(String url, Object registrationStreamRequestDto)
			throws MalformedURLException, IOException {

		StreamRequestDTO streamRequestDTO = (StreamRequestDTO) registrationStreamRequestDto;

		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.setRequestMethod("POST");
		String request = new ObjectMapper().writeValueAsString(streamRequestDTO);

		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());

		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID,
				"Stream Request Started : " + System.currentTimeMillis());
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID, "Stream Request : " + streamRequestDTO.toString());

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

}
