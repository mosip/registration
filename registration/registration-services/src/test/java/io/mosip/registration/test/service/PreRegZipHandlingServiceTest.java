package io.mosip.registration.test.service;

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.mosip.kernel.core.exception.IOException;
import io.mosip.kernel.core.security.constants.MosipSecurityMethod;
import io.mosip.kernel.core.util.FileUtils;
import io.mosip.kernel.keygenerator.bouncycastle.KeyGenerator;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.DocumentTypeDAO;
import io.mosip.registration.dto.OSIDataDTO;
import io.mosip.registration.dto.PreRegistrationDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.RegistrationMetaDataDTO;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.biometric.BiometricDTO;
import io.mosip.registration.dto.biometric.BiometricInfoDTO;
import io.mosip.registration.dto.demographic.ApplicantDocumentDTO;
import io.mosip.registration.dto.demographic.DemographicDTO;
import io.mosip.registration.dto.demographic.DemographicInfoDTO;
import io.mosip.registration.dto.demographic.IndividualIdentity;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.service.IdentitySchemaService;
import io.mosip.registration.service.external.impl.PreRegZipHandlingServiceImpl;
import io.mosip.registration.validator.RegIdObjectValidator;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ FileUtils.class, SessionContext.class })
public class PreRegZipHandlingServiceTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@Mock
	private KeyGenerator keyGenerator;

	@Mock
	private RegIdObjectValidator idObjectValidator;

	@Mock
	private DocumentTypeDAO documentTypeDAO;

	@InjectMocks
	private PreRegZipHandlingServiceImpl preRegZipHandlingServiceImpl;

	static byte[] preRegPacket;

	static byte[] preRegPacketEncrypted;

	static MosipSecurityMethod mosipSecurityMethod;
	
	@Mock
	private IdentitySchemaService identitySchemaService;
	
	List<UiSchemaDTO> schemaFields = new ArrayList<UiSchemaDTO>();

	@Before
	public void init() throws Exception {
		createRegistrationDTOObject();
		
		UiSchemaDTO field1 = new UiSchemaDTO();
		field1.setId("fullName");
		field1.setType("simpleType");
		schemaFields.add(field1);
		
		UiSchemaDTO field2 = new UiSchemaDTO();
		field2.setId("gender");
		field2.setType("simpleType");
		schemaFields.add(field2);		
		
		UiSchemaDTO field3 = new UiSchemaDTO();
		field3.setId("postalCode");
		field3.setType("string");
		schemaFields.add(field3);
	}
	
	
	@BeforeClass
	public static void initialize() throws IOException, java.io.IOException {
		URL url = PreRegZipHandlingServiceTest.class.getResource("/preRegSample.zip");
		File packetZipFile = new File(URLDecoder.decode(url.getFile(), "UTF-8"));
		preRegPacket = FileUtils.readFileToByteArray(packetZipFile);

		mosipSecurityMethod = MosipSecurityMethod.AES_WITH_CBC_AND_PKCS7PADDING;

		Map<String, Object> applicationMap = new HashMap<>();
		applicationMap.put("mosip.registration.registration_pre_reg_packet_location", "..//PreRegPacketStore");
		ApplicationContext.getInstance();
		ApplicationContext.setApplicationMap(applicationMap);
	}

	@Test
	public void extractPreRegZipFileTest() throws Exception {
		Mockito.doAnswer((idObject) -> {
			return "Success";
		}).when(idObjectValidator).validateIdObject(Mockito.any(), Mockito.any());
		Mockito.when(documentTypeDAO.getDocTypeByName(Mockito.anyString())).thenReturn(new ArrayList<>());
		Mockito.when(identitySchemaService.getLatestEffectiveUISchema()).thenReturn(schemaFields);
		
		RegistrationDTO registrationDTO = preRegZipHandlingServiceImpl.extractPreRegZipFile(preRegPacket);

		assertNotNull(registrationDTO);
	}

	@Test(expected = RegBaseCheckedException.class)
	public void extractPreRegZipFileTestNegative() throws Exception {
		try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
			ZipEntry zipEntry = new ZipEntry("id.json");
			zipOutputStream.putNextEntry(zipEntry);
			zipOutputStream.write(
					"\"identity\" : {    \"CNIENumber\" : 6789545678909,    \"gender\" : [ {      \"language\" : \"eng\",      \"value\" : \"male\"    }, {      \"language\" : \"ara\",      \"value\" : \"male\"    } ],    \"city\" : [ {      \"language\" : \"eng\",      \"value\" : \"Bangalore\"    }, {      \"language\" : \"ara\",      \"value\" : \"BLR\"    } ],    \"postalCode\" : \"570000\",    \"localAdministrativeAuthority\" : [ {      \"language\" : \"eng\",      \"value\" : \"Bangalore\"    }, {      \"language\" : \"ara\",      \"value\" : \"BLR\"    } ]"
							.getBytes());
			zipOutputStream.flush();
			zipOutputStream.closeEntry();

			Mockito.doAnswer((idObject) -> {
				return "Success";
			}).when(idObjectValidator).validateIdObject(Mockito.any(), Mockito.any());
			Mockito.when(documentTypeDAO.getDocTypeByName(Mockito.anyString())).thenReturn(new ArrayList<>());
			Mockito.when(identitySchemaService.getLatestEffectiveUISchema()).thenReturn(schemaFields);
			preRegZipHandlingServiceImpl.extractPreRegZipFile(byteArrayOutputStream.toByteArray());
		}
	}

	@Test
	public void encryptAndSavePreRegPacketTest() throws RegBaseCheckedException, IOException {

		PreRegistrationDTO preRegistrationDTO = encryptPacket();
		assertNotNull(preRegistrationDTO);
	}

	private PreRegistrationDTO encryptPacket() throws RegBaseCheckedException, IOException {

		mockSecretKey();

		PreRegistrationDTO preRegistrationDTO = preRegZipHandlingServiceImpl
				.encryptAndSavePreRegPacket("89149679063970", preRegPacket);
		return preRegistrationDTO;
	}

	@Test(expected = RegBaseCheckedException.class)
	public void encryptAndSavePreRegPacketIoExceptionTest() throws RegBaseCheckedException, IOException {
		mockExceptions();
		encryptPacket();
	}

	@Test(expected = RegBaseUncheckedException.class)
	public void encryptAndSavePreRegPacketRuntimeExceptionTest() throws RegBaseCheckedException, IOException {
		mockRuntimeExceptions();
		encryptPacket();
	}
	
	protected void mockExceptions() throws IOException {
		PowerMockito.mockStatic(FileUtils.class);
		PowerMockito.doThrow(new io.mosip.kernel.core.exception.IOException("", "")).when(FileUtils.class);
		FileUtils.copyToFile(Mockito.any(), Mockito.any());
	}
	
	protected void mockRuntimeExceptions() throws IOException {
		PowerMockito.mockStatic(FileUtils.class);
		PowerMockito.doThrow(new RuntimeException()).when(FileUtils.class);
		FileUtils.copyToFile(Mockito.any(), Mockito.any());
	}

	@Test
	public void decryptPreRegPacketTest() throws RegBaseCheckedException, IOException {

		final byte[] decrypted = preRegZipHandlingServiceImpl.decryptPreRegPacket("0E8BAAEB3CED73CBC9BF4964F321824A",
				encryptPacket().getEncryptedPacket());
		assertNotNull(decrypted);
	}
	//
	// @Test(expected = RegBaseCheckedException.class)
	// public void extractPreRegZipFileTestNegative() throws RegBaseCheckedException
	// {
	// byte[] packetValue = "sampleTestForNegativeCase".getBytes();
	// preRegZipHandlingServiceImpl.extractPreRegZipFile(packetValue);
	//
	// }

	// @Test(expected = RegBaseUncheckedException.class)
	public void encryptAndSavePreRegPacketTestNegative() throws RegBaseCheckedException {
		mockSecretKey();
		preRegZipHandlingServiceImpl.encryptAndSavePreRegPacket("89149679063970", preRegPacket);
	}

	@Test(expected = RegBaseUncheckedException.class)
	public void extractPreRegZipFileNegative() throws RegBaseCheckedException {
		mockSecretKey();
		preRegZipHandlingServiceImpl.extractPreRegZipFile(null);
	}

	private void mockSecretKey() {
		byte[] decodedKey = Base64.getDecoder().decode("0E8BAAEB3CED73CBC9BF4964F321824A");
		SecretKey originalKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
		Mockito.when(keyGenerator.getSymmetricKey()).thenReturn(originalKey);
	}

	private static void createRegistrationDTOObject() {
		RegistrationDTO registrationDTO = new RegistrationDTO();

		// Set the RID
		registrationDTO.setRegistrationId("10011100110016320190307151917");

		// Create objects for Biometric DTOS
		BiometricDTO biometricDTO = new BiometricDTO();		
		registrationDTO.setBiometricDTO(biometricDTO);		

		// Create object for OSIData DTO
		registrationDTO.setOsiDataDTO(new OSIDataDTO());

		// Create object for RegistrationMetaData DTO
		RegistrationMetaDataDTO registrationMetaDataDTO = new RegistrationMetaDataDTO();
		registrationMetaDataDTO.setRegistrationCategory("New");
		registrationDTO.setRegistrationMetaDataDTO(registrationMetaDataDTO);

		// Put the RegistrationDTO object to SessionContext Map
		PowerMockito.mockStatic(SessionContext.class);
		Map<String,Object> regMap = new LinkedHashMap<>();
		regMap.put(RegistrationConstants.REGISTRATION_DATA, registrationDTO);
		PowerMockito.when(SessionContext.map()).thenReturn(regMap);
	}

	private static BiometricInfoDTO createBiometricInfoDTO() {
		BiometricInfoDTO biometricInfoDTO = new BiometricInfoDTO();
		biometricInfoDTO.setBiometricExceptionDTO(new ArrayList<>());
		biometricInfoDTO.setFingerprintDetailsDTO(new ArrayList<>());
		biometricInfoDTO.setIrisDetailsDTO(new ArrayList<>());
		return biometricInfoDTO;
	}
}
