package io.mosip.registration.test.service;

import static org.junit.Assert.assertNotNull;

import java.net.SocketTimeoutException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import io.mosip.registration.service.BaseService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.web.client.HttpClientErrorException;

import io.mosip.kernel.core.crypto.spi.CryptoCoreSpec;
import io.mosip.kernel.keygenerator.bouncycastle.KeyGenerator;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.context.SessionContext.UserContext;
import io.mosip.registration.dao.UserOnboardDAO;
import io.mosip.registration.dto.PublicKeyResponse;
import io.mosip.registration.dto.biometric.BiometricDTO;
import io.mosip.registration.dto.biometric.BiometricInfoDTO;
import io.mosip.registration.dto.biometric.FaceDetailsDTO;
import io.mosip.registration.dto.biometric.FingerprintDetailsDTO;
import io.mosip.registration.dto.biometric.IrisDetailsDTO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.service.operator.impl.UserOnboardServiceImpl;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;


/**
 * @author Sreekar Chukka
 *
 * @since 1.0.0
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ UserOnBoardServiceImplTest.class, RegistrationSystemPropertiesChecker.class, ApplicationContext.class,
		RegistrationAppHealthCheckUtil.class, KeyGenerator.class, SecretKey.class, SessionContext.class })
public class UserOnBoardServiceImplTest {
	
	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	
	@Mock
	private RegistrationAppHealthCheckUtil registrationAppHealthCheckUtil;
	
	@Mock
	private RegistrationSystemPropertiesChecker registrationSystemPropertiesChecker;
	
	@InjectMocks
	private UserOnboardServiceImpl userOnboardServiceImpl;

	@InjectMocks
	private BaseService baseService;
	
	@Mock
	private UserOnboardDAO userOnBoardDao;
	
	@Mock
	private KeyGenerator keyGenerator;
	
	@Mock
	private ServiceDelegateUtil serviceDelegateUtil;
	
	@Mock
    private CryptoCoreSpec<byte[], byte[], SecretKey, PublicKey, PrivateKey, String> encryptor;

	
	@Mock
	io.mosip.registration.context.ApplicationContext context;
	
	@Before
	public void init() throws Exception {
		Map<String,Object> appMap = new HashMap<>();
		appMap.put(RegistrationConstants.USER_ON_BOARD_THRESHOLD_LIMIT, "10");		
		appMap.put("mosip.registration.fingerprint_disable_flag", "Y");
		appMap.put("mosip.registration.iris_disable_flag", "Y");
		appMap.put("mosip.registration.face_disable_flag", "Y");
		appMap.put("mosip.registration.onboarduser_ida_auth", "Y");
		ApplicationContext.getInstance().setApplicationMap(appMap);
		
		UserContext userContext = Mockito.mock(SessionContext.UserContext.class);
		PowerMockito.mockStatic(SessionContext.class);
		PowerMockito.doReturn(userContext).when(SessionContext.class, "userContext");
		PowerMockito.when(SessionContext.userContext().getUserId()).thenReturn("mosip");
	}
	
	/*@Test
	public void userOnBoard() throws HttpClientErrorException, SocketTimeoutException, RegBaseCheckedException {
		
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		PowerMockito.mockStatic(KeyGenerator.class);
		Mockito.when(RegistrationAppHealthCheckUtil.isNetworkAvailable()).thenReturn(true);
		
		BiometricDTO biometricDTO= new BiometricDTO();
		
		List<FingerprintDetailsDTO> listOfFingerPrints = new ArrayList<>();
		List<FingerprintDetailsDTO> listOfFingerSegmets = new ArrayList<>();

		FingerprintDetailsDTO fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftIndex".getBytes());
		fingerDto.setFingerprintImageName("leftIndex");
		fingerDto.setFingerPrintISOImage("leftIndex".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftLittle".getBytes());
		fingerDto.setFingerprintImageName("leftLittle");
		fingerDto.setFingerPrintISOImage("leftLittle".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftMiddle".getBytes());
		fingerDto.setFingerprintImageName("leftMiddle");
		fingerDto.setFingerPrintISOImage("leftMiddle".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftRing".getBytes());
		fingerDto.setFingerprintImageName("leftRing");
		fingerDto.setFingerPrintISOImage("leftRing".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftThumb".getBytes());
		fingerDto.setFingerprintImageName("leftThumb");
		fingerDto.setFingerPrintISOImage("leftThumb".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftFore".getBytes());
		fingerDto.setFingerprintImageName("leftFore");
		fingerDto.setFingerPrintISOImage("leftFore".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);


		List<IrisDetailsDTO> iriesList=new ArrayList<>();
		IrisDetailsDTO iries=new IrisDetailsDTO();
		
		iries.setIris("right".getBytes());
		iries.setIrisImageName("Right");
		iries.setIrisType("Eyes");
		iries.setNumOfIrisRetry(2);
		iries.setQualityScore(90);
		iriesList.add(iries);
		
		FaceDetailsDTO face=new FaceDetailsDTO();
		face.setFace("face".getBytes());
		face.setForceCaptured(false);
		face.setNumOfRetries(2);
		face.setQualityScore(90);
		
		BiometricInfoDTO info=new BiometricInfoDTO();
		info.setFingerprintDetailsDTO(listOfFingerPrints);
		info.setIrisDetailsDTO(iriesList);
		info.setFace(face);
		
		
		PublicKeyResponse<String> publicKeyResponse = new PublicKeyResponse<>();
		LinkedHashMap<String, Object> response = new LinkedHashMap<>();
		response.put("publicKey", "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAp7rM0m8Vp91T5IkuSPhwibGivMn46-Eg3sjwiNDd1M0frs6Vmf1A7Guo-tjgMAAWkEuk-fX9yJ4j9HxMBJmhrIM7bFFaF4htOh5A78qvTdgahvGccpC080m_pDllSIhJk3R5TExLMxC823gvm0cqJQh2xvttissag0fHXBeqo6SAvN7214WgN2zqPZiN2PbDWv8qICHTw8R2GGshc4pu-rh4atLWe7waLJFrFY00TpKLGTrR7TEH4GQ45LcQ7fRV4N63FN7A0ikM1jjLWoi78rn4PCLL5T91o3VBUau3V0WKdDQ9ZrIjcFdjaLS6gzmM9mI6BRIeMGVxOfnuops8FwIDAQAB");
		response.put("issuedAt", LocalDateTime.now());
		response.put("expiryAt", LocalDateTime.now());

		publicKeyResponse.setResponse(response);

		publicKeyResponse.setAlias("ALIAS");
		publicKeyResponse.setExpiryAt(LocalDateTime.now());
		publicKeyResponse.setIssuedAt(LocalDateTime.now());
		publicKeyResponse.setPublicKey("MY_PUBLIC_KEY");
		
		
		LinkedHashMap<String, Object> responseApi=new LinkedHashMap<>();
		LinkedHashMap<String, Object> responseMap=new LinkedHashMap<>();
		responseMap.put(RegistrationConstants.ON_BOARD_AUTH_STATUS, true);
		responseApi.put(RegistrationConstants.RESPONSE, responseMap);
		responseApi.put(RegistrationConstants.ERRORS, null);
		
		biometricDTO.setOperatorBiometricDTO(info);
		Map<String,Object> publicMapData=new LinkedHashMap<String,Object>();
		publicMapData.put("authStatus", true);
		publicMapData.put(RegistrationConstants.PUBLIC_KEY, "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAp7rM0m8Vp91T5IkuSPhwibGivMn46-Eg3sjwiNDd1M0frs6Vmf1A7Guo-tjgMAAWkEuk-fX9yJ4j9HxMBJmhrIM7bFFaF4htOh5A78qvTdgahvGccpC080m_pDllSIhJk3R5TExLMxC823gvm0cqJQh2xvttissag0fHXBeqo6SAvN7214WgN2zqPZiN2PbDWv8qICHTw8R2GGshc4pu-rh4atLWe7waLJFrFY00TpKLGTrR7TEH4GQ45LcQ7fRV4N63FN7A0ikM1jjLWoi78rn4PCLL5T91o3VBUau3V0WKdDQ9ZrIjcFdjaLS6gzmM9mI6BRIeMGVxOfnuops8FwIDAQAB");
		Map<String,Object> myMapPublic=new LinkedHashMap<String,Object>();
		myMapPublic.put("response", publicMapData);
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyBoolean(), Mockito.anyString())).thenReturn(myMapPublic);
		SecretKey mock = PowerMockito.mock(SecretKey.class);
		PowerMockito.when(keyGenerator.getSymmetricKey()).thenReturn(PowerMockito.mock(SecretKey.class));
		Mockito.when(encryptor.symmetricEncrypt(Mockito.any(), Mockito.any(),Mockito.eq(null))).thenReturn("test".getBytes());
		Mockito.when(encryptor.asymmetricEncrypt(Mockito.any(), Mockito.any())).thenReturn("test".getBytes());
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.any(), Mockito.anyString())).thenReturn(responseApi);
		Mockito.when(userOnBoardDao.insert(biometricDTO)).thenReturn(RegistrationConstants.SUCCESS);
		Mockito.when(userOnBoardDao.save()).thenReturn(RegistrationConstants.SUCCESS);	
		Map<String,Object> myMapData=new LinkedHashMap<String,Object>();
		myMapData.put("data", "IO7pawokCjgnjR-jo40DCbz2w0ELErUtCcDoKL6VTe644pqUPMq22MOXThNIXyLybBCO4LVSk3uaipYDnZr4ANxMsYr3WDp7KbQwcG9KnmVOm1mbzzCpkkJNRhbA8O2Ucj77KOXOLI3TszTKRUhBz2uLs46IvWiksuLsZwurYgkne62Gqr1QMh74PDPE9dKgvOC3o4hfPbP53H3bbc7HNR_Vu1kzn4xfyYFSNGATNVVXitZOlAKYgTcFdGde3eX2bos7OAivQad4TsmWeT0Pjmg1gixdwLq5agXhix3Pc-DvQgJzx-OnNUulAQTDkdbREx2TOJAu_ptF2JrrMuEflyNLRVlfU1BMSVRURVIjTsKlrgzfzXOXIb-NqJQWkz2OSMg");
		myMapData.put("authStatus", true);
		Map<String,Object> myMap=new LinkedHashMap<String,Object>();
		myMap.put("response", myMapData);
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyMap(),Mockito.anyString())).thenReturn(myMap);
		userOnboardServiceImpl.validateWithIDAuthAndSave(biometricDTO);
		
	}
	
	
	@Test
	public void userOnBoardFail() throws HttpClientErrorException, SocketTimeoutException, RegBaseCheckedException {
		
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		PowerMockito.mockStatic(KeyGenerator.class);
		Mockito.when(RegistrationAppHealthCheckUtil.isNetworkAvailable()).thenReturn(true);
		
		BiometricDTO biometricDTO= new BiometricDTO();
		
		List<FingerprintDetailsDTO> listOfFingerPrints = new ArrayList<>();
		List<FingerprintDetailsDTO> listOfFingerSegmets = new ArrayList<>();

		FingerprintDetailsDTO fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftIndex".getBytes());
		fingerDto.setFingerprintImageName("leftIndex");
		fingerDto.setFingerPrintISOImage("leftIndex".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftLittle".getBytes());
		fingerDto.setFingerprintImageName("leftLittle");
		fingerDto.setFingerPrintISOImage("leftLittle".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftMiddle".getBytes());
		fingerDto.setFingerprintImageName("leftMiddle");
		fingerDto.setFingerPrintISOImage("leftMiddle".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftRing".getBytes());
		fingerDto.setFingerprintImageName("leftRing");
		fingerDto.setFingerPrintISOImage("leftRing".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftThumb".getBytes());
		fingerDto.setFingerprintImageName("leftThumb");
		fingerDto.setFingerPrintISOImage("leftThumb".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftFore".getBytes());
		fingerDto.setFingerprintImageName("leftFore");
		fingerDto.setFingerPrintISOImage("leftFore".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);


		List<IrisDetailsDTO> iriesList=new ArrayList<>();
		IrisDetailsDTO iries=new IrisDetailsDTO();
		
		iries.setIris("right".getBytes());
		iries.setIrisImageName("Right");
		iries.setIrisType("Eyes");
		iries.setNumOfIrisRetry(2);
		iries.setQualityScore(90);
		iriesList.add(iries);
		
		FaceDetailsDTO face=new FaceDetailsDTO();
		face.setFace("face".getBytes());
		face.setForceCaptured(false);
		face.setNumOfRetries(2);
		face.setQualityScore(90);
		
		BiometricInfoDTO info=new BiometricInfoDTO();
		info.setFingerprintDetailsDTO(listOfFingerPrints);
		info.setIrisDetailsDTO(iriesList);
		info.setFace(face);
		
		
		PublicKeyResponse<String> publicKeyResponse = new PublicKeyResponse<>();
		LinkedHashMap<String, Object> response = new LinkedHashMap<>();
		response.put("publicKey", "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAp7rM0m8Vp91T5IkuSPhwibGivMn46-Eg3sjwiNDd1M0frs6Vmf1A7Guo-tjgMAAWkEuk-fX9yJ4j9HxMBJmhrIM7bFFaF4htOh5A78qvTdgahvGccpC080m_pDllSIhJk3R5TExLMxC823gvm0cqJQh2xvttissag0fHXBeqo6SAvN7214WgN2zqPZiN2PbDWv8qICHTw8R2GGshc4pu-rh4atLWe7waLJFrFY00TpKLGTrR7TEH4GQ45LcQ7fRV4N63FN7A0ikM1jjLWoi78rn4PCLL5T91o3VBUau3V0WKdDQ9ZrIjcFdjaLS6gzmM9mI6BRIeMGVxOfnuops8FwIDAQAB");
		response.put("issuedAt", LocalDateTime.now());
		response.put("expiryAt", LocalDateTime.now());

		publicKeyResponse.setResponse(response);

		publicKeyResponse.setAlias("ALIAS");
		publicKeyResponse.setExpiryAt(LocalDateTime.now());
		publicKeyResponse.setIssuedAt(LocalDateTime.now());
		publicKeyResponse.setPublicKey("MY_PUBLIC_KEY");
		
		
		LinkedHashMap<String, Object> responseApi=new LinkedHashMap<>();
		LinkedHashMap<String, Object> responseMap=new LinkedHashMap<>();
		responseMap.put(RegistrationConstants.ON_BOARD_AUTH_STATUS, true);
		responseApi.put(RegistrationConstants.RESPONSE, responseMap);
		responseApi.put(RegistrationConstants.ERRORS, new ArrayList<>());
		
		biometricDTO.setOperatorBiometricDTO(info);
		Map<String,Object> publicMapData=new LinkedHashMap<String,Object>();
		publicMapData.put("authStatus", false);
		publicMapData.put(RegistrationConstants.PUBLIC_KEY, "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAp7rM0m8Vp91T5IkuSPhwibGivMn46-Eg3sjwiNDd1M0frs6Vmf1A7Guo-tjgMAAWkEuk-fX9yJ4j9HxMBJmhrIM7bFFaF4htOh5A78qvTdgahvGccpC080m_pDllSIhJk3R5TExLMxC823gvm0cqJQh2xvttissag0fHXBeqo6SAvN7214WgN2zqPZiN2PbDWv8qICHTw8R2GGshc4pu-rh4atLWe7waLJFrFY00TpKLGTrR7TEH4GQ45LcQ7fRV4N63FN7A0ikM1jjLWoi78rn4PCLL5T91o3VBUau3V0WKdDQ9ZrIjcFdjaLS6gzmM9mI6BRIeMGVxOfnuops8FwIDAQAB");
		Map<String,Object> myMapPublic=new LinkedHashMap<String,Object>();
		myMapPublic.put("response", publicMapData);
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyBoolean(), Mockito.anyString())).thenReturn(myMapPublic);
		SecretKey mock = PowerMockito.mock(SecretKey.class);
		PowerMockito.when(keyGenerator.getSymmetricKey()).thenReturn(PowerMockito.mock(SecretKey.class));
		Mockito.when(encryptor.symmetricEncrypt(Mockito.any(), Mockito.any(), Mockito.eq(null))).thenReturn("test".getBytes());
		Mockito.when(encryptor.asymmetricEncrypt(Mockito.any(), Mockito.any())).thenReturn("test".getBytes());
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.any(), Mockito.anyString())).thenReturn(responseApi);
		Mockito.when(userOnBoardDao.insert(biometricDTO)).thenReturn(RegistrationConstants.SUCCESS);
		Mockito.when(userOnBoardDao.save()).thenReturn(RegistrationConstants.SUCCESS);	
		Map<String,Object> myMapData=new LinkedHashMap<String,Object>();
		myMapData.put("data", "IO7pawokCjgnjR-jo40DCbz2w0ELErUtCcDoKL6VTe644pqUPMq22MOXThNIXyLybBCO4LVSk3uaipYDnZr4ANxMsYr3WDp7KbQwcG9KnmVOm1mbzzCpkkJNRhbA8O2Ucj77KOXOLI3TszTKRUhBz2uLs46IvWiksuLsZwurYgkne62Gqr1QMh74PDPE9dKgvOC3o4hfPbP53H3bbc7HNR_Vu1kzn4xfyYFSNGATNVVXitZOlAKYgTcFdGde3eX2bos7OAivQad4TsmWeT0Pjmg1gixdwLq5agXhix3Pc-DvQgJzx-OnNUulAQTDkdbREx2TOJAu_ptF2JrrMuEflyNLRVlfU1BMSVRURVIjTsKlrgzfzXOXIb-NqJQWkz2OSMg");
		myMapData.put("authStatus", false);
		Map<String,Object> myMap=new LinkedHashMap<String,Object>();
		myMap.put("response", myMapData);
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyMap(),Mockito.anyString())).thenReturn(myMap);
		userOnboardServiceImpl.validate(biometricDTO);
		
	}
	
	@Test
	public void userOnBoardNetworkFail() throws HttpClientErrorException, SocketTimeoutException, RegBaseCheckedException {
		
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		PowerMockito.mockStatic(KeyGenerator.class);
		Mockito.when(RegistrationAppHealthCheckUtil.isNetworkAvailable()).thenReturn(true);
		
		BiometricDTO biometricDTO= new BiometricDTO();
		
		List<FingerprintDetailsDTO> listOfFingerPrints = new ArrayList<>();
		List<FingerprintDetailsDTO> listOfFingerSegmets = new ArrayList<>();

		FingerprintDetailsDTO fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftIndex".getBytes());
		fingerDto.setFingerprintImageName("leftIndex");
		fingerDto.setFingerPrintISOImage("leftIndex".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftLittle".getBytes());
		fingerDto.setFingerprintImageName("leftLittle");
		fingerDto.setFingerPrintISOImage("leftLittle".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftMiddle".getBytes());
		fingerDto.setFingerprintImageName("leftMiddle");
		fingerDto.setFingerPrintISOImage("leftMiddle".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftRing".getBytes());
		fingerDto.setFingerprintImageName("leftRing");
		fingerDto.setFingerPrintISOImage("leftRing".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftThumb".getBytes());
		fingerDto.setFingerprintImageName("leftThumb");
		fingerDto.setFingerPrintISOImage("leftThumb".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftFore".getBytes());
		fingerDto.setFingerprintImageName("leftFore");
		fingerDto.setFingerPrintISOImage("leftFore".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);


		List<IrisDetailsDTO> iriesList=new ArrayList<>();
		IrisDetailsDTO iries=new IrisDetailsDTO();
		
		iries.setIris("right".getBytes());
		iries.setIrisImageName("Right");
		iries.setIrisType("Eyes");
		iries.setNumOfIrisRetry(2);
		iries.setQualityScore(90);
		iriesList.add(iries);
		
		FaceDetailsDTO face=new FaceDetailsDTO();
		face.setFace("face".getBytes());
		face.setForceCaptured(false);
		face.setNumOfRetries(2);
		face.setQualityScore(90);
		
		BiometricInfoDTO info=new BiometricInfoDTO();
		info.setFingerprintDetailsDTO(listOfFingerPrints);
		info.setIrisDetailsDTO(iriesList);
		info.setFace(face);
		
		
		PublicKeyResponse<String> publicKeyResponse = new PublicKeyResponse<>();
		LinkedHashMap<String, Object> response = new LinkedHashMap<>();
		publicKeyResponse.setResponse(response);

		publicKeyResponse.setAlias("ALIAS");
		publicKeyResponse.setExpiryAt(LocalDateTime.now());
		publicKeyResponse.setIssuedAt(LocalDateTime.now());
		publicKeyResponse.setPublicKey("MY_PUBLIC_KEY");
		
		
		LinkedHashMap<String, Object> responseApi=new LinkedHashMap<>();
		LinkedHashMap<String, Object> responseMap=new LinkedHashMap<>();
		responseMap.put(RegistrationConstants.ON_BOARD_AUTH_STATUS, true);
		responseApi.put(RegistrationConstants.RESPONSE, responseMap);
		responseApi.put(RegistrationConstants.ERRORS, new ArrayList<>());
		
		Map<String,Object> publicMapData=new LinkedHashMap<String,Object>();
		publicMapData.put(RegistrationConstants.PUBLIC_KEY, "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAp7rM0m8Vp91T5IkuSPhwibGivMn46-Eg3sjwiNDd1M0frs6Vmf1A7Guo-tjgMAAWkEuk-fX9yJ4j9HxMBJmhrIM7bFFaF4htOh5A78qvTdgahvGccpC080m_pDllSIhJk3R5TExLMxC823gvm0cqJQh2xvttissag0fHXBeqo6SAvN7214WgN2zqPZiN2PbDWv8qICHTw8R2GGshc4pu-rh4atLWe7waLJFrFY00TpKLGTrR7TEH4GQ45LcQ7fRV4N63FN7A0ikM1jjLWoi78rn4PCLL5T91o3VBUau3V0WKdDQ9ZrIjcFdjaLS6gzmM9mI6BRIeMGVxOfnuops8FwIDAQAB");
		Map<String,Object> myMapPublic=new LinkedHashMap<String,Object>();
		myMapPublic.put("response", publicMapData);
		
		biometricDTO.setOperatorBiometricDTO(info);
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyBoolean(), Mockito.anyString())).thenReturn(myMapPublic);
		SecretKey mock = PowerMockito.mock(SecretKey.class);
		PowerMockito.when(keyGenerator.getSymmetricKey()).thenReturn(PowerMockito.mock(SecretKey.class));
		Mockito.when(encryptor.symmetricEncrypt(Mockito.any(), Mockito.any(), Mockito.eq(null))).thenReturn("test".getBytes());
		Mockito.when(encryptor.asymmetricEncrypt(Mockito.any(), Mockito.any())).thenReturn("test".getBytes());
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.any(), Mockito.anyString())).thenReturn(responseApi);
		Mockito.when(userOnBoardDao.insert(biometricDTO)).thenReturn(RegistrationConstants.SUCCESS);
		Mockito.when(userOnBoardDao.save()).thenReturn(RegistrationConstants.SUCCESS);	
		Map<String,Object> myMapData=new LinkedHashMap<String,Object>();
		publicMapData.put("authStatus", true);
		myMapData.put("data", "IO7pawokCjgnjR-jo40DCbz2w0ELErUtCcDoKL6VTe644pqUPMq22MOXThNIXyLybBCO4LVSk3uaipYDnZr4ANxMsYr3WDp7KbQwcG9KnmVOm1mbzzCpkkJNRhbA8O2Ucj77KOXOLI3TszTKRUhBz2uLs46IvWiksuLsZwurYgkne62Gqr1QMh74PDPE9dKgvOC3o4hfPbP53H3bbc7HNR_Vu1kzn4xfyYFSNGATNVVXitZOlAKYgTcFdGde3eX2bos7OAivQad4TsmWeT0Pjmg1gixdwLq5agXhix3Pc-DvQgJzx-OnNUulAQTDkdbREx2TOJAu_ptF2JrrMuEflyNLRVlfU1BMSVRURVIjTsKlrgzfzXOXIb-NqJQWkz2OSMg");
		myMapData.put("authStatus", true);
		Map<String,Object> myMap=new LinkedHashMap<String,Object>();
		myMap.put("response", myMapData);
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.anyMap(),Mockito.anyString())).thenReturn(myMap);
		userOnboardServiceImpl.validate(biometricDTO);
		
	}
	
	@Test
	public void userOnBoardFailure() throws RegBaseCheckedException {
		
		BiometricDTO biometricDTO= new BiometricDTO();
		
		List<FingerprintDetailsDTO> listOfFingerPrints = new ArrayList<>();
		List<FingerprintDetailsDTO> listOfFingerSegmets = new ArrayList<>();

		FingerprintDetailsDTO fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftIndex".getBytes());
		fingerDto.setFingerprintImageName("leftIndex");
		fingerDto.setFingerPrintISOImage("leftIndex".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftLittle".getBytes());
		fingerDto.setFingerprintImageName("leftLittle");
		fingerDto.setFingerPrintISOImage("leftLittle".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftMiddle".getBytes());
		fingerDto.setFingerprintImageName("leftMiddle");
		fingerDto.setFingerPrintISOImage("leftMiddle".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftRing".getBytes());
		fingerDto.setFingerprintImageName("leftRing");
		fingerDto.setFingerPrintISOImage("leftRing".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftThumb".getBytes());
		fingerDto.setFingerprintImageName("leftThumb");
		fingerDto.setFingerPrintISOImage("leftThumb".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftFore".getBytes());
		fingerDto.setFingerprintImageName("leftFore");
		fingerDto.setFingerPrintISOImage("leftFore".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);


		List<IrisDetailsDTO> iriesList=new ArrayList<>();
		IrisDetailsDTO iries=new IrisDetailsDTO();
		
		iries.setIris("right".getBytes());
		iries.setIrisImageName("Right");
		iries.setIrisType("Eyes");
		iries.setNumOfIrisRetry(2);
		iries.setQualityScore(90);
		iriesList.add(iries);
		
		FaceDetailsDTO face=new FaceDetailsDTO();
		face.setFace("face".getBytes());
		face.setForceCaptured(false);
		face.setNumOfRetries(2);
		face.setQualityScore(90);
		
		BiometricInfoDTO info=new BiometricInfoDTO();
		info.setFingerprintDetailsDTO(listOfFingerPrints);
		info.setIrisDetailsDTO(iriesList);
		info.setFace(face);
		
		biometricDTO.setOperatorBiometricDTO(info);
		Mockito.when(userOnBoardDao.insert(biometricDTO)).thenReturn(RegistrationConstants.SUCCESS);
		userOnboardServiceImpl.validate(biometricDTO);
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void userOnBoardException() throws RegBaseCheckedException {
		
		BiometricDTO biometricDTO= new BiometricDTO();
		
		List<FingerprintDetailsDTO> listOfFingerPrints = new ArrayList<>();
		List<FingerprintDetailsDTO> listOfFingerSegmets = new ArrayList<>();

		FingerprintDetailsDTO fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftIndex".getBytes());
		fingerDto.setFingerprintImageName("leftIndex");
		fingerDto.setFingerPrintISOImage("leftIndex".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftLittle".getBytes());
		fingerDto.setFingerprintImageName("leftLittle");
		fingerDto.setFingerPrintISOImage("leftLittle".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftMiddle".getBytes());
		fingerDto.setFingerprintImageName("leftMiddle");
		fingerDto.setFingerPrintISOImage("leftMiddle".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftRing".getBytes());
		fingerDto.setFingerprintImageName("leftRing");
		fingerDto.setFingerPrintISOImage("leftRing".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftThumb".getBytes());
		fingerDto.setFingerprintImageName("leftThumb");
		fingerDto.setFingerPrintISOImage("leftThumb".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftFore".getBytes());
		fingerDto.setFingerprintImageName("leftFore");
		fingerDto.setFingerPrintISOImage("leftFore".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);


		List<IrisDetailsDTO> iriesList=new ArrayList<>();
		IrisDetailsDTO iries=new IrisDetailsDTO();
		
		iries.setIris("right".getBytes());
		iries.setIrisImageName("Right");
		iries.setIrisType("Eyes");
		iries.setNumOfIrisRetry(2);
		iries.setQualityScore(90);
		iriesList.add(iries);
		
		FaceDetailsDTO face=new FaceDetailsDTO();
		face.setFace("face".getBytes());
		face.setForceCaptured(false);
		face.setNumOfRetries(2);
		face.setQualityScore(90);
		
		BiometricInfoDTO info=new BiometricInfoDTO();
		info.setFingerprintDetailsDTO(listOfFingerPrints);
		info.setIrisDetailsDTO(iriesList);
		info.setFace(face);
		
		biometricDTO.setOperatorBiometricDTO(info);
		Mockito.when(userOnBoardDao.insert(biometricDTO)).thenThrow(RegBaseUncheckedException.class);
		userOnboardServiceImpl.validate(biometricDTO);
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void userOnBoardRunException() throws RegBaseCheckedException {
		
		BiometricDTO biometricDTO= new BiometricDTO();
		
		List<FingerprintDetailsDTO> listOfFingerPrints = new ArrayList<>();
		List<FingerprintDetailsDTO> listOfFingerSegmets = new ArrayList<>();

		FingerprintDetailsDTO fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftIndex".getBytes());
		fingerDto.setFingerprintImageName("leftIndex");
		fingerDto.setFingerPrintISOImage("leftIndex".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftLittle".getBytes());
		fingerDto.setFingerprintImageName("leftLittle");
		fingerDto.setFingerPrintISOImage("leftLittle".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftMiddle".getBytes());
		fingerDto.setFingerprintImageName("leftMiddle");
		fingerDto.setFingerPrintISOImage("leftMiddle".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftRing".getBytes());
		fingerDto.setFingerprintImageName("leftRing");
		fingerDto.setFingerPrintISOImage("leftRing".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftThumb".getBytes());
		fingerDto.setFingerprintImageName("leftThumb");
		fingerDto.setFingerPrintISOImage("leftThumb".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);

		fingerDto = new FingerprintDetailsDTO();
		fingerDto.setFingerPrint("leftFore".getBytes());
		fingerDto.setFingerprintImageName("leftFore");
		fingerDto.setFingerPrintISOImage("leftFore".getBytes());
		fingerDto.setFingerType("FingerPrint");
		fingerDto.setNumRetry(2);
		fingerDto.setQualityScore(90);
		listOfFingerSegmets.add(fingerDto);
		fingerDto.setSegmentedFingerprints(listOfFingerSegmets);
		listOfFingerPrints.add(fingerDto);


		List<IrisDetailsDTO> iriesList=new ArrayList<>();
		IrisDetailsDTO iries=new IrisDetailsDTO();
		
		iries.setIris("right".getBytes());
		iries.setIrisImageName("Right");
		iries.setIrisType("Eyes");
		iries.setNumOfIrisRetry(2);
		iries.setQualityScore(90);
		iriesList.add(iries);
		
		FaceDetailsDTO face=new FaceDetailsDTO();
		face.setFace("face".getBytes());
		face.setForceCaptured(false);
		face.setNumOfRetries(2);
		face.setQualityScore(90);
		
		BiometricInfoDTO info=new BiometricInfoDTO();
		info.setFingerprintDetailsDTO(listOfFingerPrints);
		info.setIrisDetailsDTO(iriesList);
		info.setFace(face);
		
		biometricDTO.setOperatorBiometricDTO(info);
		Mockito.when(userOnBoardDao.insert(biometricDTO)).thenThrow(RuntimeException.class);
		userOnboardServiceImpl.validate(biometricDTO);
		
	}*/
	


	
	@Test
	public void getLastUpdatedTime() {
		Mockito.when(userOnBoardDao.getLastUpdatedTime(Mockito.anyString())).thenReturn(new Timestamp(System.currentTimeMillis()));
		assertNotNull(userOnboardServiceImpl.getLastUpdatedTime("User123"));
	}
	
	/*
	 * @Test(expected=RegBaseCheckedException.class) public void onBoard() throws
	 * RegBaseCheckedException { BiometricDTO biometricDTO=null;
	 * userOnboardServiceImpl.validate(biometricDTO); }
	 */
	


}
