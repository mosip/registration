package io.mosip.registration.processor.stages.umcvalidator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import io.mosip.registration.processor.core.packet.dto.NewDigitalId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;

import io.mosip.kernel.core.exception.IOException;
import io.mosip.kernel.core.util.exception.JsonMappingException;
import io.mosip.kernel.core.util.exception.JsonParseException;
import io.mosip.kernel.packetmanager.exception.ApiNotAccessibleException;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.core.http.RequestWrapper;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.packet.dto.DigitalId;
import io.mosip.registration.processor.core.packet.dto.FieldValue;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.RegOsiDto;
import io.mosip.registration.processor.core.packet.dto.NewRegisteredDevice;
import io.mosip.registration.processor.core.packet.dto.RegistrationCenterMachineDto;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.DeviceValidateHistoryRequest;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.DeviceValidateHistoryResponse;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.MachineHistoryDto;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.MachineHistoryResponseDto;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.RegistartionCenterTimestampResponseDto;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.RegistrationCenterDeviceHistoryDto;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.RegistrationCenterDeviceHistoryResponseDto;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.RegistrationCenterDto;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.RegistrationCenterResponseDto;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.RegistrationCenterUserMachineMappingHistoryDto;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.RegistrationCenterUserMachineMappingHistoryResponseDto;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.stages.osivalidator.UMCValidator;
import io.mosip.registration.processor.stages.osivalidator.utils.OSIUtils;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;

// TODO: Auto-generated Javadoc
/**
 * The Class UMCValidatorTest.
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*","javax.management.*", "javax.net.ssl.*" })
public class UMCValidatorTest {

	/** The umc validator. */
	@InjectMocks
	UMCValidator umcValidator;

	/** The packet info manager. */
	@Mock
	PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;



	/** The registration processor rest service. */
	@Mock
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;

	/** The osi utils. */
	@Mock
	private OSIUtils osiUtils;

	Identity identity;

	/** The rcm dto. */
	RegistrationCenterMachineDto rcmDto = new RegistrationCenterMachineDto();

	/** The reg osi. */
	RegOsiDto regOsi;

	List<FieldValue> metaData;

	InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();

	@Mock
	private Environment env;

	/**
	 * Sets the up.
	 * 
	 * @throws java.io.IOException
	 * @throws IOException
	 * @throws ApisResourceAccessException
	 * @throws PacketDecryptionFailureException
	 */
	@Before
	public void setUp()
			throws PacketDecryptionFailureException, ApisResourceAccessException, IOException, java.io.IOException {
		// umcValidator.setRegistrationStatusDto(registrationStatusDto);
		rcmDto = new RegistrationCenterMachineDto();
		regOsi = new RegOsiDto();
		rcmDto.setIsActive(true);
		rcmDto.setLatitude("13.0049");
		rcmDto.setLongitude("80.24492");
		rcmDto.setMachineId("yyeqy26356");
		rcmDto.setPacketCreationDate("2018-11-28T15:34:20.122");
		rcmDto.setRegcntrId("12245");
		rcmDto.setRegId("2018782130000121112018103016");

		regOsi.setOfficerId("O1234");

		regOsi.setSupervisorId("S1234");

		ReflectionTestUtils.setField(umcValidator, "isWorkingHourValidationRequired", true);
		ReflectionTestUtils.setField(umcValidator, "deviceValidateHistoryId", "");
		ReflectionTestUtils.setField(umcValidator, "gpsEnable", "y");
		when(env.getProperty("mosip.registration.processor.datetime.pattern"))
				.thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

		ClassLoader classLoader = getClass().getClassLoader();
		File idJsonFile = new File(classLoader.getResource("packet_meta_info.json").getFile());
		InputStream packetMetaInfoStream = new FileInputStream(idJsonFile);


		metaData = new ArrayList<>();
		FieldValue fv = new FieldValue();
		fv.setLabel("REGISTRATIONID");
		fv.setValue("");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("CENTERID");
		fv.setValue("");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("MACHINEID");
		fv.setValue("");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("GEOLOCLATITUDE");
		fv.setValue("");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("GEOLOCLONGITUDE");
		fv.setValue("");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("CREATIONDATE");
		fv.setValue("");
		metaData.add(fv);

	}

	/**
	 * Checks if is valid UMC success test.
	 *
	 * @throws ApisResourceAccessException      the apis resource access exception
	 * @throws JsonParseException               the json parse exception
	 * @throws JsonMappingException             the json mapping exception
	 * @throws IOException                      Signals that an I/O exception has
	 *                                          occurred.
	 * @throws IOException                      Signals that an I/O exception has
	 *                                          occurred.
	 * @throws PacketDecryptionFailureException
	 * @throws                                  io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
	 */
	@Test
	public void isValidUMCSuccessTest() throws ApisResourceAccessException, JsonParseException, JsonMappingException,
			IOException, java.io.IOException, PacketDecryptionFailureException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		identity = new Identity();
		RegistrationCenterDto rcdto = new RegistrationCenterDto();
		rcdto.setIsActive(true);
		rcdto.setLongitude("80.24492");
		rcdto.setLatitude("13.0049");
		rcdto.setId("12245");

		List<NewRegisteredDevice> capturedRegisteredDevices = new ArrayList<NewRegisteredDevice>();
		NewRegisteredDevice deviceDetails = new NewRegisteredDevice();
		deviceDetails.setDeviceCode("3000111");

		capturedRegisteredDevices.add(deviceDetails);
		// fv1 = new FieldValue();
		// fv1.setLabel("Document Scanner");
		// fv1.setValue("3000091");
		// capturedRegisteredDevices.add(fv1);
		// fv1 = new FieldValue();
		// fv1.setLabel("Camera");
		// fv1.setValue("3000071");
		// capturedRegisteredDevices.add(fv1);
		// fv1 = new FieldValue();
		// fv1.setLabel("Finger Print Scanner");
		// fv1.setValue("3000092");
		// capturedRegisteredDevices.add(fv1);
		identity.setCapturedRegisteredDevices(capturedRegisteredDevices);

		metaData = new ArrayList<>();
		FieldValue fv = new FieldValue();
		fv.setLabel("REGISTRATIONID");
		fv.setValue("2018782130000121112018103016");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("CENTERID");
		fv.setValue("12245");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("MACHINEID");
		fv.setValue("yyeqy26356");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("GEOLOCLATITUDE");
		fv.setValue("13.0049");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("GEOLOCLONGITUDE");
		fv.setValue("80.24492");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("CREATIONDATE");
		fv.setValue("2018-11-28T15:34:20.122");
		metaData.add(fv);

		identity.setMetaData(metaData);
		Mockito.when(osiUtils.getIdentity(any())).thenReturn(identity);
		List<RegistrationCenterDto> rcdtos = new ArrayList<>();
		rcdtos.add(rcdto);

		RegistrationCenterResponseDto regrepdto = new RegistrationCenterResponseDto();
		regrepdto.setRegistrationCentersHistory(rcdtos);

		ResponseWrapper<RegistrationCenterResponseDto> regrepdtoWrapper = new ResponseWrapper<>();

		regrepdtoWrapper.setResponse(regrepdto);
		regrepdtoWrapper.setErrors(null);

		MachineHistoryDto mcdto = new MachineHistoryDto();
		mcdto.setIsActive(true);
		mcdto.setId("yyeqy26356");

		ResponseWrapper<MachineHistoryDto> mcdtoWrapper = new ResponseWrapper<>();

		mcdtoWrapper.setResponse(mcdto);
		mcdtoWrapper.setErrors(null);

		List<MachineHistoryDto> mcdtos = new ArrayList<>();
		mcdtos.add(mcdto);
		MachineHistoryResponseDto mhrepdto = new MachineHistoryResponseDto();
		mhrepdto.setMachineHistoryDetails(mcdtos);

		ResponseWrapper<MachineHistoryResponseDto> mhrepdtoWrapper = new ResponseWrapper<>();

		mhrepdtoWrapper.setResponse(mhrepdto);
		mhrepdtoWrapper.setErrors(null);
		RegistrationCenterUserMachineMappingHistoryDto officerucmdto = new RegistrationCenterUserMachineMappingHistoryDto();
		officerucmdto.setIsActive(true);
		officerucmdto.setCntrId("12245");
		officerucmdto.setMachineId("yyeqy26356");
		officerucmdto.setUsrId("O1234");

		List<RegistrationCenterUserMachineMappingHistoryDto> officerucmdtos = new ArrayList<>();
		officerucmdtos.add(officerucmdto);

		RegistrationCenterUserMachineMappingHistoryResponseDto offrepdto = new RegistrationCenterUserMachineMappingHistoryResponseDto();

		offrepdto.setRegistrationCenters(officerucmdtos);

		ResponseWrapper<RegistrationCenterUserMachineMappingHistoryResponseDto> offrepdtoWrapper = new ResponseWrapper<>();

		offrepdtoWrapper.setResponse(offrepdto);
		offrepdtoWrapper.setErrors(null);
		RegistartionCenterTimestampResponseDto test = new RegistartionCenterTimestampResponseDto();
		test.setStatus("Valid");
		ResponseWrapper<RegistartionCenterTimestampResponseDto> testWrapper = new ResponseWrapper<>();

		testWrapper.setResponse(test);
		testWrapper.setErrors(null);

		DeviceValidateHistoryRequest deviceValidateHistoryRequest = new DeviceValidateHistoryRequest();
		deviceValidateHistoryRequest.setDeviceCode(deviceDetails.getDeviceCode());

		RequestWrapper<DeviceValidateHistoryRequest> request = new RequestWrapper<>();

		request.setRequest(deviceValidateHistoryRequest);

		DeviceValidateHistoryResponse deviceValidateResponse = new DeviceValidateHistoryResponse();
		deviceValidateResponse.setStatus("VALID");

		ResponseWrapper<DeviceValidateHistoryResponse> deviceValidateResponseWrapper = new ResponseWrapper<>();

		deviceValidateResponseWrapper.setResponse(deviceValidateResponse);
		deviceValidateResponseWrapper.setErrors(null);
		RegistrationCenterDeviceHistoryResponseDto registrationCenterDeviceHistoryResponseDto = new RegistrationCenterDeviceHistoryResponseDto();
		RegistrationCenterDeviceHistoryDto registrationCenterDeviceHistoryDetails = new RegistrationCenterDeviceHistoryDto();

		registrationCenterDeviceHistoryDetails.setIsActive(true);
		registrationCenterDeviceHistoryResponseDto
				.setRegistrationCenterDeviceHistoryDetails(registrationCenterDeviceHistoryDetails);

		ResponseWrapper<RegistrationCenterDeviceHistoryResponseDto> centerDeviceHistoryResponseDtoWrapper = new ResponseWrapper<>();
		// centerDeviceHistoryResponseDtoWrapper.setResponse(statusResponseDto);
		centerDeviceHistoryResponseDtoWrapper.setResponse(registrationCenterDeviceHistoryResponseDto);
		centerDeviceHistoryResponseDtoWrapper.setErrors(null);
		List<String> pathsegments = new ArrayList<>();
		pathsegments.add("12245");
		pathsegments.add(null);
		pathsegments.add("2018-11-28T15:34:20.122");
		List<String> pathsegments1 = new ArrayList<>();
		pathsegments1.add("yyeqy26356");
		pathsegments1.add(null);
		pathsegments1.add("2018-11-28T15:34:20.122");
		List<String> pathsegments2 = new ArrayList<>();
		pathsegments2.add("2018-11-28T15:34:20.122");
		pathsegments2.add("12245");
		pathsegments2.add("yyeqy26356");
		pathsegments2.add("S1234");
		List<String> pathsegments3 = new ArrayList<>();
		pathsegments3.add("12245");
		pathsegments3.add(null);
		pathsegments3.add("2018-11-28T15:34:20.122");
		List<String> pathsegments4 = new ArrayList<>();
		pathsegments4.add("3000111");
		pathsegments4.add(null);
		pathsegments4.add("2018-11-28T15:34:20.122");
		List<String> pathsegments5 = new ArrayList<>();
		pathsegments5.add("12245");
		pathsegments5.add("3000111");
		pathsegments5.add("2018-11-28T15:34:20.122");
		List<String> pathsegments6 = new ArrayList<>();
		pathsegments6.add("2018-11-28T15:34:20.122");
		pathsegments6.add("12245");
		pathsegments6.add("yyeqy26356");
		pathsegments6.add("O1234");

		Mockito.when(osiUtils.getOSIDetailsFromMetaInfo(any(), any())).thenReturn(regOsi);
		Mockito.when(registrationProcessorRestService.getApi(ApiName.CENTERHISTORY, pathsegments, "", "",
				ResponseWrapper.class)).thenReturn(regrepdtoWrapper);

		Mockito.when(registrationProcessorRestService.getApi(ApiName.MACHINEHISTORY, pathsegments1, "", "",
				ResponseWrapper.class)).thenReturn(mhrepdtoWrapper);
		Mockito.when(registrationProcessorRestService.getApi(ApiName.CENTERUSERMACHINEHISTORY, pathsegments2, "", "",
				ResponseWrapper.class)).thenReturn(offrepdtoWrapper);
		Mockito.when(registrationProcessorRestService.getApi(ApiName.CENTERUSERMACHINEHISTORY, pathsegments6, "", "",
				ResponseWrapper.class)).thenReturn(offrepdtoWrapper);
		Mockito.when(registrationProcessorRestService.getApi(ApiName.REGISTRATIONCENTERTIMESTAMP, pathsegments3, "", "",
				ResponseWrapper.class)).thenReturn(testWrapper);
		Mockito.when(registrationProcessorRestService.postApi(any(), any(), any(), any(), any()))
				.thenReturn(deviceValidateResponseWrapper);
		Mockito.when(registrationProcessorRestService.getApi(ApiName.REGISTRATIONCENTERDEVICEHISTORY, pathsegments5, "",
				"", ResponseWrapper.class)).thenReturn(centerDeviceHistoryResponseDtoWrapper);

		// Mockito.when(registrationProcessorRestService.getApi(any(), any(), any(),
		// any(), any())).thenReturn(offrepdto).thenReturn(test)
		// .thenReturn(deviceHistoryResponsedto).thenReturn(registrationCenterDeviceHistoryResponseDto);
		// UMC validation successfull;
		assertTrue(umcValidator.isValidUMC("2018782130000121112018103016", registrationStatusDto));
	}

	/**
	 * UMC mapping not active test.
	 *
	 * @throws ApisResourceAccessException      the apis resource access exception
	 * @throws JsonParseException               the json parse exception
	 * @throws JsonMappingException             the json mapping exception
	 * @throws IOException                      Signals that an I/O exception has
	 *                                          occurred.
	 * @throws IOException                      Signals that an I/O exception has
	 *                                          occurred.
	 * @throws PacketDecryptionFailureException
	 * @throws                                  io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
	 */
	@Test
	public void UMCMappingNotActiveTest() throws ApisResourceAccessException, JsonParseException, JsonMappingException,
			IOException, java.io.IOException, PacketDecryptionFailureException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		RegistrationCenterDto rcdto = new RegistrationCenterDto();
		rcdto.setIsActive(true);
		rcdto.setLongitude("80.24492");
		rcdto.setLatitude("13.0049");
		rcdto.setId("12245");

		List<RegistrationCenterDto> rcdtos = new ArrayList<>();
		rcdtos.add(rcdto);
		RegistrationCenterResponseDto regrepdto = new RegistrationCenterResponseDto();
		regrepdto.setRegistrationCentersHistory(rcdtos);
		ResponseWrapper<RegistrationCenterResponseDto> regrepdtoWrapper = new ResponseWrapper<>();
		regrepdtoWrapper.setResponse(regrepdto);
		regrepdtoWrapper.setErrors(null);

		MachineHistoryDto mcdto = new MachineHistoryDto();
		mcdto.setIsActive(true);
		mcdto.setId("yyeqy26356");
		ResponseWrapper<MachineHistoryDto> mcdtodtoWrapper = new ResponseWrapper<>();
		mcdtodtoWrapper.setResponse(mcdto);
		mcdtodtoWrapper.setErrors(null);

		List<MachineHistoryDto> mcdtos = new ArrayList<>();
		mcdtos.add(mcdto);
		MachineHistoryResponseDto mhrepdto = new MachineHistoryResponseDto();
		mhrepdto.setMachineHistoryDetails(mcdtos);
		ResponseWrapper<MachineHistoryResponseDto> mcdtosWrapper = new ResponseWrapper<>();
		mcdtosWrapper.setResponse(mhrepdto);
		mcdtosWrapper.setErrors(null);

		RegistrationCenterUserMachineMappingHistoryDto officerucmdto = new RegistrationCenterUserMachineMappingHistoryDto();
		officerucmdto.setIsActive(false);
		officerucmdto.setCntrId("12245");
		officerucmdto.setMachineId("yyeqy26356");
		officerucmdto.setUsrId("O1234");

		List<RegistrationCenterUserMachineMappingHistoryDto> officerucmdtos = new ArrayList<>();
		officerucmdtos.add(officerucmdto);

		RegistrationCenterUserMachineMappingHistoryResponseDto offrepdto = new RegistrationCenterUserMachineMappingHistoryResponseDto();
		offrepdto.setRegistrationCenters(officerucmdtos);
		ResponseWrapper<RegistrationCenterUserMachineMappingHistoryResponseDto> offrepdtoWrapper = new ResponseWrapper<>();
		offrepdtoWrapper.setResponse(offrepdto);
		offrepdtoWrapper.setErrors(null);
		identity = new Identity();
		identity.setMetaData(metaData);
		Mockito.when(osiUtils.getIdentity(any())).thenReturn(identity);

		Mockito.when(registrationProcessorRestService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(regrepdtoWrapper).thenReturn(mcdtosWrapper).thenReturn(offrepdtoWrapper);
		// UMC validation Failure;
		assertFalse(umcValidator.isValidUMC("2018782130000121112018103016", registrationStatusDto));
	}

	/**
	 * Machine id not found test.
	 *
	 * @throws ApisResourceAccessException      the apis resource access exception
	 * @throws JsonParseException               the json parse exception
	 * @throws JsonMappingException             the json mapping exception
	 * @throws IOException                      Signals that an I/O exception has
	 *                                          occurred.
	 * @throws IOException                      Signals that an I/O exception has
	 *                                          occurred.
	 * @throws PacketDecryptionFailureException
	 * @throws                                  io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
	 */
	@Test
	public void machineIdNotFoundTest() throws ApisResourceAccessException, JsonParseException, JsonMappingException,
			IOException, java.io.IOException, PacketDecryptionFailureException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		RegistrationCenterDto rcdto = new RegistrationCenterDto();
		rcdto.setIsActive(true);
		rcdto.setLongitude("80.24492");
		rcdto.setLatitude("13.0049");
		rcdto.setId("12245");

		List<RegistrationCenterDto> rcdtos = new ArrayList<>();
		rcdtos.add(rcdto);
		RegistrationCenterResponseDto regrepdto = new RegistrationCenterResponseDto();
		regrepdto.setRegistrationCentersHistory(rcdtos);

		ResponseWrapper<RegistrationCenterResponseDto> regrepdtoWrapper = new ResponseWrapper<>();
		regrepdtoWrapper.setResponse(regrepdto);
		regrepdtoWrapper.setErrors(null);

		MachineHistoryDto mcdto = new MachineHistoryDto();
		mcdto.setIsActive(true);

		List<MachineHistoryDto> mcdtos = new ArrayList<>();
		mcdtos.add(mcdto);
		MachineHistoryResponseDto mhrepdto = new MachineHistoryResponseDto();
		mhrepdto.setMachineHistoryDetails(mcdtos);
		ResponseWrapper<MachineHistoryResponseDto> mhrepdtoWrapper = new ResponseWrapper<>();
		mhrepdtoWrapper.setResponse(mhrepdto);
		mhrepdtoWrapper.setErrors(null);

		RegistrationCenterUserMachineMappingHistoryDto officerucmdto = new RegistrationCenterUserMachineMappingHistoryDto();
		officerucmdto.setIsActive(true);
		officerucmdto.setCntrId("12245");
		officerucmdto.setMachineId("yyeqy26356");
		officerucmdto.setUsrId("O1234");

		List<RegistrationCenterUserMachineMappingHistoryDto> officerucmdtos = new ArrayList<>();
		officerucmdtos.add(officerucmdto);

		RegistrationCenterUserMachineMappingHistoryResponseDto offrepdto = new RegistrationCenterUserMachineMappingHistoryResponseDto();
		offrepdto.setRegistrationCenters(officerucmdtos);
		ResponseWrapper<RegistrationCenterUserMachineMappingHistoryResponseDto> offrepdtoWrapper = new ResponseWrapper<>();
		offrepdtoWrapper.setResponse(offrepdto);
		offrepdtoWrapper.setErrors(null);
		identity = new Identity();
		identity.setMetaData(metaData);
		Mockito.when(osiUtils.getIdentity(any())).thenReturn(identity);
		Mockito.when(registrationProcessorRestService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(regrepdtoWrapper).thenReturn(mhrepdtoWrapper).thenReturn(offrepdtoWrapper);
		// UMC validation Failure;
		assertFalse(umcValidator.isValidUMC("2018782130000121112018103016", registrationStatusDto));
	}

	/**
	 * Machine not active test.
	 *
	 * @throws ApisResourceAccessException      the apis resource access exception
	 * @throws JsonParseException               the json parse exception
	 * @throws JsonMappingException             the json mapping exception
	 * @throws IOException                      Signals that an I/O exception has
	 *                                          occurred.
	 * @throws IOException                      Signals that an I/O exception has
	 *                                          occurred.
	 * @throws PacketDecryptionFailureException
	 * @throws                                  io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
	 */
	@Test
	public void machineNotActiveTest() throws ApisResourceAccessException, JsonParseException, JsonMappingException,
			IOException, java.io.IOException, PacketDecryptionFailureException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		RegistrationCenterDto rcdto = new RegistrationCenterDto();
		rcdto.setIsActive(true);
		rcdto.setLongitude("80.24492");
		rcdto.setLatitude("13.0049");
		rcdto.setId("12245");

		List<RegistrationCenterDto> rcdtos = new ArrayList<>();
		rcdtos.add(rcdto);
		RegistrationCenterResponseDto regrepdto = new RegistrationCenterResponseDto();
		regrepdto.setRegistrationCentersHistory(rcdtos);
		ResponseWrapper<RegistrationCenterResponseDto> regrepdtoWrapper = new ResponseWrapper<>();
		regrepdtoWrapper.setResponse(regrepdto);
		regrepdtoWrapper.setErrors(null);

		MachineHistoryDto mcdto = new MachineHistoryDto();
		mcdto.setIsActive(false);
		mcdto.setId("yyeqy26356");

		List<MachineHistoryDto> mcdtos = new ArrayList<>();
		mcdtos.add(mcdto);
		MachineHistoryResponseDto mhrepdto = new MachineHistoryResponseDto();
		mhrepdto.setMachineHistoryDetails(mcdtos);
		ResponseWrapper<MachineHistoryResponseDto> mhrepdtoWrapper = new ResponseWrapper<>();
		mhrepdtoWrapper.setResponse(mhrepdto);
		mhrepdtoWrapper.setErrors(null);

		RegistrationCenterUserMachineMappingHistoryDto officerucmdto = new RegistrationCenterUserMachineMappingHistoryDto();
		officerucmdto.setIsActive(true);
		officerucmdto.setCntrId("12245");
		officerucmdto.setMachineId("yyeqy26356");
		officerucmdto.setUsrId("O1234");

		List<RegistrationCenterUserMachineMappingHistoryDto> officerucmdtos = new ArrayList<>();
		officerucmdtos.add(officerucmdto);

		RegistrationCenterUserMachineMappingHistoryResponseDto offrepdto = new RegistrationCenterUserMachineMappingHistoryResponseDto();
		offrepdto.setRegistrationCenters(officerucmdtos);
		ResponseWrapper<RegistrationCenterUserMachineMappingHistoryResponseDto> offrepdtoWrapper = new ResponseWrapper<>();
		offrepdtoWrapper.setResponse(offrepdto);
		offrepdtoWrapper.setErrors(null);
		identity = new Identity();
		identity.setMetaData(metaData);
		Mockito.when(osiUtils.getIdentity(any())).thenReturn(identity);
		Mockito.when(registrationProcessorRestService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(regrepdtoWrapper).thenReturn(mhrepdtoWrapper).thenReturn(offrepdtoWrapper);
		// UMC validation Failure;
		assertFalse(umcValidator.isValidUMC("2018782130000121112018103016", registrationStatusDto));
	}

	/**
	 * Wronggps data present in master test.
	 *
	 * @throws ApisResourceAccessException      the apis resource access exception
	 * @throws JsonParseException               the json parse exception
	 * @throws JsonMappingException             the json mapping exception
	 * @throws IOException                      Signals that an I/O exception has
	 *                                          occurred.
	 * @throws IOException                      Signals that an I/O exception has
	 *                                          occurred.
	 * @throws PacketDecryptionFailureException
	 * @throws                                  io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
	 */
	@Test
	public void WronggpsDataPresentInMasterTest() throws ApisResourceAccessException, JsonParseException,
			JsonMappingException, IOException, java.io.IOException, PacketDecryptionFailureException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		RegistrationCenterDto rcdto = new RegistrationCenterDto();
		rcdto.setIsActive(true);
		rcdto.setId("12245");
		rcdto.setLongitude("80.21492");
		rcdto.setLatitude("13.10049");
		List<RegistrationCenterDto> rcdtos = new ArrayList<>();
		rcdtos.add(rcdto);
		RegistrationCenterResponseDto regrepdto = new RegistrationCenterResponseDto();
		regrepdto.setRegistrationCentersHistory(rcdtos);
		ResponseWrapper<RegistrationCenterResponseDto> regrepdtoWrapper = new ResponseWrapper<>();
		regrepdtoWrapper.setResponse(regrepdto);
		regrepdtoWrapper.setErrors(null);

		MachineHistoryDto mcdto = new MachineHistoryDto();
		mcdto.setIsActive(true);
		mcdto.setId("yyeqy26356");

		List<MachineHistoryDto> mcdtos = new ArrayList<>();
		mcdtos.add(mcdto);
		MachineHistoryResponseDto mhrepdto = new MachineHistoryResponseDto();
		mhrepdto.setMachineHistoryDetails(mcdtos);
		ResponseWrapper<MachineHistoryResponseDto> mhrepdtoWrapper = new ResponseWrapper<>();
		mhrepdtoWrapper.setResponse(mhrepdto);
		mhrepdtoWrapper.setErrors(null);

		RegistrationCenterUserMachineMappingHistoryDto officerucmdto = new RegistrationCenterUserMachineMappingHistoryDto();
		officerucmdto.setIsActive(true);
		officerucmdto.setCntrId("12245");
		officerucmdto.setMachineId("yyeqy26356");
		officerucmdto.setUsrId("O1234");

		List<RegistrationCenterUserMachineMappingHistoryDto> officerucmdtos = new ArrayList<>();
		officerucmdtos.add(officerucmdto);

		RegistrationCenterUserMachineMappingHistoryResponseDto offrepdto = new RegistrationCenterUserMachineMappingHistoryResponseDto();
		offrepdto.setRegistrationCenters(officerucmdtos);
		ResponseWrapper<RegistrationCenterUserMachineMappingHistoryResponseDto> offrepdtoWrapper = new ResponseWrapper<>();
		offrepdtoWrapper.setResponse(offrepdto);
		offrepdtoWrapper.setErrors(null);
		identity = new Identity();
		identity.setMetaData(metaData);
		Mockito.when(osiUtils.getIdentity(any())).thenReturn(identity);
		Mockito.when(registrationProcessorRestService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(regrepdtoWrapper).thenReturn(mhrepdtoWrapper).thenReturn(offrepdtoWrapper);
		// UMC validation Failure;
		assertFalse(umcValidator.isValidUMC("2018782130000121112018103016", registrationStatusDto));
	}

	/**
	 * Gps datanot present in packet test.
	 *
	 * @throws ApisResourceAccessException      the apis resource access exception
	 * @throws JsonParseException               the json parse exception
	 * @throws JsonMappingException             the json mapping exception
	 * @throws IOException                      Signals that an I/O exception has
	 *                                          occurred.
	 * @throws IOException                      Signals that an I/O exception has
	 *                                          occurred.
	 * @throws PacketDecryptionFailureException
	 * @throws                                  io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
	 */
	@Test
	public void gpsDatanotPresentInPacketTest() throws ApisResourceAccessException, JsonParseException,
			JsonMappingException, IOException, java.io.IOException, PacketDecryptionFailureException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		RegistrationCenterMachineDto rcmDto = new RegistrationCenterMachineDto();
		rcmDto.setIsActive(true);
		rcmDto.setLatitude("13.0049");
		rcmDto.setLongitude("");
		rcmDto.setMachineId(" ");
		rcmDto.setPacketCreationDate("2018-11-28T15:34:20");
		rcmDto.setRegcntrId("12245");
		rcmDto.setRegId("2018782130000121112018103016");

		RegistrationCenterDto rcdto = new RegistrationCenterDto();
		rcdto.setIsActive(true);
		rcdto.setId("12245");
		rcdto.setLongitude("80.24492");
		rcdto.setLatitude("13.10049");
		List<RegistrationCenterDto> rcdtos = new ArrayList<>();
		rcdtos.add(rcdto);
		RegistrationCenterResponseDto regrepdto = new RegistrationCenterResponseDto();
		regrepdto.setRegistrationCentersHistory(rcdtos);
		ResponseWrapper<RegistrationCenterResponseDto> regrepdtoWrapper = new ResponseWrapper<>();
		regrepdtoWrapper.setResponse(regrepdto);
		regrepdtoWrapper.setErrors(null);

		MachineHistoryDto mcdto = new MachineHistoryDto();
		mcdto.setIsActive(true);
		mcdto.setId("yyeqy26356");

		List<MachineHistoryDto> mcdtos = new ArrayList<>();
		mcdtos.add(mcdto);
		MachineHistoryResponseDto mhrepdto = new MachineHistoryResponseDto();
		mhrepdto.setMachineHistoryDetails(mcdtos);
		ResponseWrapper<MachineHistoryResponseDto> mhrepdtoWrapper = new ResponseWrapper<>();
		mhrepdtoWrapper.setResponse(mhrepdto);
		mhrepdtoWrapper.setErrors(null);

		RegistrationCenterUserMachineMappingHistoryDto officerucmdto = new RegistrationCenterUserMachineMappingHistoryDto();
		officerucmdto.setIsActive(true);
		officerucmdto.setCntrId("12245");
		officerucmdto.setMachineId("yyeqy26356");
		officerucmdto.setUsrId("O1234");

		List<RegistrationCenterUserMachineMappingHistoryDto> officerucmdtos = new ArrayList<>();
		officerucmdtos.add(officerucmdto);

		RegistrationCenterUserMachineMappingHistoryResponseDto offrepdto = new RegistrationCenterUserMachineMappingHistoryResponseDto();
		offrepdto.setRegistrationCenters(officerucmdtos);
		ResponseWrapper<RegistrationCenterUserMachineMappingHistoryResponseDto> offrepdtoWrapper = new ResponseWrapper<>();
		offrepdtoWrapper.setResponse(offrepdto);
		offrepdtoWrapper.setErrors(null);
		identity = new Identity();
		identity.setMetaData(metaData);
		Mockito.when(osiUtils.getIdentity(any())).thenReturn(identity);
		Mockito.when(registrationProcessorRestService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(regrepdtoWrapper).thenReturn(mhrepdtoWrapper).thenReturn(offrepdtoWrapper);
		// UMC validation Failure;
		assertFalse(umcValidator.isValidUMC("2018782130000121112018103016", registrationStatusDto));
	}

	/**
	 * Registration centernot active test.
	 *
	 * @throws ApisResourceAccessException      the apis resource access exception
	 * @throws JsonParseException               the json parse exception
	 * @throws JsonMappingException             the json mapping exception
	 * @throws IOException                      Signals that an I/O exception has
	 *                                          occurred.
	 * @throws IOException                      Signals that an I/O exception has
	 *                                          occurred.
	 * @throws PacketDecryptionFailureException
	 * @throws                                  io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
	 */
	@Test
	public void registrationCenternotActiveTest() throws ApisResourceAccessException, JsonParseException,
			JsonMappingException, IOException, java.io.IOException, PacketDecryptionFailureException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {

		RegistrationCenterDto rcdto = new RegistrationCenterDto();
		rcdto.setIsActive(false);
		rcdto.setLongitude("80.24492");
		rcdto.setLatitude("13.0049");
		List<RegistrationCenterDto> rcdtos = new ArrayList<>();
		rcdtos.add(rcdto);

		RegistrationCenterResponseDto regrepdto = new RegistrationCenterResponseDto();
		regrepdto.setRegistrationCentersHistory(rcdtos);
		ResponseWrapper<RegistrationCenterResponseDto> regrepdtoWrapper = new ResponseWrapper<>();
		regrepdtoWrapper.setResponse(regrepdto);
		regrepdtoWrapper.setErrors(null);

		MachineHistoryDto mcdto = new MachineHistoryDto();
		mcdto.setIsActive(true);
		mcdto.setId("yyeqy26356");

		List<MachineHistoryDto> mcdtos = new ArrayList<>();
		mcdtos.add(mcdto);
		MachineHistoryResponseDto mhrepdto = new MachineHistoryResponseDto();
		mhrepdto.setMachineHistoryDetails(mcdtos);
		ResponseWrapper<MachineHistoryResponseDto> mhrepdtoWrapper = new ResponseWrapper<>();
		mhrepdtoWrapper.setResponse(mhrepdto);
		mhrepdtoWrapper.setErrors(null);

		RegistrationCenterUserMachineMappingHistoryDto officerucmdto = new RegistrationCenterUserMachineMappingHistoryDto();
		officerucmdto.setIsActive(true);
		officerucmdto.setCntrId("12245");
		officerucmdto.setMachineId("yyeqy26356");
		officerucmdto.setUsrId("O1234");

		List<RegistrationCenterUserMachineMappingHistoryDto> officerucmdtos = new ArrayList<>();
		officerucmdtos.add(officerucmdto);

		RegistrationCenterUserMachineMappingHistoryResponseDto offrepdto = new RegistrationCenterUserMachineMappingHistoryResponseDto();
		offrepdto.setRegistrationCenters(officerucmdtos);
		ResponseWrapper<RegistrationCenterUserMachineMappingHistoryResponseDto> offrepdtoWrapper = new ResponseWrapper<>();
		offrepdtoWrapper.setResponse(offrepdto);
		offrepdtoWrapper.setErrors(null);
		identity = new Identity();
		identity.setMetaData(metaData);
		Mockito.when(osiUtils.getIdentity(any())).thenReturn(identity);
		Mockito.when(registrationProcessorRestService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(regrepdtoWrapper).thenReturn(mhrepdtoWrapper).thenReturn(offrepdtoWrapper);
		// UMC validation Failure;
		assertFalse(umcValidator.isValidUMC("2018782130000121112018103016", registrationStatusDto));
	}

	/**
	 * Checks if is valid UMC failure for timestamp test.
	 *
	 * @throws ApisResourceAccessException      the apis resource access exception
	 * @throws JsonParseException               the json parse exception
	 * @throws JsonMappingException             the json mapping exception
	 * @throws IOException                      Signals that an I/O exception has
	 *                                          occurred.
	 * @throws IOException                      Signals that an I/O exception has
	 *                                          occurred.
	 * @throws PacketDecryptionFailureException
	 * @throws                                  io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
	 */
	@Test
	public void isValidUMCFailureForTimestampTest() throws ApisResourceAccessException, JsonParseException,
			JsonMappingException, IOException, java.io.IOException, PacketDecryptionFailureException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		RegistrationCenterDto rcdto = new RegistrationCenterDto();
		rcdto.setIsActive(true);
		rcdto.setLongitude("80.24492");
		rcdto.setLatitude("13.0049");
		rcdto.setId("12245");

		List<RegistrationCenterDto> rcdtos = new ArrayList<>();
		rcdtos.add(rcdto);
		RegistrationCenterResponseDto regrepdto = new RegistrationCenterResponseDto();
		regrepdto.setRegistrationCentersHistory(rcdtos);
		ResponseWrapper<RegistrationCenterResponseDto> regrepdtoWrapper = new ResponseWrapper<>();
		regrepdtoWrapper.setResponse(regrepdto);
		regrepdtoWrapper.setErrors(null);

		MachineHistoryDto mcdto = new MachineHistoryDto();
		mcdto.setIsActive(true);
		mcdto.setId("12334");

		List<MachineHistoryDto> mcdtos = new ArrayList<>();
		mcdtos.add(mcdto);
		MachineHistoryResponseDto mhrepdto = new MachineHistoryResponseDto();
		mhrepdto.setMachineHistoryDetails(mcdtos);
		ResponseWrapper<MachineHistoryResponseDto> mhrepdtoWrapper = new ResponseWrapper<>();
		mhrepdtoWrapper.setResponse(mhrepdto);
		mhrepdtoWrapper.setErrors(null);

		RegistrationCenterUserMachineMappingHistoryDto officerucmdto = new RegistrationCenterUserMachineMappingHistoryDto();
		officerucmdto.setIsActive(true);
		officerucmdto.setCntrId("12245");
		officerucmdto.setMachineId("yyeqy26356");
		officerucmdto.setUsrId("O1234");

		List<RegistrationCenterUserMachineMappingHistoryDto> officerucmdtos = new ArrayList<>();
		officerucmdtos.add(officerucmdto);

		RegistrationCenterUserMachineMappingHistoryResponseDto offrepdto = new RegistrationCenterUserMachineMappingHistoryResponseDto();

		offrepdto.setRegistrationCenters(officerucmdtos);
		ResponseWrapper<RegistrationCenterUserMachineMappingHistoryResponseDto> offrepdtoWrapper = new ResponseWrapper<>();
		offrepdtoWrapper.setResponse(offrepdto);
		offrepdtoWrapper.setErrors(null);

		byte[] responseBody = "{\"timestamp\":1548931133376,\"status\":400,\"errors\":[{\"errorCode\":\"KER-MSD-033\",\"errorMessage\":\"Invalid date format Text '2019-01-23T17:15:15.463' could not be parsed at index 23\"}]}"
				.getBytes();

		ApisResourceAccessException apisResourceAccessException = Mockito.mock(ApisResourceAccessException.class);
		HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
				"Invalid request", null, responseBody, null);
		identity = new Identity();
		identity.setMetaData(metaData);
		Mockito.when(osiUtils.getIdentity(any())).thenReturn(identity);
		Mockito.when(apisResourceAccessException.getCause()).thenReturn(httpClientErrorException);

		Mockito.when(registrationProcessorRestService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(regrepdtoWrapper).thenReturn(mhrepdtoWrapper).thenReturn(offrepdtoWrapper)
				.thenReturn(offrepdtoWrapper).thenThrow(apisResourceAccessException);
		// UMC validation Failure;
		assertFalse(umcValidator.isValidUMC("2018782130000121112018103016", registrationStatusDto));
	}

	/**
	 * Checks if is valid UMC failure for registration center ID test.
	 *
	 * @throws ApisResourceAccessException      the apis resource access exception
	 * @throws JsonParseException               the json parse exception
	 * @throws JsonMappingException             the json mapping exception
	 * @throws IOException                      Signals that an I/O exception has
	 *                                          occurred.
	 * @throws IOException                      Signals that an I/O exception has
	 *                                          occurred.
	 * @throws PacketDecryptionFailureException
	 * @throws                                  io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
	 */
	@Test
	public void isValidUMCFailureForRegistrationCenterIDTest() throws ApisResourceAccessException, JsonParseException,
			JsonMappingException, IOException, java.io.IOException, PacketDecryptionFailureException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		RegistrationCenterDto rcdto = new RegistrationCenterDto();
		rcdto.setIsActive(true);
		rcdto.setLongitude("80.24492");
		rcdto.setLatitude("13.0049");
		rcdto.setId("12245");

		List<RegistrationCenterDto> rcdtos = new ArrayList<>();
		rcdtos.add(rcdto);
		RegistrationCenterResponseDto regrepdto = new RegistrationCenterResponseDto();
		regrepdto.setRegistrationCentersHistory(rcdtos);
		ResponseWrapper<RegistrationCenterResponseDto> regrepdtoWrapper = new ResponseWrapper<>();
		regrepdtoWrapper.setResponse(regrepdto);
		regrepdtoWrapper.setErrors(null);

		MachineHistoryDto mcdto = new MachineHistoryDto();
		mcdto.setIsActive(true);
		mcdto.setId("12334");

		List<MachineHistoryDto> mcdtos = new ArrayList<>();
		mcdtos.add(mcdto);
		MachineHistoryResponseDto mhrepdto = new MachineHistoryResponseDto();
		mhrepdto.setMachineHistoryDetails(mcdtos);
		ResponseWrapper<MachineHistoryResponseDto> mhrepdtoWrapper = new ResponseWrapper<>();
		mhrepdtoWrapper.setResponse(mhrepdto);
		mhrepdtoWrapper.setErrors(null);

		RegistrationCenterUserMachineMappingHistoryDto officerucmdto = new RegistrationCenterUserMachineMappingHistoryDto();
		officerucmdto.setIsActive(true);
		officerucmdto.setCntrId("12245");
		officerucmdto.setMachineId("yyeqy26356");
		officerucmdto.setUsrId("O1234");

		List<RegistrationCenterUserMachineMappingHistoryDto> officerucmdtos = new ArrayList<>();
		officerucmdtos.add(officerucmdto);

		RegistrationCenterUserMachineMappingHistoryResponseDto offrepdto = new RegistrationCenterUserMachineMappingHistoryResponseDto();

		offrepdto.setRegistrationCenters(officerucmdtos);
		ResponseWrapper<RegistrationCenterUserMachineMappingHistoryResponseDto> offrepdtoWrapper = new ResponseWrapper<>();
		offrepdtoWrapper.setResponse(offrepdto);
		offrepdtoWrapper.setErrors(null);

		byte[] responseBody = "{\"timestamp\":1548931752579,\"status\":404,\"errors\":[{\"errorCode\":\"KER-MSD-042\",\"errorMessage\":\"Registration Center not found\"}]}"
				.getBytes();

		ApisResourceAccessException apisResourceAccessException = Mockito.mock(ApisResourceAccessException.class);
		HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
				"Invalid request", null, responseBody, null);

		identity = new Identity();
		identity.setMetaData(metaData);
		Mockito.when(osiUtils.getIdentity(any())).thenReturn(identity);
		Mockito.when(apisResourceAccessException.getCause()).thenReturn(httpClientErrorException);

		Mockito.when(registrationProcessorRestService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(regrepdtoWrapper).thenReturn(mhrepdtoWrapper).thenReturn(offrepdtoWrapper)
				.thenReturn(offrepdtoWrapper).thenThrow(apisResourceAccessException);

		assertFalse(umcValidator.isValidUMC("2018782130000121112018103016", registrationStatusDto));
	}

	/**
	 * Checks if is valid UMC center id validation rejected test.
	 *
	 * @throws ApisResourceAccessException      the apis resource access exception
	 * @throws JsonParseException               the json parse exception
	 * @throws JsonMappingException             the json mapping exception
	 * @throws IOException                      Signals that an I/O exception has
	 *                                          occurred.
	 * @throws IOException                      Signals that an I/O exception has
	 *                                          occurred.
	 * @throws PacketDecryptionFailureException
	 * @throws                                  io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
	 */
	@Test
	public void isValidUMCCenterIdValidationRejectedTest() throws ApisResourceAccessException, JsonParseException,
			JsonMappingException, IOException, java.io.IOException, PacketDecryptionFailureException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		identity = new Identity();
		RegistrationCenterDto rcdto = new RegistrationCenterDto();
		rcdto.setIsActive(true);
		rcdto.setLongitude("80.24492");
		rcdto.setLatitude("13.0049");
		rcdto.setId("12245");
		List<NewRegisteredDevice> capturedRegisteredDevices = new ArrayList<NewRegisteredDevice>();
		NewRegisteredDevice deviceDetails = new NewRegisteredDevice();
		deviceDetails.setDeviceCode("3000111");

		capturedRegisteredDevices.add(deviceDetails);

		identity.setCapturedRegisteredDevices(capturedRegisteredDevices);
		List<RegistrationCenterDto> rcdtos = new ArrayList<>();
		rcdtos.add(rcdto);
		RegistrationCenterResponseDto regrepdto = new RegistrationCenterResponseDto();
		regrepdto.setRegistrationCentersHistory(rcdtos);
		ResponseWrapper<RegistrationCenterResponseDto> regrepdtoWrapper = new ResponseWrapper<>();
		regrepdtoWrapper.setResponse(regrepdto);
		regrepdtoWrapper.setErrors(null);

		MachineHistoryDto mcdto = new MachineHistoryDto();
		mcdto.setIsActive(true);
		mcdto.setId("12334");

		List<MachineHistoryDto> mcdtos = new ArrayList<>();
		mcdtos.add(mcdto);
		MachineHistoryResponseDto mhrepdto = new MachineHistoryResponseDto();
		mhrepdto.setMachineHistoryDetails(mcdtos);
		ResponseWrapper<MachineHistoryResponseDto> mhrepdtoWrapper = new ResponseWrapper<>();
		mhrepdtoWrapper.setResponse(mhrepdto);
		mhrepdtoWrapper.setErrors(null);

		RegistrationCenterUserMachineMappingHistoryDto officerucmdto = new RegistrationCenterUserMachineMappingHistoryDto();
		officerucmdto.setIsActive(true);
		officerucmdto.setCntrId("12245");
		officerucmdto.setMachineId("yyeqy26356");
		officerucmdto.setUsrId("O1234");

		List<RegistrationCenterUserMachineMappingHistoryDto> officerucmdtos = new ArrayList<>();
		officerucmdtos.add(officerucmdto);

		RegistrationCenterUserMachineMappingHistoryResponseDto offrepdto = new RegistrationCenterUserMachineMappingHistoryResponseDto();

		offrepdto.setRegistrationCenters(officerucmdtos);
		ResponseWrapper<RegistrationCenterUserMachineMappingHistoryResponseDto> offrepdtoWrapper = new ResponseWrapper<>();
		offrepdtoWrapper.setResponse(offrepdto);
		offrepdtoWrapper.setErrors(null);

		RegistartionCenterTimestampResponseDto test = new RegistartionCenterTimestampResponseDto();
		test.setStatus("Rejected");
		ResponseWrapper<RegistartionCenterTimestampResponseDto> testWrapper = new ResponseWrapper<>();
		testWrapper.setResponse(test);
		testWrapper.setErrors(null);

		DeviceValidateHistoryResponse deviceValidateResponse = new DeviceValidateHistoryResponse();
		deviceValidateResponse.setStatus("VALID");

		ResponseWrapper<DeviceValidateHistoryResponse> deviceValidateResponseWrapper = new ResponseWrapper<>();

		deviceValidateResponseWrapper.setResponse(deviceValidateResponse);
		deviceValidateResponseWrapper.setErrors(null);

		RegistrationCenterDeviceHistoryResponseDto registrationCenterDeviceHistoryResponseDto = new RegistrationCenterDeviceHistoryResponseDto();
		RegistrationCenterDeviceHistoryDto registrationCenterDeviceHistoryDetails = new RegistrationCenterDeviceHistoryDto();

		registrationCenterDeviceHistoryDetails.setIsActive(true);
		registrationCenterDeviceHistoryResponseDto
				.setRegistrationCenterDeviceHistoryDetails(registrationCenterDeviceHistoryDetails);

		identity = new Identity();
		identity.setMetaData(metaData);
		Mockito.when(osiUtils.getIdentity(any())).thenReturn(identity);
		Mockito.when(registrationProcessorRestService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(regrepdtoWrapper).thenReturn(mhrepdtoWrapper).thenReturn(offrepdtoWrapper)
				.thenReturn(offrepdtoWrapper).thenReturn(testWrapper).thenReturn(deviceValidateResponseWrapper)
				.thenReturn(registrationCenterDeviceHistoryResponseDto);

		assertFalse(umcValidator.isValidUMC("2018782130000121112018103016", registrationStatusDto));
	}

	/**
	 * Validate device failure test.
	 *
	 * @throws ApisResourceAccessException      the apis resource access exception
	 * @throws JsonParseException               the json parse exception
	 * @throws JsonMappingException             the json mapping exception
	 * @throws IOException                      Signals that an I/O exception has
	 *                                          occurred.
	 * @throws IOException                      Signals that an I/O exception has
	 *                                          occurred.
	 * @throws PacketDecryptionFailureException
	 * @throws                                  io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
	 */
	@Test
	public void validateDeviceFailureTest() throws ApisResourceAccessException, JsonParseException,
			JsonMappingException, IOException, java.io.IOException, PacketDecryptionFailureException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {

		ApisResourceAccessException apisResourceAccessException = Mockito.mock(ApisResourceAccessException.class);
		byte[] response = "{\"timestamp\":1548930810031,\"status\":404,\"errors\":[{\"errorCode\":\"KER-MSD-129\",\"errorMessage\":\"Device History not found\"}]}"
				.getBytes();
		HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
				"Invalid request", response, StandardCharsets.UTF_8);
		RegistrationCenterDto rcdto = new RegistrationCenterDto();

		Mockito.when(apisResourceAccessException.getCause()).thenReturn(httpClientErrorException);
		rcdto.setIsActive(true);
		rcdto.setLongitude("80.24492");
		rcdto.setLatitude("13.0049");
		rcdto.setId("12245");

		List<RegistrationCenterDto> rcdtos = new ArrayList<>();
		rcdtos.add(rcdto);
		RegistrationCenterResponseDto regrepdto = new RegistrationCenterResponseDto();
		regrepdto.setRegistrationCentersHistory(rcdtos);
		ResponseWrapper<RegistrationCenterResponseDto> regrepdtoWrapper = new ResponseWrapper<>();
		regrepdtoWrapper.setResponse(regrepdto);
		regrepdtoWrapper.setErrors(null);

		MachineHistoryDto mcdto = new MachineHistoryDto();
		mcdto.setIsActive(true);
		mcdto.setId("12334");

		List<MachineHistoryDto> mcdtos = new ArrayList<>();
		mcdtos.add(mcdto);
		MachineHistoryResponseDto mhrepdto = new MachineHistoryResponseDto();
		mhrepdto.setMachineHistoryDetails(mcdtos);

		ResponseWrapper<MachineHistoryResponseDto> mhrepdtoWrapper = new ResponseWrapper<>();
		mhrepdtoWrapper.setResponse(mhrepdto);
		mhrepdtoWrapper.setErrors(null);

		RegistrationCenterUserMachineMappingHistoryDto officerucmdto = new RegistrationCenterUserMachineMappingHistoryDto();
		officerucmdto.setIsActive(true);
		officerucmdto.setCntrId("12245");
		officerucmdto.setMachineId("yyeqy26356");
		officerucmdto.setUsrId("O1234");

		List<RegistrationCenterUserMachineMappingHistoryDto> officerucmdtos = new ArrayList<>();
		officerucmdtos.add(officerucmdto);

		RegistrationCenterUserMachineMappingHistoryResponseDto offrepdto = new RegistrationCenterUserMachineMappingHistoryResponseDto();

		offrepdto.setRegistrationCenters(officerucmdtos);
		ResponseWrapper<RegistrationCenterUserMachineMappingHistoryResponseDto> offrepdtoWrapper = new ResponseWrapper<>();
		offrepdtoWrapper.setResponse(offrepdto);
		offrepdtoWrapper.setErrors(null);

		RegistartionCenterTimestampResponseDto test = new RegistartionCenterTimestampResponseDto();
		test.setStatus("Accepted");
		ResponseWrapper<RegistartionCenterTimestampResponseDto> testWrapper = new ResponseWrapper<>();
		testWrapper.setResponse(test);
		testWrapper.setErrors(null);

		RegistrationCenterDeviceHistoryResponseDto registrationCenterDeviceHistoryResponseDto = new RegistrationCenterDeviceHistoryResponseDto();

		RegistrationCenterDeviceHistoryDto registrationCenterDeviceHistoryDetails = new RegistrationCenterDeviceHistoryDto();
		registrationCenterDeviceHistoryDetails.setIsActive(true);
		registrationCenterDeviceHistoryResponseDto
				.setRegistrationCenterDeviceHistoryDetails(registrationCenterDeviceHistoryDetails);
		ResponseWrapper<RegistrationCenterDeviceHistoryResponseDto> centerDeviceHistoryResponseDto = new ResponseWrapper<>();
		centerDeviceHistoryResponseDto.setResponse(registrationCenterDeviceHistoryResponseDto);
		centerDeviceHistoryResponseDto.setErrors(null);

		identity = new Identity();
		identity.setMetaData(metaData);
		Mockito.when(osiUtils.getIdentity(any())).thenReturn(identity);
		Mockito.when(registrationProcessorRestService.getApi(any(), any(), any(), any(), any()))
				.thenReturn(regrepdtoWrapper).thenReturn(mhrepdtoWrapper).thenReturn(offrepdtoWrapper)
				.thenReturn(offrepdtoWrapper).thenReturn(testWrapper).thenThrow(apisResourceAccessException)
				.thenReturn(centerDeviceHistoryResponseDto);
		assertFalse(umcValidator.isValidUMC("2018782130000121112018103016", registrationStatusDto));
	}

	@Test
	public void isValidCenterHistroyTest() throws ApisResourceAccessException, JsonParseException, JsonMappingException,
			IOException, java.io.IOException, PacketDecryptionFailureException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		identity = new Identity();
		RegistrationCenterDto rcdto = new RegistrationCenterDto();
		rcdto.setIsActive(true);
		rcdto.setLongitude("80.24492");
		rcdto.setLatitude("13.0049");
		rcdto.setId("12245");

		List<NewRegisteredDevice> capturedRegisteredDevices = new ArrayList<NewRegisteredDevice>();
		NewRegisteredDevice fv1 = new NewRegisteredDevice();
		fv1.setDeviceCode("3000111");

		capturedRegisteredDevices.add(fv1);
		// fv1 = new FieldValue();
		// fv1.setLabel("Document Scanner");
		// fv1.setValue("3000091");
		// capturedRegisteredDevices.add(fv1);
		// fv1 = new FieldValue();
		// fv1.setLabel("Camera");
		// fv1.setValue("3000071");
		// capturedRegisteredDevices.add(fv1);
		// fv1 = new FieldValue();
		// fv1.setLabel("Finger Print Scanner");
		// fv1.setValue("3000092");
		// capturedRegisteredDevices.add(fv1);
		identity.setCapturedRegisteredDevices(capturedRegisteredDevices);

		metaData = new ArrayList<>();
		FieldValue fv = new FieldValue();
		fv.setLabel("REGISTRATIONID");
		fv.setValue("2018782130000121112018103016");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("CENTERID");
		fv.setValue("12245");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("MACHINEID");
		fv.setValue("yyeqy26356");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("GEOLOCLATITUDE");
		fv.setValue("13.0049");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("GEOLOCLONGITUDE");
		fv.setValue("80.24492");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("CREATIONDATE");
		fv.setValue("2018-11-28T15:34:20.122");
		metaData.add(fv);

		identity.setMetaData(metaData);
		Mockito.when(osiUtils.getIdentity(any())).thenReturn(identity);
		List<RegistrationCenterDto> rcdtos = new ArrayList<>();
		rcdtos.add(rcdto);

		RegistrationCenterResponseDto regrepdto = new RegistrationCenterResponseDto();
		regrepdto.setRegistrationCentersHistory(rcdtos);

		ResponseWrapper<RegistrationCenterResponseDto> regrepdtoWrapper = new ResponseWrapper<>();
		List<ErrorDTO> errors = new ArrayList<ErrorDTO>();
		ErrorDTO error = new ErrorDTO();
		error.setMessage("In valid center");
		errors.add(error);
		regrepdtoWrapper.setResponse(null);
		regrepdtoWrapper.setErrors(errors);

		List<String> pathsegments = new ArrayList<>();
		pathsegments.add("12245");
		pathsegments.add(null);
		pathsegments.add("2018-11-28T15:34:20.122");
		List<String> pathsegments1 = new ArrayList<>();
		pathsegments1.add("yyeqy26356");
		pathsegments1.add(null);
		pathsegments1.add("2018-11-28T15:34:20.122");
		List<String> pathsegments2 = new ArrayList<>();
		pathsegments2.add("2018-11-28T15:34:20.122");
		pathsegments2.add("12245");
		pathsegments2.add("yyeqy26356");
		pathsegments2.add("S1234");
		List<String> pathsegments3 = new ArrayList<>();
		pathsegments3.add("12245");
		pathsegments3.add(null);
		pathsegments3.add("2018-11-28T15:34:20.122");
		List<String> pathsegments4 = new ArrayList<>();
		pathsegments4.add("3000111");
		pathsegments4.add(null);
		pathsegments4.add("2018-11-28T15:34:20.122");
		List<String> pathsegments5 = new ArrayList<>();
		pathsegments5.add("12245");
		pathsegments5.add("3000111");
		pathsegments5.add("2018-11-28T15:34:20.122");
		List<String> pathsegments6 = new ArrayList<>();
		pathsegments6.add("2018-11-28T15:34:20.122");
		pathsegments6.add("12245");
		pathsegments6.add("yyeqy26356");
		pathsegments6.add("O1234");

		Mockito.when(osiUtils.getOSIDetailsFromMetaInfo(any(), any())).thenReturn(regOsi);
		Mockito.when(registrationProcessorRestService.getApi(ApiName.CENTERHISTORY, pathsegments, "", "",
				ResponseWrapper.class)).thenReturn(regrepdtoWrapper);

		assertFalse(umcValidator.isValidUMC("2018782130000121112018103016", registrationStatusDto));
	}

	@Test
	public void isValidMachineHistroyTest() throws ApisResourceAccessException, JsonParseException,
			JsonMappingException, IOException, java.io.IOException, PacketDecryptionFailureException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		identity = new Identity();
		RegistrationCenterDto rcdto = new RegistrationCenterDto();
		rcdto.setIsActive(true);
		rcdto.setLongitude("80.24492");
		rcdto.setLatitude("13.0049");
		rcdto.setId("12245");

		List<NewRegisteredDevice> capturedRegisteredDevices = new ArrayList<NewRegisteredDevice>();
		NewRegisteredDevice fv1 = new NewRegisteredDevice();
		fv1.setDeviceCode("3000111");

		capturedRegisteredDevices.add(fv1);
		// fv1 = new FieldValue();
		// fv1.setLabel("Document Scanner");
		// fv1.setValue("3000091");
		// capturedRegisteredDevices.add(fv1);
		// fv1 = new FieldValue();
		// fv1.setLabel("Camera");
		// fv1.setValue("3000071");
		// capturedRegisteredDevices.add(fv1);
		// fv1 = new FieldValue();
		// fv1.setLabel("Finger Print Scanner");
		// fv1.setValue("3000092");
		// capturedRegisteredDevices.add(fv1);
		identity.setCapturedRegisteredDevices(capturedRegisteredDevices);

		metaData = new ArrayList<>();
		FieldValue fv = new FieldValue();
		fv.setLabel("REGISTRATIONID");
		fv.setValue("2018782130000121112018103016");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("CENTERID");
		fv.setValue("12245");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("MACHINEID");
		fv.setValue("yyeqy26356");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("GEOLOCLATITUDE");
		fv.setValue("13.0049");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("GEOLOCLONGITUDE");
		fv.setValue("80.24492");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("CREATIONDATE");
		fv.setValue("2018-11-28T15:34:20.122");
		metaData.add(fv);

		identity.setMetaData(metaData);
		Mockito.when(osiUtils.getIdentity(any())).thenReturn(identity);
		List<RegistrationCenterDto> rcdtos = new ArrayList<>();
		rcdtos.add(rcdto);

		RegistrationCenterResponseDto regrepdto = new RegistrationCenterResponseDto();
		regrepdto.setRegistrationCentersHistory(rcdtos);

		ResponseWrapper<RegistrationCenterResponseDto> regrepdtoWrapper = new ResponseWrapper<>();

		regrepdtoWrapper.setResponse(regrepdto);
		regrepdtoWrapper.setErrors(null);

		MachineHistoryDto mcdto = new MachineHistoryDto();
		mcdto.setIsActive(true);
		mcdto.setId("yyeqy26356");

		ResponseWrapper<MachineHistoryDto> mcdtoWrapper = new ResponseWrapper<>();

		mcdtoWrapper.setResponse(mcdto);
		mcdtoWrapper.setErrors(null);

		List<MachineHistoryDto> mcdtos = new ArrayList<>();
		mcdtos.add(mcdto);
		MachineHistoryResponseDto mhrepdto = new MachineHistoryResponseDto();
		mhrepdto.setMachineHistoryDetails(mcdtos);

		ResponseWrapper<MachineHistoryResponseDto> mhrepdtoWrapper = new ResponseWrapper<>();
		List<ErrorDTO> errors = new ArrayList<ErrorDTO>();
		ErrorDTO error = new ErrorDTO();
		error.setMessage("In valid Machine");
		errors.add(error);
		mhrepdtoWrapper.setResponse(null);
		mhrepdtoWrapper.setErrors(errors);

		List<String> pathsegments = new ArrayList<>();
		pathsegments.add("12245");
		pathsegments.add(null);
		pathsegments.add("2018-11-28T15:34:20.122");
		List<String> pathsegments1 = new ArrayList<>();
		pathsegments1.add("yyeqy26356");
		pathsegments1.add(null);
		pathsegments1.add("2018-11-28T15:34:20.122");
		List<String> pathsegments2 = new ArrayList<>();
		pathsegments2.add("2018-11-28T15:34:20.122");
		pathsegments2.add("12245");
		pathsegments2.add("yyeqy26356");
		pathsegments2.add("S1234");
		List<String> pathsegments3 = new ArrayList<>();
		pathsegments3.add("12245");
		pathsegments3.add(null);
		pathsegments3.add("2018-11-28T15:34:20.122");
		List<String> pathsegments4 = new ArrayList<>();
		pathsegments4.add("3000111");
		pathsegments4.add(null);
		pathsegments4.add("2018-11-28T15:34:20.122");
		List<String> pathsegments5 = new ArrayList<>();
		pathsegments5.add("12245");
		pathsegments5.add("3000111");
		pathsegments5.add("2018-11-28T15:34:20.122");
		List<String> pathsegments6 = new ArrayList<>();
		pathsegments6.add("2018-11-28T15:34:20.122");
		pathsegments6.add("12245");
		pathsegments6.add("yyeqy26356");
		pathsegments6.add("O1234");

		Mockito.when(osiUtils.getOSIDetailsFromMetaInfo(any(), any())).thenReturn(regOsi);
		Mockito.when(registrationProcessorRestService.getApi(ApiName.CENTERHISTORY, pathsegments, "", "",
				ResponseWrapper.class)).thenReturn(regrepdtoWrapper);

		Mockito.when(registrationProcessorRestService.getApi(ApiName.MACHINEHISTORY, pathsegments1, "", "",
				ResponseWrapper.class)).thenReturn(mhrepdtoWrapper);

		// Mockito.when(registrationProcessorRestService.getApi(any(), any(), any(),
		// any(), any())).thenReturn(offrepdto).thenReturn(test)
		// .thenReturn(deviceHistoryResponsedto).thenReturn(registrationCenterDeviceHistoryResponseDto);
		// UMC validation successfull;
		assertFalse(umcValidator.isValidUMC("2018782130000121112018103016", registrationStatusDto));
	}

	@Test
	public void isValidDeviceMappedWithCenterTest() throws ApisResourceAccessException, JsonParseException,
			JsonMappingException, IOException, java.io.IOException, PacketDecryptionFailureException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		identity = new Identity();
		RegistrationCenterDto rcdto = new RegistrationCenterDto();
		rcdto.setIsActive(true);
		rcdto.setLongitude("80.24492");
		rcdto.setLatitude("13.0049");
		rcdto.setId("12245");

		List<NewRegisteredDevice> capturedRegisteredDevices = new ArrayList<NewRegisteredDevice>();
		NewRegisteredDevice deviceDetails = new NewRegisteredDevice();
		deviceDetails.setDeviceCode("3000111");
		NewDigitalId digitalid = new NewDigitalId();
		digitalid.setType("FACE");
		deviceDetails.setDigitalId(digitalid);
		capturedRegisteredDevices.add(deviceDetails);

		identity.setCapturedRegisteredDevices(capturedRegisteredDevices);

		metaData = new ArrayList<>();
		FieldValue fv = new FieldValue();
		fv.setLabel("REGISTRATIONID");
		fv.setValue("2018782130000121112018103016");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("CENTERID");
		fv.setValue("12245");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("MACHINEID");
		fv.setValue("yyeqy26356");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("GEOLOCLATITUDE");
		fv.setValue("13.0049");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("GEOLOCLONGITUDE");
		fv.setValue("80.24492");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("CREATIONDATE");
		fv.setValue("2018-11-28T15:34:20.122");
		metaData.add(fv);

		identity.setMetaData(metaData);
		Mockito.when(osiUtils.getIdentity(any())).thenReturn(identity);
		List<RegistrationCenterDto> rcdtos = new ArrayList<>();
		rcdtos.add(rcdto);

		RegistrationCenterResponseDto regrepdto = new RegistrationCenterResponseDto();
		regrepdto.setRegistrationCentersHistory(rcdtos);

		ResponseWrapper<RegistrationCenterResponseDto> regrepdtoWrapper = new ResponseWrapper<>();

		regrepdtoWrapper.setResponse(regrepdto);
		regrepdtoWrapper.setErrors(null);

		MachineHistoryDto mcdto = new MachineHistoryDto();
		mcdto.setIsActive(true);
		mcdto.setId("yyeqy26356");

		ResponseWrapper<MachineHistoryDto> mcdtoWrapper = new ResponseWrapper<>();

		mcdtoWrapper.setResponse(mcdto);
		mcdtoWrapper.setErrors(null);

		List<MachineHistoryDto> mcdtos = new ArrayList<>();
		mcdtos.add(mcdto);
		MachineHistoryResponseDto mhrepdto = new MachineHistoryResponseDto();
		mhrepdto.setMachineHistoryDetails(mcdtos);

		ResponseWrapper<MachineHistoryResponseDto> mhrepdtoWrapper = new ResponseWrapper<>();

		mhrepdtoWrapper.setResponse(mhrepdto);
		mhrepdtoWrapper.setErrors(null);
		RegistrationCenterUserMachineMappingHistoryDto officerucmdto = new RegistrationCenterUserMachineMappingHistoryDto();
		officerucmdto.setIsActive(true);
		officerucmdto.setCntrId("12245");
		officerucmdto.setMachineId("yyeqy26356");
		officerucmdto.setUsrId("O1234");

		List<RegistrationCenterUserMachineMappingHistoryDto> officerucmdtos = new ArrayList<>();
		officerucmdtos.add(officerucmdto);

		RegistrationCenterUserMachineMappingHistoryResponseDto offrepdto = new RegistrationCenterUserMachineMappingHistoryResponseDto();

		offrepdto.setRegistrationCenters(officerucmdtos);

		ResponseWrapper<RegistrationCenterUserMachineMappingHistoryResponseDto> offrepdtoWrapper = new ResponseWrapper<>();

		offrepdtoWrapper.setResponse(offrepdto);
		offrepdtoWrapper.setErrors(null);
		RegistartionCenterTimestampResponseDto test = new RegistartionCenterTimestampResponseDto();
		test.setStatus("Valid");
		ResponseWrapper<RegistartionCenterTimestampResponseDto> testWrapper = new ResponseWrapper<>();

		testWrapper.setResponse(test);
		testWrapper.setErrors(null);
		DeviceValidateHistoryRequest deviceValidateHistoryRequest = new DeviceValidateHistoryRequest();
		deviceValidateHistoryRequest.setDeviceCode(deviceDetails.getDeviceCode());

		RequestWrapper<DeviceValidateHistoryRequest> request = new RequestWrapper<>();

		request.setRequest(deviceValidateHistoryRequest);

		DeviceValidateHistoryResponse deviceValidateResponse = new DeviceValidateHistoryResponse();
		deviceValidateResponse.setStatus("VALID");

		ResponseWrapper<DeviceValidateHistoryResponse> deviceValidateResponseWrapper = new ResponseWrapper<>();

		deviceValidateResponseWrapper.setResponse(deviceValidateResponse);
		deviceValidateResponseWrapper.setErrors(null);
		RegistrationCenterDeviceHistoryResponseDto registrationCenterDeviceHistoryResponseDto = new RegistrationCenterDeviceHistoryResponseDto();
		RegistrationCenterDeviceHistoryDto registrationCenterDeviceHistoryDetails = new RegistrationCenterDeviceHistoryDto();

		registrationCenterDeviceHistoryDetails.setIsActive(true);
		registrationCenterDeviceHistoryResponseDto
				.setRegistrationCenterDeviceHistoryDetails(registrationCenterDeviceHistoryDetails);

		ResponseWrapper<RegistrationCenterDeviceHistoryResponseDto> centerDeviceHistoryResponseDtoWrapper = new ResponseWrapper<>();
		// centerDeviceHistoryResponseDtoWrapper.setResponse(statusResponseDto);
		List<ErrorDTO> errors = new ArrayList<ErrorDTO>();
		ErrorDTO error = new ErrorDTO();
		error.setMessage("Invalid center device mapping");
		errors.add(error);
		centerDeviceHistoryResponseDtoWrapper.setResponse(null);
		centerDeviceHistoryResponseDtoWrapper.setErrors(errors);
		List<String> pathsegments = new ArrayList<>();
		pathsegments.add("12245");
		pathsegments.add(null);
		pathsegments.add("2018-11-28T15:34:20.122");
		List<String> pathsegments1 = new ArrayList<>();
		pathsegments1.add("yyeqy26356");
		pathsegments1.add(null);
		pathsegments1.add("2018-11-28T15:34:20.122");
		List<String> pathsegments2 = new ArrayList<>();
		pathsegments2.add("2018-11-28T15:34:20.122");
		pathsegments2.add("12245");
		pathsegments2.add("yyeqy26356");
		pathsegments2.add("S1234");
		List<String> pathsegments3 = new ArrayList<>();
		pathsegments3.add("12245");
		pathsegments3.add(null);
		pathsegments3.add("2018-11-28T15:34:20.122");
		List<String> pathsegments4 = new ArrayList<>();
		pathsegments4.add("3000111");
		pathsegments4.add(null);
		pathsegments4.add("2018-11-28T15:34:20.122");
		List<String> pathsegments5 = new ArrayList<>();
		pathsegments5.add("12245");
		pathsegments5.add("3000111");
		pathsegments5.add("2018-11-28T15:34:20.122");
		List<String> pathsegments6 = new ArrayList<>();
		pathsegments6.add("2018-11-28T15:34:20.122");
		pathsegments6.add("12245");
		pathsegments6.add("yyeqy26356");
		pathsegments6.add("O1234");

		Mockito.when(osiUtils.getOSIDetailsFromMetaInfo(any(), any())).thenReturn(regOsi);
		Mockito.when(registrationProcessorRestService.getApi(ApiName.CENTERHISTORY, pathsegments, "", "",
				ResponseWrapper.class)).thenReturn(regrepdtoWrapper);

		Mockito.when(registrationProcessorRestService.getApi(ApiName.MACHINEHISTORY, pathsegments1, "", "",
				ResponseWrapper.class)).thenReturn(mhrepdtoWrapper);
		Mockito.when(registrationProcessorRestService.getApi(ApiName.CENTERUSERMACHINEHISTORY, pathsegments2, "", "",
				ResponseWrapper.class)).thenReturn(offrepdtoWrapper);
		Mockito.when(registrationProcessorRestService.getApi(ApiName.CENTERUSERMACHINEHISTORY, pathsegments6, "", "",
				ResponseWrapper.class)).thenReturn(offrepdtoWrapper);
		Mockito.when(registrationProcessorRestService.getApi(ApiName.REGISTRATIONCENTERTIMESTAMP, pathsegments3, "", "",
				ResponseWrapper.class)).thenReturn(testWrapper);
		Mockito.when(registrationProcessorRestService.postApi(any(), any(), any(), any(), any()))
				.thenReturn(deviceValidateResponseWrapper);
		Mockito.when(registrationProcessorRestService.getApi(ApiName.REGISTRATIONCENTERDEVICEHISTORY, pathsegments5, "",
				"", ResponseWrapper.class)).thenReturn(centerDeviceHistoryResponseDtoWrapper);

		// Mockito.when(registrationProcessorRestService.getApi(any(), any(), any(),
		// any(), any())).thenReturn(offrepdto).thenReturn(test)
		// .thenReturn(deviceHistoryResponsedto).thenReturn(registrationCenterDeviceHistoryResponseDto);
		// UMC validation successfull;
		assertFalse(umcValidator.isValidUMC("2018782130000121112018103016", registrationStatusDto));
	}

	@Test
	public void isValidDeviceTest() throws ApisResourceAccessException, JsonParseException, JsonMappingException,
			IOException, java.io.IOException, PacketDecryptionFailureException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		identity = new Identity();
		RegistrationCenterDto rcdto = new RegistrationCenterDto();
		rcdto.setIsActive(true);
		rcdto.setLongitude("80.24492");
		rcdto.setLatitude("13.0049");
		rcdto.setId("12245");

		List<NewRegisteredDevice> capturedRegisteredDevices = new ArrayList<NewRegisteredDevice>();
		NewRegisteredDevice deviceDetails = new NewRegisteredDevice();
		deviceDetails.setDeviceCode("3000111");
        NewDigitalId digitalid=new NewDigitalId();
        digitalid.setType("FACE");
		DigitalId digitalId=new DigitalId();
		digitalId.setType("FACE");
        deviceDetails.setDigitalId(digitalid);
		capturedRegisteredDevices.add(deviceDetails);
		// fv1 = new FieldValue();
		// fv1.setLabel("Document Scanner");
		// fv1.setValue("3000091");
		// capturedRegisteredDevices.add(fv1);
		// fv1 = new FieldValue();
		// fv1.setLabel("Camera");
		// fv1.setValue("3000071");
		// capturedRegisteredDevices.add(fv1);
		// fv1 = new FieldValue();
		// fv1.setLabel("Finger Print Scanner");
		// fv1.setValue("3000092");
		// capturedRegisteredDevices.add(fv1);
		identity.setCapturedRegisteredDevices(capturedRegisteredDevices);

		metaData = new ArrayList<>();
		FieldValue fv = new FieldValue();
		fv.setLabel("REGISTRATIONID");
		fv.setValue("2018782130000121112018103016");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("CENTERID");
		fv.setValue("12245");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("MACHINEID");
		fv.setValue("yyeqy26356");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("GEOLOCLATITUDE");
		fv.setValue("13.0049");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("GEOLOCLONGITUDE");
		fv.setValue("80.24492");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("CREATIONDATE");
		fv.setValue("2018-11-28T15:34:20.122");
		metaData.add(fv);

		identity.setMetaData(metaData);
		Mockito.when(osiUtils.getIdentity(any())).thenReturn(identity);
		List<RegistrationCenterDto> rcdtos = new ArrayList<>();
		rcdtos.add(rcdto);

		RegistrationCenterResponseDto regrepdto = new RegistrationCenterResponseDto();
		regrepdto.setRegistrationCentersHistory(rcdtos);

		ResponseWrapper<RegistrationCenterResponseDto> regrepdtoWrapper = new ResponseWrapper<>();

		regrepdtoWrapper.setResponse(regrepdto);
		regrepdtoWrapper.setErrors(null);

		MachineHistoryDto mcdto = new MachineHistoryDto();
		mcdto.setIsActive(true);
		mcdto.setId("yyeqy26356");

		ResponseWrapper<MachineHistoryDto> mcdtoWrapper = new ResponseWrapper<>();

		mcdtoWrapper.setResponse(mcdto);
		mcdtoWrapper.setErrors(null);

		List<MachineHistoryDto> mcdtos = new ArrayList<>();
		mcdtos.add(mcdto);
		MachineHistoryResponseDto mhrepdto = new MachineHistoryResponseDto();
		mhrepdto.setMachineHistoryDetails(mcdtos);

		ResponseWrapper<MachineHistoryResponseDto> mhrepdtoWrapper = new ResponseWrapper<>();

		mhrepdtoWrapper.setResponse(mhrepdto);
		mhrepdtoWrapper.setErrors(null);
		RegistrationCenterUserMachineMappingHistoryDto officerucmdto = new RegistrationCenterUserMachineMappingHistoryDto();
		officerucmdto.setIsActive(true);
		officerucmdto.setCntrId("12245");
		officerucmdto.setMachineId("yyeqy26356");
		officerucmdto.setUsrId("O1234");

		List<RegistrationCenterUserMachineMappingHistoryDto> officerucmdtos = new ArrayList<>();
		officerucmdtos.add(officerucmdto);

		RegistrationCenterUserMachineMappingHistoryResponseDto offrepdto = new RegistrationCenterUserMachineMappingHistoryResponseDto();

		offrepdto.setRegistrationCenters(officerucmdtos);

		ResponseWrapper<RegistrationCenterUserMachineMappingHistoryResponseDto> offrepdtoWrapper = new ResponseWrapper<>();

		offrepdtoWrapper.setResponse(offrepdto);
		offrepdtoWrapper.setErrors(null);
		RegistartionCenterTimestampResponseDto test = new RegistartionCenterTimestampResponseDto();
		test.setStatus("Valid");
		ResponseWrapper<RegistartionCenterTimestampResponseDto> testWrapper = new ResponseWrapper<>();

		testWrapper.setResponse(test);
		testWrapper.setErrors(null);

		DeviceValidateHistoryRequest deviceValidateHistoryRequest = new DeviceValidateHistoryRequest();
		deviceValidateHistoryRequest.setDeviceCode(deviceDetails.getDeviceCode());
		deviceValidateHistoryRequest.setDigitalId(digitalId);

		RequestWrapper<DeviceValidateHistoryRequest> request = new RequestWrapper<>();

		request.setRequest(deviceValidateHistoryRequest);

		ResponseWrapper<DeviceValidateHistoryResponse> deviceValidateResponseWrapper = new ResponseWrapper<>();

		List<ErrorDTO> errors = new ArrayList<ErrorDTO>();
		ErrorDTO error = new ErrorDTO();
		error.setMessage("Invalid device");
		errors.add(error);
		deviceValidateResponseWrapper.setResponse(null);
		deviceValidateResponseWrapper.setErrors(errors);
		RegistrationCenterDeviceHistoryResponseDto registrationCenterDeviceHistoryResponseDto = new RegistrationCenterDeviceHistoryResponseDto();
		RegistrationCenterDeviceHistoryDto registrationCenterDeviceHistoryDetails = new RegistrationCenterDeviceHistoryDto();

		registrationCenterDeviceHistoryDetails.setIsActive(true);
		registrationCenterDeviceHistoryResponseDto
				.setRegistrationCenterDeviceHistoryDetails(registrationCenterDeviceHistoryDetails);

		ResponseWrapper<RegistrationCenterDeviceHistoryResponseDto> centerDeviceHistoryResponseDtoWrapper = new ResponseWrapper<>();
		// centerDeviceHistoryResponseDtoWrapper.setResponse(statusResponseDto);
		centerDeviceHistoryResponseDtoWrapper.setResponse(registrationCenterDeviceHistoryResponseDto);
		centerDeviceHistoryResponseDtoWrapper.setErrors(null);
		List<String> pathsegments = new ArrayList<>();
		pathsegments.add("12245");
		pathsegments.add(null);
		pathsegments.add("2018-11-28T15:34:20.122");
		List<String> pathsegments1 = new ArrayList<>();
		pathsegments1.add("yyeqy26356");
		pathsegments1.add(null);
		pathsegments1.add("2018-11-28T15:34:20.122");
		List<String> pathsegments2 = new ArrayList<>();
		pathsegments2.add("2018-11-28T15:34:20.122");
		pathsegments2.add("12245");
		pathsegments2.add("yyeqy26356");
		pathsegments2.add("S1234");
		List<String> pathsegments3 = new ArrayList<>();
		pathsegments3.add("12245");
		pathsegments3.add(null);
		pathsegments3.add("2018-11-28T15:34:20.122");
		List<String> pathsegments4 = new ArrayList<>();
		pathsegments4.add("3000111");
		pathsegments4.add(null);
		pathsegments4.add("2018-11-28T15:34:20.122");
		List<String> pathsegments5 = new ArrayList<>();
		pathsegments5.add("12245");
		pathsegments5.add("3000111");
		pathsegments5.add("2018-11-28T15:34:20.122");
		List<String> pathsegments6 = new ArrayList<>();
		pathsegments6.add("2018-11-28T15:34:20.122");
		pathsegments6.add("12245");
		pathsegments6.add("yyeqy26356");
		pathsegments6.add("O1234");

		Mockito.when(osiUtils.getOSIDetailsFromMetaInfo(any(), any())).thenReturn(regOsi);
		Mockito.when(registrationProcessorRestService.getApi(ApiName.CENTERHISTORY, pathsegments, "", "",
				ResponseWrapper.class)).thenReturn(regrepdtoWrapper);

		Mockito.when(registrationProcessorRestService.getApi(ApiName.MACHINEHISTORY, pathsegments1, "", "",
				ResponseWrapper.class)).thenReturn(mhrepdtoWrapper);
		Mockito.when(registrationProcessorRestService.getApi(ApiName.CENTERUSERMACHINEHISTORY, pathsegments2, "", "",
				ResponseWrapper.class)).thenReturn(offrepdtoWrapper);
		Mockito.when(registrationProcessorRestService.getApi(ApiName.CENTERUSERMACHINEHISTORY, pathsegments6, "", "",
				ResponseWrapper.class)).thenReturn(offrepdtoWrapper);
		Mockito.when(registrationProcessorRestService.getApi(ApiName.REGISTRATIONCENTERTIMESTAMP, pathsegments3, "", "",
				ResponseWrapper.class)).thenReturn(testWrapper);
		Mockito.when(registrationProcessorRestService.postApi(any(), any(), any(), any(), any()))
				.thenReturn(deviceValidateResponseWrapper);
		Mockito.when(registrationProcessorRestService.getApi(ApiName.REGISTRATIONCENTERDEVICEHISTORY, pathsegments5, "",
				"", ResponseWrapper.class)).thenReturn(centerDeviceHistoryResponseDtoWrapper);

		// Mockito.when(registrationProcessorRestService.getApi(any(), any(), any(),
		// any(), any())).thenReturn(offrepdto).thenReturn(test)
		// .thenReturn(deviceHistoryResponsedto).thenReturn(registrationCenterDeviceHistoryResponseDto);
		// UMC validation successfull;
		assertFalse(umcValidator.isValidUMC("2018782130000121112018103016", registrationStatusDto));
	}

	@Test
	public void isValidCenterIdAndTimestampTest() throws ApisResourceAccessException, JsonParseException,
			JsonMappingException, IOException, java.io.IOException, PacketDecryptionFailureException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		identity = new Identity();
		RegistrationCenterDto rcdto = new RegistrationCenterDto();
		rcdto.setIsActive(true);
		rcdto.setLongitude("80.24492");
		rcdto.setLatitude("13.0049");
		rcdto.setId("12245");

		List<NewRegisteredDevice> capturedRegisteredDevices = new ArrayList<NewRegisteredDevice>();
		NewRegisteredDevice deviceDetails = new NewRegisteredDevice();
		deviceDetails.setDeviceCode("3000111");

		capturedRegisteredDevices.add(deviceDetails);
		// fv1 = new FieldValue();
		// fv1.setLabel("Document Scanner");
		// fv1.setValue("3000091");
		// capturedRegisteredDevices.add(fv1);
		// fv1 = new FieldValue();
		// fv1.setLabel("Camera");
		// fv1.setValue("3000071");
		// capturedRegisteredDevices.add(fv1);
		// fv1 = new FieldValue();
		// fv1.setLabel("Finger Print Scanner");
		// fv1.setValue("3000092");
		// capturedRegisteredDevices.add(fv1);
		identity.setCapturedRegisteredDevices(capturedRegisteredDevices);

		metaData = new ArrayList<>();
		FieldValue fv = new FieldValue();
		fv.setLabel("REGISTRATIONID");
		fv.setValue("2018782130000121112018103016");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("CENTERID");
		fv.setValue("12245");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("MACHINEID");
		fv.setValue("yyeqy26356");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("GEOLOCLATITUDE");
		fv.setValue("13.0049");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("GEOLOCLONGITUDE");
		fv.setValue("80.24492");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("CREATIONDATE");
		fv.setValue("2018-11-28T15:34:20.122");
		metaData.add(fv);

		identity.setMetaData(metaData);
		Mockito.when(osiUtils.getIdentity(any())).thenReturn(identity);
		List<RegistrationCenterDto> rcdtos = new ArrayList<>();
		rcdtos.add(rcdto);

		RegistrationCenterResponseDto regrepdto = new RegistrationCenterResponseDto();
		regrepdto.setRegistrationCentersHistory(rcdtos);

		ResponseWrapper<RegistrationCenterResponseDto> regrepdtoWrapper = new ResponseWrapper<>();

		regrepdtoWrapper.setResponse(regrepdto);
		regrepdtoWrapper.setErrors(null);

		MachineHistoryDto mcdto = new MachineHistoryDto();
		mcdto.setIsActive(true);
		mcdto.setId("yyeqy26356");

		ResponseWrapper<MachineHistoryDto> mcdtoWrapper = new ResponseWrapper<>();

		mcdtoWrapper.setResponse(mcdto);
		mcdtoWrapper.setErrors(null);

		List<MachineHistoryDto> mcdtos = new ArrayList<>();
		mcdtos.add(mcdto);
		MachineHistoryResponseDto mhrepdto = new MachineHistoryResponseDto();
		mhrepdto.setMachineHistoryDetails(mcdtos);

		ResponseWrapper<MachineHistoryResponseDto> mhrepdtoWrapper = new ResponseWrapper<>();

		mhrepdtoWrapper.setResponse(mhrepdto);
		mhrepdtoWrapper.setErrors(null);
		RegistrationCenterUserMachineMappingHistoryDto officerucmdto = new RegistrationCenterUserMachineMappingHistoryDto();
		officerucmdto.setIsActive(true);
		officerucmdto.setCntrId("12245");
		officerucmdto.setMachineId("yyeqy26356");
		officerucmdto.setUsrId("O1234");

		List<RegistrationCenterUserMachineMappingHistoryDto> officerucmdtos = new ArrayList<>();
		officerucmdtos.add(officerucmdto);

		RegistrationCenterUserMachineMappingHistoryResponseDto offrepdto = new RegistrationCenterUserMachineMappingHistoryResponseDto();

		offrepdto.setRegistrationCenters(officerucmdtos);

		ResponseWrapper<RegistrationCenterUserMachineMappingHistoryResponseDto> offrepdtoWrapper = new ResponseWrapper<>();

		offrepdtoWrapper.setResponse(offrepdto);
		offrepdtoWrapper.setErrors(null);
		RegistartionCenterTimestampResponseDto test = new RegistartionCenterTimestampResponseDto();
		test.setStatus("Valid");
		ResponseWrapper<RegistartionCenterTimestampResponseDto> testWrapper = new ResponseWrapper<>();
		List<ErrorDTO> errors = new ArrayList<ErrorDTO>();
		ErrorDTO error = new ErrorDTO();
		error.setMessage("Invalid  center Timestamp");
		errors.add(error);
		testWrapper.setResponse(null);
		testWrapper.setErrors(errors);
		DeviceValidateHistoryRequest deviceValidateHistoryRequest = new DeviceValidateHistoryRequest();
		deviceValidateHistoryRequest.setDeviceCode(deviceDetails.getDeviceCode());

		RequestWrapper<DeviceValidateHistoryRequest> request = new RequestWrapper<>();

		request.setRequest(deviceValidateHistoryRequest);

		DeviceValidateHistoryResponse deviceValidateResponse = new DeviceValidateHistoryResponse();
		deviceValidateResponse.setStatus("VALID");

		ResponseWrapper<DeviceValidateHistoryResponse> deviceValidateResponseWrapper = new ResponseWrapper<>();

		deviceValidateResponseWrapper.setResponse(deviceValidateResponse);
		deviceValidateResponseWrapper.setErrors(null);
		RegistrationCenterDeviceHistoryResponseDto registrationCenterDeviceHistoryResponseDto = new RegistrationCenterDeviceHistoryResponseDto();
		RegistrationCenterDeviceHistoryDto registrationCenterDeviceHistoryDetails = new RegistrationCenterDeviceHistoryDto();

		registrationCenterDeviceHistoryDetails.setIsActive(true);
		registrationCenterDeviceHistoryResponseDto
				.setRegistrationCenterDeviceHistoryDetails(registrationCenterDeviceHistoryDetails);

		ResponseWrapper<RegistrationCenterDeviceHistoryResponseDto> centerDeviceHistoryResponseDtoWrapper = new ResponseWrapper<>();
		// centerDeviceHistoryResponseDtoWrapper.setResponse(statusResponseDto);
		centerDeviceHistoryResponseDtoWrapper.setResponse(registrationCenterDeviceHistoryResponseDto);
		centerDeviceHistoryResponseDtoWrapper.setErrors(null);
		List<String> pathsegments = new ArrayList<>();
		pathsegments.add("12245");
		pathsegments.add(null);
		pathsegments.add("2018-11-28T15:34:20.122");
		List<String> pathsegments1 = new ArrayList<>();
		pathsegments1.add("yyeqy26356");
		pathsegments1.add(null);
		pathsegments1.add("2018-11-28T15:34:20.122");
		List<String> pathsegments2 = new ArrayList<>();
		pathsegments2.add("2018-11-28T15:34:20.122");
		pathsegments2.add("12245");
		pathsegments2.add("yyeqy26356");
		pathsegments2.add("S1234");
		List<String> pathsegments3 = new ArrayList<>();
		pathsegments3.add("12245");
		pathsegments3.add(null);
		pathsegments3.add("2018-11-28T15:34:20.122");
		List<String> pathsegments4 = new ArrayList<>();
		pathsegments4.add("3000111");
		pathsegments4.add(null);
		pathsegments4.add("2018-11-28T15:34:20.122");
		List<String> pathsegments5 = new ArrayList<>();
		pathsegments5.add("12245");
		pathsegments5.add("3000111");
		pathsegments5.add("2018-11-28T15:34:20.122");
		List<String> pathsegments6 = new ArrayList<>();
		pathsegments6.add("2018-11-28T15:34:20.122");
		pathsegments6.add("12245");
		pathsegments6.add("yyeqy26356");
		pathsegments6.add("O1234");

		Mockito.when(osiUtils.getOSIDetailsFromMetaInfo(any(), any())).thenReturn(regOsi);
		Mockito.when(registrationProcessorRestService.getApi(ApiName.CENTERHISTORY, pathsegments, "", "",
				ResponseWrapper.class)).thenReturn(regrepdtoWrapper);

		Mockito.when(registrationProcessorRestService.getApi(ApiName.MACHINEHISTORY, pathsegments1, "", "",
				ResponseWrapper.class)).thenReturn(mhrepdtoWrapper);
		Mockito.when(registrationProcessorRestService.getApi(ApiName.CENTERUSERMACHINEHISTORY, pathsegments2, "", "",
				ResponseWrapper.class)).thenReturn(offrepdtoWrapper);
		Mockito.when(registrationProcessorRestService.getApi(ApiName.CENTERUSERMACHINEHISTORY, pathsegments6, "", "",
				ResponseWrapper.class)).thenReturn(offrepdtoWrapper);
		Mockito.when(registrationProcessorRestService.getApi(ApiName.REGISTRATIONCENTERTIMESTAMP, pathsegments3, "", "",
				ResponseWrapper.class)).thenReturn(testWrapper);
		Mockito.when(registrationProcessorRestService.postApi(any(), any(), any(), any(), any()))
				.thenReturn(deviceValidateResponseWrapper);
		Mockito.when(registrationProcessorRestService.getApi(ApiName.REGISTRATIONCENTERDEVICEHISTORY, pathsegments5, "",
				"", ResponseWrapper.class)).thenReturn(centerDeviceHistoryResponseDtoWrapper);

		// Mockito.when(registrationProcessorRestService.getApi(any(), any(), any(),
		// any(), any())).thenReturn(offrepdto).thenReturn(test)
		// .thenReturn(deviceHistoryResponsedto).thenReturn(registrationCenterDeviceHistoryResponseDto);
		// UMC validation successfull;
		assertTrue(umcValidator.isValidUMC("2018782130000121112018103016", registrationStatusDto));
	}

	@Test
	public void isValidCenterUserMachineMappingTest() throws ApisResourceAccessException, JsonParseException,
			JsonMappingException, IOException, java.io.IOException, PacketDecryptionFailureException,
			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		identity = new Identity();
		RegistrationCenterDto rcdto = new RegistrationCenterDto();
		rcdto.setIsActive(true);
		rcdto.setLongitude("80.24492");
		rcdto.setLatitude("13.0049");
		rcdto.setId("12245");

		List<NewRegisteredDevice> capturedRegisteredDevices = new ArrayList<NewRegisteredDevice>();
		NewRegisteredDevice deviceDetails = new NewRegisteredDevice();
		deviceDetails.setDeviceCode("3000111");

		capturedRegisteredDevices.add(deviceDetails);
		// fv1 = new FieldValue();
		// fv1.setLabel("Document Scanner");
		// fv1.setValue("3000091");
		// capturedRegisteredDevices.add(fv1);
		// fv1 = new FieldValue();
		// fv1.setLabel("Camera");
		// fv1.setValue("3000071");
		// capturedRegisteredDevices.add(fv1);
		// fv1 = new FieldValue();
		// fv1.setLabel("Finger Print Scanner");
		// fv1.setValue("3000092");
		// capturedRegisteredDevices.add(fv1);
		identity.setCapturedRegisteredDevices(capturedRegisteredDevices);

		metaData = new ArrayList<>();
		FieldValue fv = new FieldValue();
		fv.setLabel("REGISTRATIONID");
		fv.setValue("2018782130000121112018103016");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("CENTERID");
		fv.setValue("12245");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("MACHINEID");
		fv.setValue("yyeqy26356");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("GEOLOCLATITUDE");
		fv.setValue("13.0049");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("GEOLOCLONGITUDE");
		fv.setValue("80.24492");
		metaData.add(fv);

		fv = new FieldValue();
		fv.setLabel("CREATIONDATE");
		fv.setValue("2018-11-28T15:34:20.122");
		metaData.add(fv);

		identity.setMetaData(metaData);
		Mockito.when(osiUtils.getIdentity(any())).thenReturn(identity);
		List<RegistrationCenterDto> rcdtos = new ArrayList<>();
		rcdtos.add(rcdto);

		RegistrationCenterResponseDto regrepdto = new RegistrationCenterResponseDto();
		regrepdto.setRegistrationCentersHistory(rcdtos);

		ResponseWrapper<RegistrationCenterResponseDto> regrepdtoWrapper = new ResponseWrapper<>();

		regrepdtoWrapper.setResponse(regrepdto);
		regrepdtoWrapper.setErrors(null);

		MachineHistoryDto mcdto = new MachineHistoryDto();
		mcdto.setIsActive(true);
		mcdto.setId("yyeqy26356");

		ResponseWrapper<MachineHistoryDto> mcdtoWrapper = new ResponseWrapper<>();

		mcdtoWrapper.setResponse(mcdto);
		mcdtoWrapper.setErrors(null);

		List<MachineHistoryDto> mcdtos = new ArrayList<>();
		mcdtos.add(mcdto);
		MachineHistoryResponseDto mhrepdto = new MachineHistoryResponseDto();
		mhrepdto.setMachineHistoryDetails(mcdtos);

		ResponseWrapper<MachineHistoryResponseDto> mhrepdtoWrapper = new ResponseWrapper<>();

		mhrepdtoWrapper.setResponse(mhrepdto);
		mhrepdtoWrapper.setErrors(null);
		RegistrationCenterUserMachineMappingHistoryDto officerucmdto = new RegistrationCenterUserMachineMappingHistoryDto();
		officerucmdto.setIsActive(true);
		officerucmdto.setCntrId("12245");
		officerucmdto.setMachineId("yyeqy26356");
		officerucmdto.setUsrId("O1234");

		List<RegistrationCenterUserMachineMappingHistoryDto> officerucmdtos = new ArrayList<>();
		officerucmdtos.add(officerucmdto);

		RegistrationCenterUserMachineMappingHistoryResponseDto offrepdto = new RegistrationCenterUserMachineMappingHistoryResponseDto();

		offrepdto.setRegistrationCenters(officerucmdtos);

		ResponseWrapper<RegistrationCenterUserMachineMappingHistoryResponseDto> offrepdtoWrapper = new ResponseWrapper<>();
		List<ErrorDTO> errors = new ArrayList<ErrorDTO>();
		ErrorDTO error = new ErrorDTO();
		error.setMessage("Invalid  operator center user");
		errors.add(error);
		offrepdtoWrapper.setResponse(null);
		offrepdtoWrapper.setErrors(errors);
		RegistartionCenterTimestampResponseDto test = new RegistartionCenterTimestampResponseDto();
		test.setStatus("Valid");
		ResponseWrapper<RegistartionCenterTimestampResponseDto> testWrapper = new ResponseWrapper<>();

		testWrapper.setResponse(test);
		testWrapper.setErrors(null);
		DeviceValidateHistoryRequest deviceValidateHistoryRequest = new DeviceValidateHistoryRequest();
		deviceValidateHistoryRequest.setDeviceCode(deviceDetails.getDeviceCode());

		RequestWrapper<DeviceValidateHistoryRequest> request = new RequestWrapper<>();

		request.setRequest(deviceValidateHistoryRequest);

		DeviceValidateHistoryResponse deviceValidateResponse = new DeviceValidateHistoryResponse();
		deviceValidateResponse.setStatus("VALID");

		ResponseWrapper<DeviceValidateHistoryResponse> deviceValidateResponseWrapper = new ResponseWrapper<>();

		deviceValidateResponseWrapper.setResponse(deviceValidateResponse);
		deviceValidateResponseWrapper.setErrors(null);
		RegistrationCenterDeviceHistoryResponseDto registrationCenterDeviceHistoryResponseDto = new RegistrationCenterDeviceHistoryResponseDto();
		RegistrationCenterDeviceHistoryDto registrationCenterDeviceHistoryDetails = new RegistrationCenterDeviceHistoryDto();

		registrationCenterDeviceHistoryDetails.setIsActive(true);
		registrationCenterDeviceHistoryResponseDto
				.setRegistrationCenterDeviceHistoryDetails(registrationCenterDeviceHistoryDetails);

		ResponseWrapper<RegistrationCenterDeviceHistoryResponseDto> centerDeviceHistoryResponseDtoWrapper = new ResponseWrapper<>();
		// centerDeviceHistoryResponseDtoWrapper.setResponse(statusResponseDto);
		centerDeviceHistoryResponseDtoWrapper.setResponse(registrationCenterDeviceHistoryResponseDto);
		centerDeviceHistoryResponseDtoWrapper.setErrors(null);
		List<String> pathsegments = new ArrayList<>();
		pathsegments.add("12245");
		pathsegments.add(null);
		pathsegments.add("2018-11-28T15:34:20.122");
		List<String> pathsegments1 = new ArrayList<>();
		pathsegments1.add("yyeqy26356");
		pathsegments1.add(null);
		pathsegments1.add("2018-11-28T15:34:20.122");
		List<String> pathsegments2 = new ArrayList<>();
		pathsegments2.add("2018-11-28T15:34:20.122");
		pathsegments2.add("12245");
		pathsegments2.add("yyeqy26356");
		pathsegments2.add("S1234");
		List<String> pathsegments3 = new ArrayList<>();
		pathsegments3.add("12245");
		pathsegments3.add(null);
		pathsegments3.add("2018-11-28T15:34:20.122");
		List<String> pathsegments4 = new ArrayList<>();
		pathsegments4.add("3000111");
		pathsegments4.add(null);
		pathsegments4.add("2018-11-28T15:34:20.122");
		List<String> pathsegments5 = new ArrayList<>();
		pathsegments5.add("12245");
		pathsegments5.add("3000111");
		pathsegments5.add("2018-11-28T15:34:20.122");
		List<String> pathsegments6 = new ArrayList<>();
		pathsegments6.add("2018-11-28T15:34:20.122");
		pathsegments6.add("12245");
		pathsegments6.add("yyeqy26356");
		pathsegments6.add("O1234");

		Mockito.when(osiUtils.getOSIDetailsFromMetaInfo(any(), any())).thenReturn(regOsi);
		Mockito.when(registrationProcessorRestService.getApi(ApiName.CENTERHISTORY, pathsegments, "", "",
				ResponseWrapper.class)).thenReturn(regrepdtoWrapper);

		Mockito.when(registrationProcessorRestService.getApi(ApiName.MACHINEHISTORY, pathsegments1, "", "",
				ResponseWrapper.class)).thenReturn(mhrepdtoWrapper);
		Mockito.when(registrationProcessorRestService.getApi(ApiName.CENTERUSERMACHINEHISTORY, pathsegments2, "", "",
				ResponseWrapper.class)).thenReturn(offrepdtoWrapper);
		Mockito.when(registrationProcessorRestService.getApi(ApiName.CENTERUSERMACHINEHISTORY, pathsegments6, "", "",
				ResponseWrapper.class)).thenReturn(offrepdtoWrapper);
		Mockito.when(registrationProcessorRestService.getApi(ApiName.REGISTRATIONCENTERTIMESTAMP, pathsegments3, "", "",
				ResponseWrapper.class)).thenReturn(testWrapper);

		Mockito.when(registrationProcessorRestService.postApi(any(), any(), any(), any(), any()))
				.thenReturn(deviceValidateResponseWrapper);
		Mockito.when(registrationProcessorRestService.getApi(ApiName.REGISTRATIONCENTERDEVICEHISTORY, pathsegments5, "",
				"", ResponseWrapper.class)).thenReturn(centerDeviceHistoryResponseDtoWrapper);

		// Mockito.when(registrationProcessorRestService.getApi(any(), any(), any(),
		// any(), any())).thenReturn(offrepdto).thenReturn(test)
		// .thenReturn(deviceHistoryResponsedto).thenReturn(registrationCenterDeviceHistoryResponseDto);
		// UMC validation successfull;
		assertFalse(umcValidator.isValidUMC("2018782130000121112018103016", registrationStatusDto));
	}

}
