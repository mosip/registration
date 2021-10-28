package io.mosip.registration.processor.introducer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Lists;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
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
import org.xml.sax.SAXException;

import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.QualityType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.registration.processor.core.auth.dto.AuthResponseDTO;
import io.mosip.registration.processor.core.auth.dto.ErrorDTO;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.AuthSystemException;
import io.mosip.registration.processor.core.exception.BioTypeException;
import io.mosip.registration.processor.core.exception.IntroducerOnHoldException;
import io.mosip.registration.processor.core.idrepo.dto.IdResponseDTO;
import io.mosip.registration.processor.core.idrepo.dto.ResponseDTO;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.packet.dto.FieldValue;
import io.mosip.registration.processor.core.packet.dto.FieldValueArray;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.RIDResponseDto;
import io.mosip.registration.processor.core.packet.dto.RidDto;
import io.mosip.registration.processor.core.packet.dto.demographicinfo.DemographicInfoDto;
import io.mosip.registration.processor.core.packet.dto.masterdata.UserDetailsDto;
import io.mosip.registration.processor.core.packet.dto.masterdata.UserDetailsResponseDto;
import io.mosip.registration.processor.core.packet.dto.masterdata.UserResponseDto;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.storage.utils.ABISHandlerUtil;
import io.mosip.registration.processor.packet.storage.utils.AuthUtil;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.stages.introducervalidator.IntroducerValidator;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.TransactionDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.TransactionService;

/**
 * The Class IntroducerValidatorTest.
 *
 * @author M1022006
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Utilities.class, IOUtils.class })
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "javax.net.ssl.*" })
public class IntroducerValidatorTest {

	/** The input stream. */
	@Mock
	private InputStream inputStream;

	/** The registration status service. */
	@Mock
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	@Mock
	private IdRepoService idRepoService;

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

	/** The data. */
	byte[] data = "1234567890".getBytes();

	/** The registration status dto. */
	InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();

	/** The transaction dto. */
	TransactionDto transactionDto = new TransactionDto();

	/** The supervisor validator. */
	@InjectMocks
	IntroducerValidator introducerValidator;

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
	private PriorityBasedPacketManagerService packetManagerService;

	/**
	 * Sets the up.
	 *
	 * @throws Exception the exception
	 */
	@Before
	public void setUp() throws Exception {
		classLoader = getClass().getClassLoader();

		Mockito.when(utility.isUinMissingFromIdAuth(any(),any(),any())).thenReturn(false);
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

		demographicDedupeDtoList.add(demographicInfoDto);

		Mockito.when(env.getProperty("mosip.kernel.applicant.type.age.limit")).thenReturn("5");
		PowerMockito.mockStatic(IOUtils.class);
		PowerMockito.when(IOUtils.class, "toByteArray", inputStream).thenReturn(data);

		Mockito.when(authUtil.authByIdAuthentication(anyString(), any(), any())).thenReturn(authResponseDTO);

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

		//File cbeffFile = new File(classLoader.getResource("cbeff.xml").getFile());

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

		BiometricRecord biometricRecord = new BiometricRecord();
		biometricRecord.setSegments(Lists.newArrayList(birType3, birType4));
		when(packetManagerService.getBiometricsByMappingJsonKey(anyString(), any(), any(), any()))
				.thenReturn(biometricRecord);

		when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), anyString(), any()))
				.thenReturn("field");
	}

	/**
	 * Test introducer details null.
	 *
	 * @throws Exception the exception
	 */
	@Test
	@Ignore
	public void testIntroducerDetailsNull() throws Exception {
		introducerValidator.validate("reg1234", registrationStatusDto);
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
	@Test(expected = BaseCheckedException.class)
	public void testIntroducerRIDFailedOnHold() throws ApisResourceAccessException, IOException, Exception {
		registrationStatusDto.setStatusCode("FAILED");
		List<InternalRegistrationStatusDto> internalRegistrationStatusDtoList=new ArrayList<InternalRegistrationStatusDto>();
		internalRegistrationStatusDtoList.add(registrationStatusDto);

		Mockito.when(registrationStatusService.getAllRegistrationStatuses(anyString())).thenReturn(internalRegistrationStatusDtoList);
		when(packetManagerService.getFieldByMappingJsonKey("reg1234", "introducerUIN", "New",
				ProviderStageName.INTRODUCER_VALIDATOR)).thenReturn(null);
		when(packetManagerService.getFieldByMappingJsonKey("reg1234", "introducerRID", "New",
				ProviderStageName.INTRODUCER_VALIDATOR)).thenReturn("field");
		introducerValidator.validate("reg1234", registrationStatusDto);
	}
	
	@Test(expected = BaseCheckedException.class)
	public void testIntroducerBiometricValidationFailed() throws ApisResourceAccessException, IOException, Exception {

		when(packetManagerService.getFieldByMappingJsonKey("reg1234", "introducerUIN", "New",
				ProviderStageName.INTRODUCER_VALIDATOR)).thenReturn("2832677");
		when(packetManagerService.getFieldByMappingJsonKey("reg1234", "introducerRID", "New",
				ProviderStageName.INTRODUCER_VALIDATOR)).thenReturn("field");
		when(packetManagerService.getBiometricsByMappingJsonKey(anyString(), any(), any(), any()))
		.thenReturn(null);
		introducerValidator.validate("reg1234", registrationStatusDto);
	}
	
	@Test(expected = BaseCheckedException.class)
	public void testIntroducerRIDUINNotFound() throws ApisResourceAccessException, IOException, Exception {
		registrationStatusDto.setStatusCode("PROCESSED");
		List<InternalRegistrationStatusDto> internalRegistrationStatusDtoList=new ArrayList<InternalRegistrationStatusDto>();
		internalRegistrationStatusDtoList.add(registrationStatusDto);

		Mockito.when(registrationStatusService.getAllRegistrationStatuses(anyString())).thenReturn(internalRegistrationStatusDtoList);
		when(packetManagerService.getFieldByMappingJsonKey("reg1234", "introducerUIN", "New",
				ProviderStageName.INTRODUCER_VALIDATOR)).thenReturn(null);
		when(packetManagerService.getFieldByMappingJsonKey("reg1234", "introducerRID", "New",
				ProviderStageName.INTRODUCER_VALIDATOR)).thenReturn("field");
		introducerValidator.validate("reg1234", registrationStatusDto);
	}

	@Test(expected = IntroducerOnHoldException.class)
	public void testIntroducerRIDProcessingOnHold()
			throws NumberFormatException, InvalidKeySpecException, NoSuchAlgorithmException, IOException,
			ParserConfigurationException, SAXException, JSONException, CertificateException, BaseCheckedException {
		registrationStatusDto.setStatusCode((RegistrationStatusCode.PROCESSING.toString()));
		List<InternalRegistrationStatusDto> internalRegistrationStatusDtoList=new ArrayList<InternalRegistrationStatusDto>();
		internalRegistrationStatusDtoList.add(registrationStatusDto);
		Mockito.when(registrationStatusService.getAllRegistrationStatuses(anyString())).thenReturn(internalRegistrationStatusDtoList);

		when(packetManagerService.getFieldByMappingJsonKey("reg1234", "introducerUIN", "New",
				ProviderStageName.INTRODUCER_VALIDATOR)).thenReturn(null);
		when(packetManagerService.getFieldByMappingJsonKey("reg1234", "introducerRID", "New",
				ProviderStageName.INTRODUCER_VALIDATOR)).thenReturn("field");

		introducerValidator.validate("reg1234", registrationStatusDto);
	}

	@Test(expected = BaseCheckedException.class)
	public void testIntroducerUINAndRIDNotPresent() throws Exception {
		when(packetManagerService.getFieldByMappingJsonKey("reg1234", "introducerUIN", "New",
				ProviderStageName.INTRODUCER_VALIDATOR)).thenReturn("");
		when(packetManagerService.getFieldByMappingJsonKey("reg1234", "introducerRID", "New",
				ProviderStageName.INTRODUCER_VALIDATOR)).thenReturn("");

		introducerValidator.validate("reg1234", registrationStatusDto);
	}

	@Test(expected = IntroducerOnHoldException.class)
	@Ignore
	public void testIntroducerNotInRegProc() throws Exception {
		InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setStatusCode(RegistrationStatusCode.REJECTED.toString());
		registrationStatusDto.setRegistrationType("NEW");
		when(packetManagerService.getFieldByMappingJsonKey("reg1234", "introducerUIN", "New",
				ProviderStageName.INTRODUCER_VALIDATOR)).thenReturn(null);
		when(packetManagerService.getFieldByMappingJsonKey("reg1234", "introducerRID", "New",
				ProviderStageName.INTRODUCER_VALIDATOR)).thenReturn("field");

		Mockito.when(registrationStatusService.getAllRegistrationStatuses(anyString())).thenReturn(null);
		introducerValidator.validate("reg1234", registrationStatusDto);
	}

	@Test
	public void testIntroducerBioFileNotNull() throws Exception {
		demoJson.put("value", "biometreics");
		
		AuthResponseDTO authResponseDTO1 = new AuthResponseDTO();
		authResponseDTO1.setErrors(null);
		io.mosip.registration.processor.core.auth.dto.ResponseDTO responseDTO = new io.mosip.registration.processor.core.auth.dto.ResponseDTO();
		responseDTO.setAuthStatus(true);
		authResponseDTO1.setResponse(responseDTO);
		Mockito.when(authUtil.authByIdAuthentication(anyString(), any(), any())).thenReturn(authResponseDTO1);

		registrationStatusDto.setStatusCode("PROCESSED");
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any())).thenReturn(registrationStatusDto);
		Mockito.when(idRepoService.getUinByRid(any(), any())).thenReturn("123456789");
		introducerValidator.validate("reg1234", registrationStatusDto);
	}

	@Test(expected = BaseCheckedException.class)
	public void testIntroducerErrorTrue() throws Exception {
		// Mockito.when(osiUtils.getMetaDataValue(anyString(),
		// any())).thenReturn("2015/01/01");
		demoJson.put("value", "biometreics");
		ErrorDTO errordto = new ErrorDTO();
		errordto.setErrorCode("true");
		List errorDtoList = new ArrayList<>();
		errorDtoList.add(errordto);
		authResponseDTO.setErrors(errorDtoList);
		io.mosip.registration.processor.core.auth.dto.ResponseDTO responseDTO = new io.mosip.registration.processor.core.auth.dto.ResponseDTO();
		responseDTO.setAuthStatus(true);
		Mockito.when(authUtil.authByIdAuthentication(anyString(), any(), any())).thenReturn(authResponseDTO);
		introducerValidator.validate("reg1234", registrationStatusDto);
	}
	
	@Test(expected = AuthSystemException.class)
	public void testIntroducerAuthSystemError() throws Exception {
		demoJson.put("value", "biometreics");
		ErrorDTO errordto = new ErrorDTO();
		errordto.setErrorCode("IDA-MLC-007");
		authResponseDTO.setErrors(Arrays.asList(errordto));
		Mockito.when(authUtil.authByIdAuthentication(anyString(), any(), any())).thenReturn(authResponseDTO);
		introducerValidator.validate("reg1234", registrationStatusDto);
	}

	@Test(expected = BaseCheckedException.class)
	public void testIntroducerAuthFalse() throws Exception {
		registrationStatusDto.setStatusCode("PROCESSED");
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any())).thenReturn(registrationStatusDto);
		/*
		 * Mockito.when(osiUtils.getMetaDataValue(anyString(),
		 * any())).thenReturn("2015/01/01");
		 * Mockito.when(utility.getDemographicIdentityJSONObject(Mockito.anyString(),
		 * Mockito.anyString())).thenReturn(
		 * JsonUtil.getJSONObject(JsonUtil.objectMapperReadValue(childMappingJson,
		 * JSONObject.class), "identity"));
		 */
		AuthResponseDTO authResponseDTO1 = new AuthResponseDTO();
		authResponseDTO1.setErrors(null);
		io.mosip.registration.processor.core.auth.dto.ResponseDTO responseDTO = new io.mosip.registration.processor.core.auth.dto.ResponseDTO();
		responseDTO.setAuthStatus(false);
		authResponseDTO1.setResponse(responseDTO);
		Mockito.when(authUtil.authByIdAuthentication(anyString(), any(), any())).thenReturn(authResponseDTO1);
		Mockito.when(idRepoService.getUinByRid(any(), any())).thenReturn("123456789");
		introducerValidator.validate("reg1234", registrationStatusDto);
	}

	@Test(expected = BaseCheckedException.class)
	public void testIntroducerUINNull() throws Exception {
		InternalRegistrationStatusDto introducerRegistrationStatusDto = new InternalRegistrationStatusDto();
		introducerRegistrationStatusDto.setStatusCode((RegistrationStatusCode.PROCESSED.toString()));
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any()))
				.thenReturn(introducerRegistrationStatusDto);
		when(packetManagerService.getFieldByMappingJsonKey("reg1234", "introducerUIN", "New",
				ProviderStageName.INTRODUCER_VALIDATOR)).thenReturn(null);
		when(packetManagerService.getFieldByMappingJsonKey("reg1234", "introducerRID", "New",
				ProviderStageName.INTRODUCER_VALIDATOR)).thenReturn("field");
		Mockito.when(idRepoService.getUinByRid(any(), any())).thenReturn(null);

		introducerValidator.validate("reg1234", registrationStatusDto);
	}
}