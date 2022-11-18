package io.mosip.registration.processor.supervisor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.mosip.registration.processor.core.auth.dto.ErrorDTO;
import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Lists;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.QualityType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.registration.processor.core.auth.dto.AuthResponseDTO;
import io.mosip.registration.processor.core.auth.dto.IndividualIdDto;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.AuthSystemException;
import io.mosip.registration.processor.core.exception.ValidationFailedException;
import io.mosip.registration.processor.core.idrepo.dto.IdResponseDTO;
import io.mosip.registration.processor.core.idrepo.dto.ResponseDTO;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.packet.dto.FieldValue;
import io.mosip.registration.processor.core.packet.dto.FieldValueArray;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.RIDResponseDto;
import io.mosip.registration.processor.core.packet.dto.RegOsiDto;
import io.mosip.registration.processor.core.packet.dto.RidDto;
import io.mosip.registration.processor.core.packet.dto.ServerError;
import io.mosip.registration.processor.core.packet.dto.demographicinfo.DemographicInfoDto;
import io.mosip.registration.processor.core.packet.dto.masterdata.UserDetailsDto;
import io.mosip.registration.processor.core.packet.dto.masterdata.UserDetailsResponseDto;
import io.mosip.registration.processor.core.packet.dto.masterdata.UserResponseDto;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.RegistrationCenterUserMachineMappingHistoryDto;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.RegistrationCenterUserMachineMappingHistoryResponseDto;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.storage.utils.ABISHandlerUtil;
import io.mosip.registration.processor.packet.storage.utils.BioSdkUtil;
import io.mosip.registration.processor.packet.storage.utils.OSIUtils;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.stages.supervisorvalidator.SupervisorValidator;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.TransactionDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.TransactionService;

/**
 * The Class SupervisorValidatorTest.
 *
 * @author M1022006
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Utilities.class, IOUtils.class })
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "javax.net.ssl.*" })
public class SupervisorValidatorTest {

	/** The input stream. */
	@Mock
	private InputStream inputStream;
	
	@Mock
	ObjectMapper mapper;

	/** The registration status service. */
	@Mock
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	@Mock
	private IdRepoService idRepoService;

	/** The rest client service. */
	@Mock
	RegistrationProcessorRestClientService<Object> restClientService;

	/** The transcation status service. */
	@Mock
	private TransactionService<TransactionDto> transcationStatusService;

	/** The auth response DTO. */
	@Mock
	AuthResponseDTO authResponseDTO = new AuthResponseDTO();

	@Mock
	private ABISHandlerUtil abisHandlerUtil;

	/** The env. */
	@Mock
	Environment env;

	@Mock
	private OSIUtils osiUtils;

	/** The data. */
	byte[] data = "1234567890".getBytes();

	/** The reg osi dto. */
	private RegOsiDto regOsiDto = new RegOsiDto();

	/** The registration status dto. */
	InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();

	/** The transaction dto. */
	TransactionDto transactionDto = new TransactionDto();

	/** The supervisor validator. */
	@InjectMocks
	SupervisorValidator supervisorValidator;

	@Mock
	private Utilities utility;

	@Mock
	private BioSdkUtil bioUtil;

	private Map<String, String> metaInfo;

	/** The demographic dedupe dto list. */
	List<DemographicInfoDto> demographicDedupeDtoList = new ArrayList<>();

	/** The demographic info dto. */
	DemographicInfoDto demographicInfoDto = new DemographicInfoDto();

	/** The identity. */
	private Identity identity = new Identity();

	private UserResponseDto userResponseDto = new UserResponseDto();
	private RidDto ridDto = new RidDto();
	private ResponseDTO responseDTO1 = new ResponseDTO();
	private RIDResponseDto ridResponseDto1 = new RIDResponseDto();
	private IdResponseDTO idResponseDTO = new IdResponseDTO();
	private io.mosip.registration.processor.core.http.ResponseWrapper<RegistrationCenterUserMachineMappingHistoryResponseDto> centerResponse = new io.mosip.registration.processor.core.http.ResponseWrapper<RegistrationCenterUserMachineMappingHistoryResponseDto>();
	private io.mosip.registration.processor.core.http.ResponseWrapper<IndividualIdDto> individualResponse = new io.mosip.registration.processor.core.http.ResponseWrapper<IndividualIdDto>();
	private RegistrationCenterUserMachineMappingHistoryResponseDto centerMapping = new RegistrationCenterUserMachineMappingHistoryResponseDto();
	private List<RegistrationCenterUserMachineMappingHistoryDto> registrationCenters = new ArrayList<RegistrationCenterUserMachineMappingHistoryDto>();
	private RegistrationCenterUserMachineMappingHistoryDto center = new RegistrationCenterUserMachineMappingHistoryDto();
	private IndividualIdDto individualIdDto = new IndividualIdDto();
	
	BiometricRecord biometricRecord = new BiometricRecord();
	
	@Mock
	RegistrationExceptionMapperUtil registrationExceptionMapperUtil;

	@Mock
	LogDescription description;
	private ClassLoader classLoader;

	@Mock
	private PriorityBasedPacketManagerService packetManagerService;

	/**
	 * Sets the up.
	 *
	 * @throws Exception the exception
	 */
	@Before
	public void setUp() throws Exception {
		classLoader = getClass().getClassLoader();

		Mockito.when(utility.getGetRegProcessorDemographicIdentity()).thenReturn("identity");
		File file = new File(classLoader.getResource("RegistrationProcessorIdentity.json").getFile());
		InputStream inputStream = new FileInputStream(file);
		String mappingJson = IOUtils.toString(inputStream);
		JSONObject mappingJSONObject = JsonUtil.objectMapperReadValue(mappingJson, JSONObject.class);
		Mockito.when(utility.getRegistrationProcessorMappingJson(anyString()))
				.thenReturn(JsonUtil.getJSONObject(mappingJSONObject, MappingJsonConstants.IDENTITY));
		PowerMockito.mockStatic(Utilities.class);
		PowerMockito.when(Utilities.class, "getJson", anyString(), anyString()).thenReturn(mappingJson);
		Mockito.doNothing().when(description).setMessage(any());
		Mockito.when(registrationExceptionMapperUtil.getStatusCode(any())).thenReturn("ERROR");
		regOsiDto.setOfficerId("O1234");
		regOsiDto.setOfficerHashedPin("officerHashedPin");
		regOsiDto.setSupervisorId("S1234");
		regOsiDto.setSupervisorHashedPin("supervisorHashedPin");
		regOsiDto.setIntroducerTyp("Parent");
		regOsiDto.setPacketCreationDate("2020-08-06T11:35:13.934Z");
		demographicDedupeDtoList.add(demographicInfoDto);

		metaInfo = new HashMap<>();
		metaInfo.put(JsonConstant.OFFICERPIN, "S1234");
		metaInfo.put(JsonConstant.OFFICERPWR, "S1234");
		metaInfo.put(JsonConstant.OFFICERID, "S1234");
		metaInfo.put(JsonConstant.OFFICEROTPAUTHENTICATION, "S1234");
		metaInfo.put(JsonConstant.PREREGISTRATIONID, "123234567899");
		metaInfo.put(JsonConstant.REGISTRATIONID, "1234567890");
		metaInfo.put(MappingJsonConstants.SUPERVISORBIOMETRICFILENAME, "S1234");
		metaInfo.put(MappingJsonConstants.OFFICERBIOMETRICFILENAME, "S1234");
		metaInfo.put(JsonConstant.OFFICERPHOTONAME, "S1234");
		metaInfo.put(JsonConstant.SUPERVISORPWR, "S1234");
		metaInfo.put(JsonConstant.SUPERVISORID, "S1234");

		metaInfo.put(JsonConstant.CENTERID, "S1234");
		metaInfo.put(JsonConstant.MACHINEID, "S1234");
		metaInfo.put(JsonConstant.GEOLOCLATITUDE, "S1234");
		metaInfo.put(JsonConstant.GEOLOCLONGITUDE, "S1234");
		metaInfo.put(JsonConstant.CREATIONDATE, "2020-08-06T11:35:13.934Z");

		RegOsiDto regOsi = new RegOsiDto();
		regOsi.setOfficerHashedPin("S1234");
		regOsi.setOfficerHashedPwd("S1234");
		regOsi.setOfficerId("S1234");
		regOsi.setOfficerOTPAuthentication("false");
		regOsi.setPreregId("S1234");
		regOsi.setRegId("S1234");
		regOsi.setSupervisorBiometricFileName("S1234");
		regOsi.setSupervisorHashedPin("S1234");
		regOsi.setSupervisorHashedPwd("S1234");
		regOsi.setSupervisorId("S1234");
		regOsi.setSupervisorOTPAuthentication("false");
		regOsi.setOfficerBiometricFileName("S1234");
		regOsi.setRegcntrId("S1234");
		regOsi.setMachineId("S1234");
		regOsi.setLatitude("S1234");
		regOsi.setLongitude("S1234");
		regOsi.setPacketCreationDate("2020-08-06T11:35:13.934Z");

		Mockito.when(osiUtils.getOSIDetailsFromMetaInfo(anyMap())).thenReturn(regOsi);

		PowerMockito.mockStatic(IOUtils.class);
		PowerMockito.when(IOUtils.class, "toByteArray", inputStream).thenReturn(data);

		io.mosip.registration.processor.core.auth.dto.ResponseDTO responseDTO = new io.mosip.registration.processor.core.auth.dto.ResponseDTO();
		responseDTO.setAuthStatus(true);
		authResponseDTO.setResponse(responseDTO);
		Mockito.when(restClientService.postApi(any(), anyString(), anyString(), anyString(), any()))
				.thenReturn(authResponseDTO);

		registrationStatusDto.setRegistrationId("reg1234");
		registrationStatusDto.setApplicantType("Child");
		registrationStatusDto.setRegistrationType("New");

		Mockito.when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any())).thenReturn(registrationStatusDto);

		FieldValue officerBiofileName = new FieldValue();
		officerBiofileName.setLabel(MappingJsonConstants.OFFICERBIOMETRICFILENAME);
		officerBiofileName.setValue("officer_bio_CBEFF");

		FieldValue officerPassword = new FieldValue();
		officerPassword.setLabel(JsonConstant.OFFICERPWR);
		officerPassword.setValue("false");

		FieldValue officerOtp = new FieldValue();
		officerOtp.setLabel(JsonConstant.OFFICEROTPAUTHENTICATION);
		officerOtp.setValue("false");

		FieldValue supervisorPassword = new FieldValue();
		supervisorPassword.setLabel(JsonConstant.SUPERVISORPWR);
		supervisorPassword.setValue("true");

		FieldValue supervisorId = new FieldValue();
		supervisorId.setLabel(JsonConstant.SUPERVISORID);
		supervisorId.setValue("110016");

		FieldValue supervisorOtp = new FieldValue();
		supervisorOtp.setLabel(MappingJsonConstants.SUPERVISOROTPAUTHENTICATION);
		supervisorOtp.setValue("false");

		FieldValue supervisorBiofileName = new FieldValue();
		supervisorBiofileName.setLabel(MappingJsonConstants.SUPERVISORBIOMETRICFILENAME);
		supervisorBiofileName.setValue("supervisor_bio_CBEFF");

		FieldValue creationDate = new FieldValue();
		creationDate.setLabel("creationDate");
		creationDate.setValue("2019-04-30T12:42:03.541Z");

		identity.setOsiData((Arrays.asList(officerBiofileName, officerOtp, officerPassword, supervisorOtp,
				supervisorPassword, supervisorId, supervisorBiofileName)));
		identity.setMetaData((Arrays.asList(creationDate)));
		List<FieldValueArray> fieldValueArrayList = new ArrayList<FieldValueArray>();
		FieldValueArray introducerBiometric = new FieldValueArray();
		introducerBiometric.setLabel(PacketFiles.INTRODUCERBIOMETRICSEQUENCE.name());
		List<String> introducerBiometricValues = new ArrayList<String>();
		introducerBiometricValues.add("introducer_bio_CBEFF");
		introducerBiometric.setValue(introducerBiometricValues);
		fieldValueArrayList.add(introducerBiometric);
		identity.setHashSequence(fieldValueArrayList);
		//regOsiDto.setSupervisorBiometricFileName("supervisor_bio_CBEFF");
		// regOsiDto.setOfficerBiometricFileName("officer_bio_CBEFF");
		UserDetailsResponseDto userDetailsResponseDto = new UserDetailsResponseDto();
		UserDetailsDto userDetailsDto = new UserDetailsDto();
		userDetailsDto.setIsActive(true);
		userDetailsResponseDto.setUserResponseDto(Arrays.asList(userDetailsDto));
		userResponseDto.setResponse(userDetailsResponseDto);
		ridDto.setRid("reg4567");
		ridResponseDto1.setResponse(ridDto);
		String identityJson = "{\"UIN\":\"123456\"}";
		responseDTO1.setIdentity(identityJson);
		idResponseDTO.setResponse(responseDTO1);
		regOsiDto.setSupervisorHashedPwd("true");
		regOsiDto.setOfficerHashedPwd("true");
		regOsiDto.setSupervisorBiometricFileName("supervisorBiometrics");
		
		center.setCntrId("10001");
		center.setIsActive(true);
		registrationCenters.add(center);
		centerMapping.setRegistrationCenters(registrationCenters);
		centerResponse.setErrors(null);
		centerResponse.setResponse(centerMapping);
		
		individualIdDto.setIndividualId("6531762");
		individualResponse.setResponse(individualIdDto);
		individualResponse.setErrors(null);
		//ObjectMapper mockObjectMapper = Mockito.mock(ObjectMapper.class);
		Mockito.when(mapper.writeValueAsString(any())).thenReturn("Qiia8saij");
		Mockito.when(mapper.readValue(anyString(), eq(IndividualIdDto.class))).thenReturn(individualIdDto);
		Mockito.when(mapper.readValue(anyString(),
				Mockito.eq(RegistrationCenterUserMachineMappingHistoryResponseDto.class))).thenReturn(centerMapping);
		Mockito.when(restClientService.getApi(any(), any(), anyString(), any(), any()))
		.thenReturn(userResponseDto).thenReturn(individualResponse).thenReturn(centerResponse);
		Mockito.when(osiUtils.getOSIDetailsFromMetaInfo(anyMap())).thenReturn(regOsiDto);

		BIR birType3 = new BIR.BIRBuilder().build();
		BDBInfo bdbInfoType3 = new BDBInfo.BDBInfoBuilder().build();
		bdbInfoType3.setQuality(new QualityType());
		BiometricType singleType3 = BiometricType.IRIS;
		List<BiometricType> singleTypeList3 = new ArrayList<>();
		singleTypeList3.add(singleType3);
		List<String> subtype3 = new ArrayList<>(Arrays.asList("Right"));
		bdbInfoType3.setSubtype(subtype3);
		bdbInfoType3.setType(singleTypeList3);
		birType3.setBdbInfo(bdbInfoType3);

		BIR birType4 = new BIR.BIRBuilder().build();
		BDBInfo bdbInfoType4 = new BDBInfo.BDBInfoBuilder().build();
		bdbInfoType4.setQuality(new QualityType());
		BiometricType singleType4 = BiometricType.FACE;
		List<BiometricType> singleTypeList4 = new ArrayList<>();
		singleTypeList4.add(singleType4);
		List<String> subtype4 = new ArrayList<>();
		bdbInfoType4.setSubtype(subtype4);
		bdbInfoType4.setType(singleTypeList4);
		birType4.setBdbInfo(bdbInfoType4);

		biometricRecord.setSegments(Lists.newArrayList(birType3, birType4));

		when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), anyString(), any()))
				.thenReturn("field");
	}

	/**
	 * Test is valid Supervisor success.
	 *
	 * @throws Exception the exception
	 */
	@Test(expected = ValidationFailedException.class)
	public void testisValidSupervisorSuccess() throws Exception {

		Mockito.when(registrationStatusService.checkUinAvailabilityForRid(any())).thenReturn(true);
		registrationStatusDto.setRegistrationType("ACTIVATED");
		when(packetManagerService.getBiometricsByMappingJsonKey(anyString(), any(), any(), any()))
		.thenReturn(null);
		doNothing().when(bioUtil).authenticateBiometrics(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
		supervisorValidator.validate("reg1234", registrationStatusDto, regOsiDto);
	}
	
	@Test(expected = BaseCheckedException.class)
	public void testisValidSupervisorCreatedDateNull() throws Exception {

		Mockito.when(registrationStatusService.checkUinAvailabilityForRid(any())).thenReturn(true);
		regOsiDto.setPacketCreationDate(null);
		registrationStatusDto.setRegistrationType("ACTIVATED");
		supervisorValidator.validate("reg1234", registrationStatusDto, regOsiDto);
	}
	
	@Test(expected = ValidationFailedException.class)
	public void testisValidSupervisorBiometricfileNull() throws Exception {

		Mockito.when(registrationStatusService.checkUinAvailabilityForRid(any())).thenReturn(true);
		regOsiDto.setSupervisorBiometricFileName(null);
		regOsiDto.setSupervisorHashedPwd(null);
		registrationStatusDto.setRegistrationType("ACTIVATED");
		supervisorValidator.validate("reg1234", registrationStatusDto, regOsiDto);
	}
	
	@Test(expected = ValidationFailedException.class)
	public void testisValidSupervisorBiometricRecordNull() throws Exception {

		Mockito.when(registrationStatusService.checkUinAvailabilityForRid(any())).thenReturn(true);
		when(packetManagerService.getBiometricsByMappingJsonKey(anyString(), any(), any(), any()))
		.thenReturn(null);
		registrationStatusDto.setRegistrationType("ACTIVATED");
		supervisorValidator.validate("reg1234", registrationStatusDto, regOsiDto);
	}

	@Test(expected = ValidationFailedException.class)
	public void testAuthByIdAuthenticationStatusInActive() throws Exception {

		io.mosip.registration.processor.core.auth.dto.ResponseDTO responseDTO = new io.mosip.registration.processor.core.auth.dto.ResponseDTO();
		responseDTO.setAuthStatus(false);
		authResponseDTO.setResponse(responseDTO);
		Mockito.when(registrationStatusService.checkUinAvailabilityForRid(any())).thenReturn(true);
		when(packetManagerService.getBiometricsByMappingJsonKey(anyString(), any(), any(), any()))
		.thenReturn(null);
		doNothing().when(bioUtil).authenticateBiometrics(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
		registrationStatusDto.setRegistrationType("ACTIVATED");
		supervisorValidator.validate("reg1234", registrationStatusDto, regOsiDto);
	}

	@Test(expected = ValidationFailedException.class)
	public void testAuthByIdAuthenticationStatusFailed() throws Exception {

		io.mosip.registration.processor.core.auth.dto.ResponseDTO responseDTO = new io.mosip.registration.processor.core.auth.dto.ResponseDTO();
		io.mosip.registration.processor.core.auth.dto.ErrorDTO errorDTO = new io.mosip.registration.processor.core.auth.dto.ErrorDTO();
		responseDTO.setAuthStatus(false);
		errorDTO.setErrorCode("ERR-001");
		errorDTO.setErrorMessage("error occured");
		authResponseDTO.setResponse(responseDTO);
		authResponseDTO.setErrors(Arrays.asList(errorDTO));
		Mockito.when(registrationStatusService.checkUinAvailabilityForRid(any())).thenReturn(true);
		when(packetManagerService.getBiometricsByMappingJsonKey(anyString(), any(), any(), any()))
		.thenReturn(null);
		doNothing().when(bioUtil).authenticateBiometrics(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
		registrationStatusDto.setRegistrationType("ACTIVATED");
		supervisorValidator.validate("reg1234", registrationStatusDto, regOsiDto);
	}

	@Test(expected = BaseCheckedException.class)
	public void testoperatorPasswordNull() throws Exception {
		regOsiDto.setOfficerBiometricFileName(null);
		regOsiDto.setSupervisorBiometricFileName(null);
		regOsiDto.setSupervisorHashedPwd(null);
		regOsiDto.setOfficerHashedPwd(null);
		Mockito.when(osiUtils.getOSIDetailsFromMetaInfo(anyMap())).thenReturn(regOsiDto);

		Mockito.when(registrationStatusService.checkUinAvailabilityForRid(any())).thenReturn(true);
		registrationStatusDto.setRegistrationType("ACTIVATED");
		supervisorValidator.validate("reg1234", registrationStatusDto, regOsiDto);
	}

	@Test(expected = BaseCheckedException.class)
	public void testusernotActive() throws Exception {
		UserDetailsResponseDto userDetailsResponseDto = new UserDetailsResponseDto();
		UserDetailsDto userDetailsDto = new UserDetailsDto();
		userDetailsDto.setIsActive(false);
		userDetailsResponseDto.setUserResponseDto(Arrays.asList(userDetailsDto));
		userResponseDto.setResponse(userDetailsResponseDto);

		Mockito.when(registrationStatusService.checkUinAvailabilityForRid(any())).thenReturn(true);
		Mockito.when(restClientService.getApi(any(), any(), anyString(), any(), any())).thenReturn(userResponseDto)
				.thenReturn(userResponseDto);
		registrationStatusDto.setRegistrationType("ACTIVATED");
		supervisorValidator.validate("reg1234", registrationStatusDto, regOsiDto);
	}

	@Test(expected = BaseCheckedException.class)
	public void testinvalidUserInput() throws Exception {
		ServerError error = new ServerError();
		error.setMessage("Invalid Date format");
		List<ServerError> errors = new ArrayList<>();
		errors.add(error);
		userResponseDto.setErrors(errors);

		Mockito.when(registrationStatusService.checkUinAvailabilityForRid(any())).thenReturn(true);
		Mockito.when(restClientService.getApi(any(), any(), anyString(), any(), any())).thenReturn(userResponseDto)
				.thenReturn(userResponseDto);
		registrationStatusDto.setRegistrationType("ACTIVATED");
		supervisorValidator.validate("reg1234", registrationStatusDto, regOsiDto);
	}

	@Test(expected = ApisResourceAccessException.class)
	public void tesApisResourceAccessException() throws Exception {
		ApisResourceAccessException apisResourceAccessException = new ApisResourceAccessException("bad request");
		Mockito.when(restClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenThrow(apisResourceAccessException);

		Mockito.when(registrationStatusService.checkUinAvailabilityForRid(any())).thenReturn(true);
		registrationStatusDto.setRegistrationType("ACTIVATED");
		supervisorValidator.validate("reg1234", registrationStatusDto, regOsiDto);
	}

	@Test(expected = ApisResourceAccessException.class)
	public void testHttpClientErrorException() throws Exception {
		HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
				"error");
		ApisResourceAccessException apisResourceAccessException = new ApisResourceAccessException("bad request",
				httpClientErrorException);
		Mockito.when(restClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenThrow(apisResourceAccessException);

		Mockito.when(registrationStatusService.checkUinAvailabilityForRid(any())).thenReturn(true);
		registrationStatusDto.setRegistrationType("ACTIVATED");
		supervisorValidator.validate("reg1234", registrationStatusDto, regOsiDto);

	}

	@Test(expected = ApisResourceAccessException.class)
	public void testHttpServerErrorException() throws Exception {
		HttpServerErrorException httpServerErrorException = new HttpServerErrorException(HttpStatus.BAD_REQUEST,
				"error");
		ApisResourceAccessException apisResourceAccessException = new ApisResourceAccessException("bad request",
				httpServerErrorException);
		Mockito.when(restClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenThrow(apisResourceAccessException);

		Mockito.when(registrationStatusService.checkUinAvailabilityForRid(any())).thenReturn(true);
		registrationStatusDto.setRegistrationType("ACTIVATED");
		supervisorValidator.validate("reg1234", registrationStatusDto, regOsiDto);

	}

	/**
	 * Test supervisor details null.
	 *
	 * @throws Exception the exception
	 */
	@Test(expected = BaseCheckedException.class)
	public void testSupervisorDetailsNull() throws Exception {
		regOsiDto.setOfficerId(null);
		regOsiDto.setSupervisorId(null);
		Mockito.when(osiUtils.getOSIDetailsFromMetaInfo(anyMap())).thenReturn(regOsiDto);
		supervisorValidator.validate("reg1234", registrationStatusDto, regOsiDto);
	}
	
	@Test(expected = ApisResourceAccessException.class)
	public void testIndividualIdResponseFailed() throws Exception {

		io.mosip.registration.processor.core.common.rest.dto.ErrorDTO errordto = new io.mosip.registration.processor.core.common.rest.dto.ErrorDTO();
		errordto.setErrorCode("ERROR-001");
		errordto.setMessage("individual Id not found");
		individualResponse.setErrors(Arrays.asList(errordto));
		individualIdDto.setIndividualId("6531762");

		Mockito.when(registrationStatusService.checkUinAvailabilityForRid(any())).thenReturn(true);
		when(packetManagerService.getBiometricsByMappingJsonKey(anyString(), any(), any(), any()))
				.thenReturn(biometricRecord);
		supervisorValidator.validate("reg1234", registrationStatusDto, regOsiDto);
	}

	@Test(expected = ValidationFailedException.class)
	public void testSupervisorAuthSystemError() throws Exception {
		ErrorDTO errordto = new ErrorDTO();
		errordto.setErrorCode("IDA-MLC-007");
		authResponseDTO.setErrors(Arrays.asList(errordto));
		when(packetManagerService.getBiometricsByMappingJsonKey(anyString(), any(), any(), any()))
				.thenReturn(null);
		doNothing().when(bioUtil).authenticateBiometrics(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
		supervisorValidator.validate("reg1234", registrationStatusDto, regOsiDto);
	}

	@Test(expected = BaseCheckedException.class)
	public void testMappingNotFoundResponse() throws Exception {

		io.mosip.registration.processor.core.common.rest.dto.ErrorDTO errordto = new io.mosip.registration.processor.core.common.rest.dto.ErrorDTO();
		errordto.setErrorCode("ERROR-001");
		errordto.setMessage("mapping not found");
		centerResponse.setErrors(Arrays.asList(errordto));

		Mockito.when(registrationStatusService.checkUinAvailabilityForRid(any())).thenReturn(true);
		when(packetManagerService.getBiometricsByMappingJsonKey(anyString(), any(), any(), any()))
				.thenReturn(biometricRecord);
		doNothing().when(bioUtil).authenticateBiometrics(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
		regOsiDto.setPacketCreationDate(null);
		supervisorValidator.validate("reg1234", registrationStatusDto, regOsiDto);
	}

	@Test(expected = ValidationFailedException.class)
	public void testUMCValidationFailed() throws Exception {
		
		center.setCntrId("10001");
		center.setIsActive(false);
		registrationCenters.add(center);
		centerMapping.setRegistrationCenters(registrationCenters);
		centerResponse.setErrors(null);
		centerResponse.setResponse(centerMapping);
		Mockito.when(mapper.writeValueAsString(any())).thenReturn("");
		Mockito.when(mapper.readValue(anyString(),
				Mockito.eq(RegistrationCenterUserMachineMappingHistoryResponseDto.class))).thenReturn(centerMapping);
		when(packetManagerService.getBiometricsByMappingJsonKey(anyString(), any(), any(), any()))
		.thenReturn(null);
		Mockito.when(restClientService.getApi(any(), any(), anyString(), any(), any()))
		.thenReturn(userResponseDto).thenReturn(centerResponse);
		doNothing().when(bioUtil).authenticateBiometrics(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
		supervisorValidator.validate("reg1234", registrationStatusDto, regOsiDto);
	}

}
