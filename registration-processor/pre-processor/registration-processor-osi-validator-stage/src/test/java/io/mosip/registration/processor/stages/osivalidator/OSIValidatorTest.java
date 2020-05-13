package io.mosip.registration.processor.stages.osivalidator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import io.mosip.registration.processor.packet.utility.exception.ApiNotAccessibleException;
import org.apache.commons.io.IOUtils;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.xml.sax.SAXException;

import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.registration.processor.core.auth.dto.AuthResponseDTO;
import io.mosip.registration.processor.core.auth.dto.ErrorDTO;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.AuthSystemException;
import io.mosip.registration.processor.core.exception.BioTypeException;
import io.mosip.registration.processor.core.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.core.exception.ParentOnHoldException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
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
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.storage.utils.ABISHandlerUtil;
import io.mosip.registration.processor.packet.storage.utils.AuthUtil;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.packet.utility.service.PacketReaderService;
import io.mosip.registration.processor.packet.utility.utils.IdSchemaUtils;
import io.mosip.registration.processor.stages.osivalidator.utils.OSIUtils;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.TransactionDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.TransactionService;

/**
 * The Class OSIValidatorTest.
 *
 * @author M1022006
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Utilities.class, IOUtils.class })
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*" })
public class OSIValidatorTest {

	/** The input stream. */
	@Mock
	private InputStream inputStream;

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

	/** The osi validator. */
	@InjectMocks
	OSIValidator osiValidator;

	@Mock
	private Utilities utility;

	@Mock
	private AuthUtil authUtil;

	/** The demographic dedupe dto list. */
	List<DemographicInfoDto> demographicDedupeDtoList = new ArrayList<>();

	/** The demographic info dto. */
	DemographicInfoDto demographicInfoDto = new DemographicInfoDto();

	/** The identity. */
	private Identity identity = new Identity();

	private String childMappingJson;

	private JSONObject demoJson = new JSONObject();
	private UserResponseDto userResponseDto = new UserResponseDto();
	private RidDto ridDto = new RidDto();
	private ResponseDTO responseDTO1 = new ResponseDTO();
	private RIDResponseDto ridResponseDto1 = new RIDResponseDto();
	private IdResponseDTO idResponseDTO = new IdResponseDTO();
	@Mock
	RegistrationExceptionMapperUtil registrationExceptionMapperUtil;

	@Mock
	LogDescription description;
	private ClassLoader classLoader;

	@Mock
	private PacketReaderService packetReaderService;

	@Mock
	private IdSchemaUtils idSchemaUtils;
	/**
	 * Sets the up.
	 *
	 * @throws Exception the exception
	 */
	@Before
	public void setUp() throws Exception {
		classLoader = getClass().getClassLoader();
		File childFile = new File(classLoader.getResource("Child_ID.json").getFile());
		InputStream is = new FileInputStream(childFile);
		childMappingJson = IOUtils.toString(is, "UTF-8");

		ReflectionTestUtils.setField(osiValidator, "ageLimit", "5");
		ReflectionTestUtils.setField(osiValidator, "dobFormat", "yyyy/MM/dd");
		ReflectionTestUtils.setField(osiValidator, "introducerValidation", true);
		ReflectionTestUtils.setField(osiValidator, "officerBiometricFileSource", "id");
		ReflectionTestUtils.setField(osiValidator, "supervisorBiometricFileSource", "id");
		File idJson = new File(classLoader.getResource("ID.json").getFile());
		InputStream ip = new FileInputStream(idJson);
		String idJsonString = IOUtils.toString(ip, "UTF-8");
		Mockito.when(utility.getDemographicIdentityJSONObject(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(JsonUtil.getJSONObject(
				JsonUtil.objectMapperReadValue(idJsonString, JSONObject.class), MappingJsonConstants.IDENTITY));

		Mockito.when(utility.getGetRegProcessorDemographicIdentity()).thenReturn("identity");
		File file = new File(classLoader.getResource("RegistrationProcessorIdentity.json").getFile());
		InputStream inputStream = new FileInputStream(file);
		String mappingJson = IOUtils.toString(inputStream);
		JSONObject mappingJSONObject = JsonUtil.objectMapperReadValue(mappingJson, JSONObject.class);
		Mockito.when(utility.getRegistrationProcessorMappingJson())
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
		demographicDedupeDtoList.add(demographicInfoDto);

		Mockito.when(env.getProperty("mosip.kernel.applicant.type.age.limit")).thenReturn("5");



		PowerMockito.mockStatic(IOUtils.class);
		PowerMockito.when(IOUtils.class, "toByteArray", inputStream).thenReturn(data);

		io.mosip.registration.processor.core.auth.dto.ResponseDTO responseDTO = new io.mosip.registration.processor.core.auth.dto.ResponseDTO();
		responseDTO.setAuthStatus(true);
		authResponseDTO.setResponse(responseDTO);
		Mockito.when(restClientService.postApi(any(), anyString(), anyString(), anyString(), any()))
				.thenReturn(authResponseDTO);

		Mockito.when(authUtil.authByIdAuthentication(anyString(), any(), any())).thenReturn(authResponseDTO);

		registrationStatusDto.setRegistrationId("reg1234");
		registrationStatusDto.setApplicantType("Child");
		registrationStatusDto.setRegistrationType("New");

		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);

		FieldValue officerBiofileName = new FieldValue();
		officerBiofileName.setLabel(JsonConstant.OFFICERBIOMETRICFILENAME);
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
		supervisorOtp.setLabel(JsonConstant.SUPERVISOROTPAUTHENTICATION);
		supervisorOtp.setValue("false");

		FieldValue supervisorBiofileName = new FieldValue();
		supervisorBiofileName.setLabel(JsonConstant.SUPERVISORBIOMETRICFILENAME);
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
		regOsiDto.setSupervisorBiometricFileName("supervisor_bio_CBEFF");

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
		Mockito.when(osiUtils.getIdentity(anyString())).thenReturn(identity);
		Mockito.when(restClientService.getApi(any(), any(), any(), any(), any())).thenReturn(userResponseDto)
				.thenReturn(userResponseDto).thenReturn(ridResponseDto1).thenReturn(idResponseDTO)
				.thenReturn(ridResponseDto1).thenReturn(idResponseDTO);
		Mockito.when(osiUtils.getOSIDetailsFromMetaInfo(anyString(), any())).thenReturn(regOsiDto);
		File cbeffFile = new File(classLoader.getResource("cbeff.xml").getFile());
		InputStream cbeffInputstream = new FileInputStream(cbeffFile);
		Mockito.when(packetReaderService.getFile(anyString(), anyString(), anyString())).thenReturn(cbeffInputstream);
		Mockito.when(idSchemaUtils.getSource((anyString()))).thenReturn("id");
	}

	/**
	 * Testis valid OSI success.
	 *
	 * @throws Exception the exception
	 */
	@Test
	public void testisValidOSISuccess() throws Exception {
		Mockito.when(osiUtils.getMetaDataValue(anyString(), any()))
				.thenReturn(identity.getMetaData().get(0).getValue());
		Mockito.when(registrationStatusService.checkUinAvailabilityForRid(any())).thenReturn(true);
		registrationStatusDto.setRegistrationType("ACTIVATED");
		boolean isValid = osiValidator.isValidOSI("reg1234", registrationStatusDto);
		assertTrue(isValid);
	}

	@Test
	public void testoperatorPasswordNull() throws Exception {
		regOsiDto.setOfficerBiometricFileName(null);
		regOsiDto.setSupervisorBiometricFileName(null);
		regOsiDto.setSupervisorHashedPwd(null);
		regOsiDto.setOfficerHashedPwd(null);
		Mockito.when(osiUtils.getOSIDetailsFromMetaInfo(anyString(), any())).thenReturn(regOsiDto);
		Mockito.when(osiUtils.getMetaDataValue(anyString(), any()))
				.thenReturn(identity.getMetaData().get(0).getValue());
		Mockito.when(registrationStatusService.checkUinAvailabilityForRid(any())).thenReturn(true);
		registrationStatusDto.setRegistrationType("ACTIVATED");
		boolean isValid = osiValidator.isValidOSI("reg1234", registrationStatusDto);
		assertFalse(isValid);
	}

	@Test
	public void testusernotActive() throws Exception {
		UserDetailsResponseDto userDetailsResponseDto = new UserDetailsResponseDto();
		UserDetailsDto userDetailsDto = new UserDetailsDto();
		userDetailsDto.setIsActive(false);
		userDetailsResponseDto.setUserResponseDto(Arrays.asList(userDetailsDto));
		userResponseDto.setResponse(userDetailsResponseDto);
		Mockito.when(osiUtils.getMetaDataValue(anyString(), any()))
				.thenReturn(identity.getMetaData().get(0).getValue());
		Mockito.when(registrationStatusService.checkUinAvailabilityForRid(any())).thenReturn(true);
		Mockito.when(restClientService.getApi(any(), any(), any(), any(), any())).thenReturn(userResponseDto)
				.thenReturn(userResponseDto);
		registrationStatusDto.setRegistrationType("ACTIVATED");
		boolean isValid = osiValidator.isValidOSI("reg1234", registrationStatusDto);
		assertFalse(isValid);
	}

	@Test
	public void testinvalidUserInput() throws Exception {
		ServerError error = new ServerError();
		error.setMessage("Invalid Date format");
		List<ServerError> errors = new ArrayList<>();
		errors.add(error);
		userResponseDto.setErrors(errors);
		Mockito.when(osiUtils.getMetaDataValue(anyString(), any()))
				.thenReturn(identity.getMetaData().get(0).getValue());
		Mockito.when(registrationStatusService.checkUinAvailabilityForRid(any())).thenReturn(true);
		Mockito.when(restClientService.getApi(any(), any(), any(), any(), any())).thenReturn(userResponseDto)
				.thenReturn(userResponseDto);
		registrationStatusDto.setRegistrationType("ACTIVATED");
		boolean isValid = osiValidator.isValidOSI("reg1234", registrationStatusDto);
		assertFalse(isValid);
	}

	@Test
	public void testoperatorBiometricaAuthenticationFailure() throws Exception {
		io.mosip.registration.processor.core.auth.dto.ResponseDTO responseDTO = new io.mosip.registration.processor.core.auth.dto.ResponseDTO();
		responseDTO.setAuthStatus(false);
		authResponseDTO.setResponse(responseDTO);
		ErrorDTO errorDTO = new ErrorDTO();
		errorDTO.setErrorCode("IDA-MLC-008");
		errorDTO.setErrorMessage("authentication failed");
		List<ErrorDTO> errors = new ArrayList<>();
		errors.add(errorDTO);
		authResponseDTO.setErrors(errors);
		Mockito.when(restClientService.postApi(any(), anyString(), anyString(), anyString(), any()))
				.thenReturn(authResponseDTO);
		Mockito.when(osiUtils.getMetaDataValue(anyString(), any()))
				.thenReturn(identity.getMetaData().get(0).getValue());
		Mockito.when(registrationStatusService.checkUinAvailabilityForRid(any())).thenReturn(true);
		registrationStatusDto.setRegistrationType("ACTIVATED");
		boolean isValid = osiValidator.isValidOSI("reg1234", registrationStatusDto);
		assertFalse(isValid);
	}

	@Test(expected = ApisResourceAccessException.class)
	public void tesApisResourceAccessException() throws Exception {
		ApisResourceAccessException apisResourceAccessException = new ApisResourceAccessException("bad request");
		Mockito.when(restClientService.getApi(any(), any(), any(), any(), any())).thenReturn(userResponseDto)
				.thenThrow(apisResourceAccessException);
		Mockito.when(osiUtils.getMetaDataValue(anyString(), any()))
				.thenReturn(identity.getMetaData().get(0).getValue());
		Mockito.when(registrationStatusService.checkUinAvailabilityForRid(any())).thenReturn(true);
		registrationStatusDto.setRegistrationType("ACTIVATED");
		osiValidator.isValidOSI("reg1234", registrationStatusDto);

	}

	@Test(expected = ApisResourceAccessException.class)
	public void testHttpClientErrorException() throws Exception {
		HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
				"error");
		ApisResourceAccessException apisResourceAccessException = new ApisResourceAccessException("bad request",
				httpClientErrorException);
		Mockito.when(restClientService.getApi(any(), any(), any(), any(), any())).thenReturn(userResponseDto)
				.thenThrow(apisResourceAccessException);
		Mockito.when(osiUtils.getMetaDataValue(anyString(), any()))
				.thenReturn(identity.getMetaData().get(0).getValue());
		Mockito.when(registrationStatusService.checkUinAvailabilityForRid(any())).thenReturn(true);
		registrationStatusDto.setRegistrationType("ACTIVATED");
		osiValidator.isValidOSI("reg1234", registrationStatusDto);

	}

	@Test(expected = ApisResourceAccessException.class)
	public void testHttpServerErrorException() throws Exception {
		HttpServerErrorException httpServerErrorException = new HttpServerErrorException(HttpStatus.BAD_REQUEST,
				"error");
		ApisResourceAccessException apisResourceAccessException = new ApisResourceAccessException("bad request",
				httpServerErrorException);
		Mockito.when(restClientService.getApi(any(), any(), any(), any(), any())).thenReturn(userResponseDto)
				.thenThrow(apisResourceAccessException);
		Mockito.when(osiUtils.getMetaDataValue(anyString(), any()))
				.thenReturn(identity.getMetaData().get(0).getValue());
		Mockito.when(registrationStatusService.checkUinAvailabilityForRid(any())).thenReturn(true);
		registrationStatusDto.setRegistrationType("ACTIVATED");
		osiValidator.isValidOSI("reg1234", registrationStatusDto);

	}

	/**
	 * Test officer details null.
	 *
	 * @throws Exception the exception
	 */
	@Test
	public void testOfficerDetailsNull() throws Exception {
		regOsiDto.setOfficerId(null);
		regOsiDto.setSupervisorId(null);
		Mockito.when(osiUtils.getOSIDetailsFromMetaInfo(anyString(), any())).thenReturn(regOsiDto);
		boolean isValid = osiValidator.isValidOSI("reg1234", registrationStatusDto);
		assertFalse(isValid);
	}

	/**
	 * Test introducer details null.
	 *
	 * @throws Exception the exception
	 */
	@Test
	public void testIntroducerDetailsNull() throws Exception {
		boolean isValid = osiValidator.isValidOSI("reg1234", registrationStatusDto);
		assertFalse(isValid);
	}

	/**
	 * Test invalid iris.
	 * 
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws BioTypeException
	 * @throws BiometricException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 * @throws NumberFormatException
	 * @throws ApisResourceAccessException  the apis resource access exception
	 * @throws IOException                  Signals that an I/O exception has
	 *                                      occurred.
	 */
	@Test(expected = ParentOnHoldException.class)
	public void testIntroducerRIDFailedOnHold() throws ApisResourceAccessException, IOException, Exception {
		Mockito.when(osiUtils.getMetaDataValue(anyString(), any())).thenReturn("2015/01/01");
		registrationStatusDto.setStatusCode("FAILED");
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.when(utility.getDemographicIdentityJSONObject(Mockito.anyString(), Mockito.anyString())).thenReturn(
				JsonUtil.getJSONObject(JsonUtil.objectMapperReadValue(childMappingJson, JSONObject.class), "identity"));
		osiValidator.isValidOSI("reg1234", registrationStatusDto);
	}

	@Test(expected = ParentOnHoldException.class)
	public void testIntroducerRIDProcessingOnHold() throws NumberFormatException, ApisResourceAccessException,
			InvalidKeySpecException, NoSuchAlgorithmException, BiometricException, BioTypeException, IOException,
			ParserConfigurationException, SAXException, PacketDecryptionFailureException,
			io.mosip.kernel.core.exception.IOException, ParentOnHoldException, AuthSystemException,
			RegistrationProcessorCheckedException,
			io.mosip.registration.processor.packet.utility.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
		Mockito.when(osiUtils.getMetaDataValue(anyString(), any())).thenReturn("2015/01/01");
		InternalRegistrationStatusDto introducerRegistrationStatusDto = new InternalRegistrationStatusDto();

		introducerRegistrationStatusDto.setStatusCode((RegistrationStatusCode.PROCESSING.toString()));
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString()))
				.thenReturn(introducerRegistrationStatusDto);
		Mockito.when(utility.getDemographicIdentityJSONObject(Mockito.anyString(), Mockito.anyString())).thenReturn(
				JsonUtil.getJSONObject(JsonUtil.objectMapperReadValue(childMappingJson, JSONObject.class), "identity"));
		osiValidator.isValidOSI("reg1234", registrationStatusDto);
	}

	@Test(expected = ParentOnHoldException.class)
	public void testIntroducerNotInRegProc() throws ApisResourceAccessException, IOException, Exception {
		Mockito.when(osiUtils.getMetaDataValue(anyString(), any())).thenReturn("2015/01/01");
		InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setStatusCode(RegistrationStatusCode.REJECTED.toString());
		registrationStatusDto.setRegistrationType("NEW");
		Mockito.when(utility.getDemographicIdentityJSONObject(Mockito.anyString(), Mockito.anyString())).thenReturn(
				JsonUtil.getJSONObject(JsonUtil.objectMapperReadValue(childMappingJson, JSONObject.class), "identity"));
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(null);
		osiValidator.isValidOSI("reg1234", registrationStatusDto);
	}

	@Test(expected = ParentOnHoldException.class)
	public void testIntroducerUINAndRIDNull() throws Exception {
		Mockito.when(osiUtils.getMetaDataValue(anyString(), any())).thenReturn("2015/01/01");
		osiValidator.isValidOSI("reg1234", registrationStatusDto);

	}

	@Test(expected = ParentOnHoldException.class)
	public void testIntroducerBioFileNull() throws Exception {
		Mockito.when(osiUtils.getMetaDataValue(anyString(), any())).thenReturn("2015/01/01");
		osiValidator.isValidOSI("reg1234", registrationStatusDto);
	}

	@Test
	public void testIntroducerBioFileNotNull() throws Exception {
		Mockito.when(osiUtils.getMetaDataValue(anyString(), any())).thenReturn("2015/01/01");
		demoJson.put("value", "biometreics");
		authResponseDTO.setErrors(null);
		io.mosip.registration.processor.core.auth.dto.ResponseDTO responseDTO = new io.mosip.registration.processor.core.auth.dto.ResponseDTO();
		responseDTO.setAuthStatus(true);
		Mockito.when(authUtil.authByIdAuthentication(anyString(), any(), any())).thenReturn(authResponseDTO);
		Mockito.when(utility.getDemographicIdentityJSONObject(Mockito.anyString(), Mockito.anyString())).thenReturn(
				JsonUtil.getJSONObject(JsonUtil.objectMapperReadValue(childMappingJson, JSONObject.class), "identity"));
		registrationStatusDto.setStatusCode("PROCESSED");
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.when(idRepoService.getUinByRid(any(), any())).thenReturn(123456789);
		boolean isValid = osiValidator.isValidOSI("reg1234", registrationStatusDto);

		assertTrue(isValid);
	}

	@Test
	public void testIntroducerErrorTrue() throws Exception {
		Mockito.when(osiUtils.getMetaDataValue(anyString(), any())).thenReturn("2015/01/01");
		demoJson.put("value", "biometreics");
		ErrorDTO errordto = new ErrorDTO();
		errordto.setErrorCode("true");
		List errorDtoList = new ArrayList<>();
		errorDtoList.add(errordto);
		authResponseDTO.setErrors(errorDtoList);
		io.mosip.registration.processor.core.auth.dto.ResponseDTO responseDTO = new io.mosip.registration.processor.core.auth.dto.ResponseDTO();
		responseDTO.setAuthStatus(true);
		Mockito.when(authUtil.authByIdAuthentication(anyString(), any(), any())).thenReturn(authResponseDTO);
		boolean isValid = osiValidator.isValidOSI("reg1234", registrationStatusDto);
		assertFalse(isValid);
	}

	@Test
	public void testIntroducerAuthFalse() throws Exception {
		registrationStatusDto.setStatusCode("PROCESSED");
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.when(osiUtils.getMetaDataValue(anyString(), any())).thenReturn("2015/01/01");
		Mockito.when(utility.getDemographicIdentityJSONObject(Mockito.anyString(), Mockito.anyString())).thenReturn(
				JsonUtil.getJSONObject(JsonUtil.objectMapperReadValue(childMappingJson, JSONObject.class), "identity"));
		AuthResponseDTO authResponseDTO1 = new AuthResponseDTO();
		authResponseDTO1.setErrors(null);
		io.mosip.registration.processor.core.auth.dto.ResponseDTO responseDTO = new io.mosip.registration.processor.core.auth.dto.ResponseDTO();
		responseDTO.setAuthStatus(false);
		authResponseDTO1.setResponse(responseDTO);
		Mockito.when(authUtil.authByIdAuthentication(anyString(), any(), any())).thenReturn(authResponseDTO)
				.thenReturn(authResponseDTO1);
		Mockito.when(idRepoService.getUinByRid(any(), any())).thenReturn(123456789);
		boolean isValid = osiValidator.isValidOSI("reg1234", registrationStatusDto);

		assertFalse(isValid);
	}

	@Test(expected = ParentOnHoldException.class)
	public void testIntroducerUINNull() throws ApisResourceAccessException, IOException, Exception {
		Mockito.when(osiUtils.getMetaDataValue(anyString(), any())).thenReturn("2015/01/01");
		InternalRegistrationStatusDto introducerRegistrationStatusDto = new InternalRegistrationStatusDto();
		introducerRegistrationStatusDto.setStatusCode((RegistrationStatusCode.PROCESSED.toString()));
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString()))
				.thenReturn(introducerRegistrationStatusDto);
		Mockito.when(utility.getDemographicIdentityJSONObject(Mockito.anyString(), Mockito.anyString())).thenReturn(
				JsonUtil.getJSONObject(JsonUtil.objectMapperReadValue(childMappingJson, JSONObject.class), "identity"));
		Mockito.when(idRepoService.getUinByRid(any(), any())).thenReturn(null);
		osiValidator.isValidOSI("reg1234", registrationStatusDto);
	}
	@Test(expected=AuthSystemException.class)
	public void testoperatorAuthSystemException() throws Exception {
		io.mosip.registration.processor.core.auth.dto.ResponseDTO responseDTO = new io.mosip.registration.processor.core.auth.dto.ResponseDTO();
		responseDTO.setAuthStatus(false);
		authResponseDTO.setResponse(responseDTO);
		ErrorDTO errorDTO = new ErrorDTO();
		errorDTO.setErrorCode("IDA-MLC-007");
		errorDTO.setErrorMessage("system exception");
		List<ErrorDTO> errors = new ArrayList<>();
		errors.add(errorDTO);
		authResponseDTO.setErrors(errors);
		Mockito.when(restClientService.postApi(any(), anyString(), anyString(), anyString(), any()))
				.thenReturn(authResponseDTO);
		Mockito.when(osiUtils.getMetaDataValue(anyString(), any()))
				.thenReturn(identity.getMetaData().get(0).getValue());
		Mockito.when(registrationStatusService.checkUinAvailabilityForRid(any())).thenReturn(true);
		registrationStatusDto.setRegistrationType("ACTIVATED");
        osiValidator.isValidOSI("reg1234", registrationStatusDto);
		
	}
}
