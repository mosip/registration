package io.mosip.registration.mdm.dto;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Consts;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dto.json.metadata.DigitalId;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.mdm.constants.MosipBioDeviceConstants;
import io.mosip.registration.mdm.integrator.IMosipBioDeviceIntegrator;
import io.mosip.registration.mdm.util.MdmRequestResponseBuilder;
import lombok.Getter;
import lombok.Setter;

/**
 * Holds the Biometric Device details
 * 
 * @author balamurugan.ramamoorthy
 *
 */
@Getter
@Setter
public class BioDevice {

	private String deviceType;
	private String deviceSubType;
	private String deviceModality;
	private int runningPort;
	private String runningUrl;
	private String status;
	private String providerName;
	private String providerId;
	private String serialVersion;
	private String certification;
	private String callbackId;
	private String deviceModel;
	private String deviceMake;
	private String firmWare;
	private String deviceExpiry;
	private String deviceId;
	private String deviceCode;
	private String serialNumber;
	private String deviceSubId;
	private String deviceProviderName;
	private String deviceProviderId;
	private String timestamp;
	private String purpose;
	private String[] specVersion;
	private DigitalId digitalId;
	private boolean isRegistered;
	private boolean isSpecVersionValid;

	private Map<String, String> deviceSubIdMapper = new HashMap<String, String>() {
		{

			put("LEFT", "1");
			put("RIGHT", "2");
			put("THUMBS", "3");
			put("FACE", "0");
			put("DOUBLE", "3");
			put("SINGLE", "0");
		}
	};

	private static final Logger LOGGER = AppConfig.getLogger(BioDevice.class);
	private IMosipBioDeviceIntegrator mosipBioDeviceIntegrator;

	public void checkForSpec() {
		if (specVersion[0].equals((String) ApplicationContext.getInstance().map().get("current_mdm_spec")))
			isSpecVersionValid = true;
		else
			isSpecVersionValid = false;
	}

	public CaptureResponseDto regCapture(RequestDetail requestDetail) throws RegBaseCheckedException, IOException {
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID,
				"Entering into Capture method....." + System.currentTimeMillis());

		String url = runningUrl + ":" + runningPort + "/" + MosipBioDeviceConstants.CAPTURE_ENDPOINT;

		CaptureResponseDto captureResponse = null;

		/* build the request object for capture */
		CaptureRequestDto mosipBioCaptureRequestDto = MdmRequestResponseBuilder.buildMosipBioCaptureRequestDto(this,
				requestDetail);
		String requestBody = null;
		ObjectMapper mapper = new ObjectMapper();
		requestBody = mapper.writeValueAsString(mosipBioCaptureRequestDto);
		CloseableHttpClient client = HttpClients.createDefault();
		StringEntity requestEntity = new StringEntity(requestBody, ContentType.create("Content-Type", Consts.UTF_8));
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID,
				"Bulding capture url...." + System.currentTimeMillis());
		HttpUriRequest request = RequestBuilder
				.create(requestDetail.getMosipProcess().equals("Registration") ? "RCAPTURE" : "CAPTURE").setUri(url)
				.setEntity(requestEntity).build();
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID,
				"Requesting capture url...." + System.currentTimeMillis());
		CloseableHttpResponse response = client.execute(request);
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID,
				"Request completed.... " + System.currentTimeMillis());
		captureResponse = mapper.readValue(EntityUtils.toString(response.getEntity()).getBytes(StandardCharsets.UTF_8),
				CaptureResponseDto.class);
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID,
				"Response Recived.... " + System.currentTimeMillis());
		decode(captureResponse);
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID,
				"Response Decode and leaving the method.... " + System.currentTimeMillis());
		return captureResponse;

	}

	private void decode(CaptureResponseDto mosipBioCaptureResponseDto)
			throws IOException, JsonParseException, JsonMappingException, RegBaseCheckedException {
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID,
				"Entering into Decode Method.... " + System.currentTimeMillis());
		ObjectMapper mapper = new ObjectMapper();
		if (null != mosipBioCaptureResponseDto && null != mosipBioCaptureResponseDto.getMosipBioDeviceDataResponses()) {
			for (CaptureResponseBioDto captureResponseBioDto : mosipBioCaptureResponseDto
					.getMosipBioDeviceDataResponses()) {
				//
				if (null != captureResponseBioDto) {
					String bioJson = captureResponseBioDto.getCaptureBioData();
					if (null != bioJson) {
						CaptureResponsBioDataDto captureResponsBioDataDto = getCaptureResponsBioDataDecoded(bioJson,
								mapper);
						captureResponsBioDataDto.setDigitalIdDecoded(mapper.readValue(
								new String(Base64.getUrlDecoder().decode(captureResponsBioDataDto.getDigitalId()))
										.getBytes(),
								DigitalId.class));
						captureResponseBioDto.setCaptureResponseData(captureResponsBioDataDto);
					}
				}
			}
		}
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID,
				"Leaving into Decode Method.... " + System.currentTimeMillis());
	}

	private CaptureResponsBioDataDto getCaptureResponsBioDataDecoded(String capturedData, ObjectMapper mapper) throws RegBaseCheckedException
			 {

		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID,
				"Entering into Stream Method.... " + System.currentTimeMillis());

		try {

			if (mosipBioDeviceIntegrator.jwsValidation(capturedData)) {
				mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
				Pattern pattern = Pattern.compile(RegistrationConstants.BIOMETRIC_SEPERATOR);
				Matcher matcher = pattern.matcher(capturedData);
				String afterMatch = null;
				if (matcher.find()) {
					afterMatch = matcher.group(1);
				}

				String result = new String(Base64.getUrlDecoder().decode(afterMatch));
				try {
					return (CaptureResponsBioDataDto) (mapper.readValue(result.getBytes(),
							CaptureResponsBioDataDto.class));
				} catch (Exception exception) {
					throw new RegBaseCheckedException();
				}
			} else {
				throw new RegBaseCheckedException();
			}
		} catch (RegBaseCheckedException excption) {
			
			LOGGER.error("BioDevice", APPLICATION_NAME, APPLICATION_ID,
					"JWT verification is not working" + System.currentTimeMillis()+ " \n"+excption.getMessage() + ExceptionUtils.getStackTrace(excption));
			throw new RegBaseCheckedException("403", "");
		}
	}

	public InputStream stream() throws IOException {

		String url = runningUrl + ":" + runningPort + "/" + MosipBioDeviceConstants.STREAM_ENDPOINT;

		/* build the request object for capture */
		StreamingRequestDetail streamRequest = new StreamingRequestDetail();
		streamRequest.setDeviceId(this.getDeviceId());
		streamRequest.setDeviceSubId(this.getDeviceSubId());

		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.setRequestMethod("POST");
		String request = new ObjectMapper().writeValueAsString(streamRequest);

		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());

		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID,
				"Stream Request Started : " + System.currentTimeMillis());
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID, "Stream Request : " + streamRequest.toString());

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

	public byte[] forceCapture() {
		return null;

	}

	public void buildDeviceSubId(String slapType) {
		setDeviceSubId(deviceSubIdMapper.get(slapType));
	}

	public int deviceStatus() {
		return 0;

	}

}
