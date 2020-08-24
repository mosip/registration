package io.mosip.registration.test.authentication;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
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

import com.machinezoo.sourceafis.FingerprintTemplate;

import io.mosip.kernel.core.bioapi.exception.BiometricException;
//import io.mosip.kernel.core.bioapi.model.CompositeScore;
import io.mosip.kernel.core.bioapi.model.KeyValuePair;
//import io.mosip.kernel.core.bioapi.model.Score;
import io.mosip.kernel.core.bioapi.spi.IBioApi;
import io.mosip.kernel.core.cbeffutil.entity.BIR;
import io.mosip.kernel.core.cbeffutil.entity.BIR.BIRBuilder;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.UserDetailDAO;
import io.mosip.registration.dto.AuthenticationValidatorDTO;
import io.mosip.registration.dto.biometric.FingerprintDetailsDTO;
import io.mosip.registration.entity.UserBiometric;
import io.mosip.registration.service.bio.BioService;
import io.mosip.registration.validator.FingerprintValidatorImpl;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ SessionContext.class, ApplicationContext.class})
public class FingerprintValidatorTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@InjectMocks
	FingerprintValidatorImpl fingerprintValidator;

	@Mock
	private UserDetailDAO userDetailDAO;

	@Mock
	private BioService bioService;

	@Mock
	private IBioApi ibioApi;
	
	

	AuthenticationValidatorDTO authenticationValidatorDTO = new AuthenticationValidatorDTO();
	private ApplicationContext applicationContext = ApplicationContext.getInstance();

	@Before
	public void initialize() throws IOException {

		Map<String, Object> temp = new HashMap<String, Object>();
		temp.put("mosip.registration.finger_print_score", "1");
		applicationContext.setApplicationMap(temp);
		authenticationValidatorDTO.setUserId("mosip");
		authenticationValidatorDTO.setPassword("mosip");
		authenticationValidatorDTO.setOtp("12345");
		List<FingerprintDetailsDTO> fingerPrintDetails = new ArrayList<>();
		FingerprintDetailsDTO fingerprintDetailsDTO = new FingerprintDetailsDTO();

		byte[] bytes = new byte[100];
		Arrays.fill(bytes, (byte) 1);

		fingerprintDetailsDTO
				.setFingerPrint(IOUtils.toByteArray(this.getClass().getResourceAsStream("/ISOTemplate.iso")));
		fingerprintDetailsDTO.setFingerprintImageName("fingerprintImageName");
		fingerprintDetailsDTO.setFingerType("fingerType");
		fingerprintDetailsDTO.setForceCaptured(false);
		fingerprintDetailsDTO.setNumRetry(2);
		fingerprintDetailsDTO.setQualityScore(90.1);
		fingerPrintDetails.add(fingerprintDetailsDTO);
		authenticationValidatorDTO.setFingerPrintDetails(fingerPrintDetails);
	}
	
	@Test
	public void validateSingleTest() throws BiometricException {
		Map<String, Object> applicationMap = new HashMap<>();	
		PowerMockito.mockStatic(ApplicationContext.class);
		when(ApplicationContext.map()).thenReturn(applicationMap);
		ApplicationContext.map().put(RegistrationConstants.DEDUPLICATION_FINGERPRINT_ENABLE_FLAG, RegistrationConstants.DISABLE);
		assertThat(fingerprintValidator.validate(authenticationValidatorDTO), is(false));
	}
	
	/*@Test
	public void validateSingleTestWithBioDedup() throws BiometricException {
		authenticationValidatorDTO.setAuthValidationType(RegistrationConstants.SINGLE);
		FingerprintDetailsDTO fingerprintDetailsDTO=new FingerprintDetailsDTO();
		fingerprintDetailsDTO.setFingerType("right index");
		UserBiometric userBiometric = new UserBiometric();
		userBiometric.setQualityScore(91);
		List<UserBiometric> userBiometrics = new ArrayList<>();
		userBiometrics.add(userBiometric);

		FingerprintTemplate fingerprintTemplate = new FingerprintTemplate()
				.convert(authenticationValidatorDTO.getFingerPrintDetails().get(0).getFingerPrint());
		String minutiae = fingerprintTemplate.serialize();
      	userBiometric.setBioMinutia(minutiae);
        //capturedBir.setBDB(minutiae.getBytes());
		Map<String, Object> applicationMap = new HashMap<>();	
		PowerMockito.mockStatic(ApplicationContext.class);
		when(ApplicationContext.map()).thenReturn(applicationMap);
		ApplicationContext.map().put(RegistrationConstants.DEDUPLICATION_FINGERPRINT_ENABLE_FLAG, RegistrationConstants.ENABLE);

		when(userDetailDAO.getUserSpecificBioDetails("mosip", "FIN")).thenReturn(userBiometrics);
		//when(bioService.validateFP(authenticationValidatorDTO.getFingerPrintDetails().get(0), userBiometrics))
		//		.thenReturn(true);
		BIR bir = new BIR(new BIRBuilder().withBdb(minutiae.getBytes()));
		BIR biType[] = new BIR[userBiometrics.size()];
		BIR b = new BIR(new BIRBuilder().withBdb(minutiae.getBytes()));
		biType[0] = b;
		Score score[] = new Score[1];
		Score score2 = new Score();
		score2.setInternalScore(30);
		score[0] = score2;
		CompositeScore c=new CompositeScore();
		c.setIndividualScores(score);
		when(ibioApi.compositeMatch(Mockito.any(), Mockito.any(), (KeyValuePair[]) Mockito.isNull())).thenReturn(c);
		assertThat(fingerprintValidator.validate(authenticationValidatorDTO), is(false));	
	}*/
	
	/*@Test
	public void validateSingleTestWithBioDedupGoodQuality() throws BiometricException {
		authenticationValidatorDTO.setAuthValidationType(RegistrationConstants.SINGLE);
		FingerprintDetailsDTO fingerprintDetailsDTO=new FingerprintDetailsDTO();
		fingerprintDetailsDTO.setFingerType("right index");
		UserBiometric userBiometric = new UserBiometric();
		userBiometric.setQualityScore(91);
		List<UserBiometric> userBiometrics = new ArrayList<>();
		userBiometrics.add(userBiometric);

		FingerprintTemplate fingerprintTemplate = new FingerprintTemplate()
				.convert(authenticationValidatorDTO.getFingerPrintDetails().get(0).getFingerPrint());
		String minutiae = fingerprintTemplate.serialize();
      	userBiometric.setBioMinutia(minutiae);
        //capturedBir.setBDB(minutiae.getBytes());
		Map<String, Object> applicationMap = new HashMap<>();	
		PowerMockito.mockStatic(ApplicationContext.class);
		when(ApplicationContext.map()).thenReturn(applicationMap);
		ApplicationContext.map().put(RegistrationConstants.DEDUPLICATION_FINGERPRINT_ENABLE_FLAG, RegistrationConstants.ENABLE);

		when(userDetailDAO.getUserSpecificBioDetails("mosip", "FIN")).thenReturn(userBiometrics);
		//when(bioService.validateFP(authenticationValidatorDTO.getFingerPrintDetails().get(0), userBiometrics))
		//		.thenReturn(true);
		BIR bir = new BIR(new BIRBuilder().withBdb(minutiae.getBytes()));
		BIR biType[] = new BIR[userBiometrics.size()];
		BIR b = new BIR(new BIRBuilder().withBdb(minutiae.getBytes()));
		biType[0] = b;
		Score score[] = new Score[1];
		Score score2 = new Score();
		score2.setScaleScore(90);
		score[0] = score2;
		CompositeScore c=new CompositeScore();
		c.setIndividualScores(score);
		when(ibioApi.compositeMatch(Mockito.any(), Mockito.any(), (KeyValuePair[]) Mockito.isNull())).thenReturn(c);
		assertThat(fingerprintValidator.validate(authenticationValidatorDTO), is(true));	
	}
	
	@Test
	public void validateSingleTestWithBioDedupException() throws BiometricException {
		authenticationValidatorDTO.setAuthValidationType(RegistrationConstants.SINGLE);
		FingerprintDetailsDTO fingerprintDetailsDTO=new FingerprintDetailsDTO();
		fingerprintDetailsDTO.setFingerType("right index");
		UserBiometric userBiometric = new UserBiometric();
		userBiometric.setQualityScore(91);
		List<UserBiometric> userBiometrics = new ArrayList<>();
		userBiometrics.add(userBiometric);

		FingerprintTemplate fingerprintTemplate = new FingerprintTemplate()
				.convert(authenticationValidatorDTO.getFingerPrintDetails().get(0).getFingerPrint());
		String minutiae = fingerprintTemplate.serialize();
      	userBiometric.setBioMinutia(minutiae);
        //capturedBir.setBDB(minutiae.getBytes());
		Map<String, Object> applicationMap = new HashMap<>();	
		PowerMockito.mockStatic(ApplicationContext.class);
		when(ApplicationContext.map()).thenReturn(applicationMap);
		ApplicationContext.map().put(RegistrationConstants.DEDUPLICATION_FINGERPRINT_ENABLE_FLAG, RegistrationConstants.ENABLE);

		when(userDetailDAO.getUserSpecificBioDetails("mosip", "FIN")).thenReturn(userBiometrics);
		//when(bioService.validateFP(authenticationValidatorDTO.getFingerPrintDetails().get(0), userBiometrics))
		//		.thenReturn(true);
		BIR bir = new BIR(new BIRBuilder().withBdb(minutiae.getBytes()));
		BIR biType[] = new BIR[userBiometrics.size()];
		BIR b = new BIR(new BIRBuilder().withBdb(minutiae.getBytes()));
		biType[0] = b;
		Score score[] = new Score[1];
		Score score2 = new Score();
		score2.setScaleScore(90);
		score[0] = score2;
		CompositeScore c=new CompositeScore();
		c.setIndividualScores(score);
		when(ibioApi.compositeMatch(Mockito.any(), Mockito.any(), (KeyValuePair[]) Mockito.isNull())).thenThrow(BiometricException.class);
		assertThat(fingerprintValidator.validate(authenticationValidatorDTO), is(false));
	}
	
	@Test
	public void validateMultipleTest() throws BiometricException {
		
		authenticationValidatorDTO.setAuthValidationType(RegistrationConstants.MULTIPLE);
		FingerprintDetailsDTO fingerprintDetailsDTO = new FingerprintDetailsDTO();
		fingerprintDetailsDTO.setFingerType("right index");
		UserBiometric userBiometric = new UserBiometric();
		userBiometric.setQualityScore(91);
		userBiometric.setBioMinutia("bioMinutia");
		List<UserBiometric> userBiometrics = new ArrayList<>();
		userBiometrics.add(userBiometric);
		
		PowerMockito.mockStatic(SessionContext.class);
		SessionContext.map().put(RegistrationConstants.DUPLICATE_FINGER, fingerprintDetailsDTO);
		Map<String, Object> applicationMap = new HashMap<>();	
		PowerMockito.mockStatic(ApplicationContext.class);
		when(ApplicationContext.map()).thenReturn(applicationMap);
		ApplicationContext.map().put(RegistrationConstants.DEDUPLICATION_FINGERPRINT_ENABLE_FLAG, RegistrationConstants.ENABLE);

		when(userDetailDAO.getUserSpecificBioDetail("mosip", "FIN", "fingerType")).thenReturn(userBiometric);
		//when(bioService.validateFP(authenticationValidatorDTO.getFingerPrintDetails().get(0), userBiometrics))
		//		.thenReturn(true);
		FingerprintTemplate fingerprintTemplate = new FingerprintTemplate()
				.convert(authenticationValidatorDTO.getFingerPrintDetails().get(0).getFingerPrint());
		String minutiae = fingerprintTemplate.serialize();

		BIR bir = new BIR(new BIRBuilder().withBdb(minutiae.getBytes()));
		BIR biType[] = new BIR[userBiometrics.size()];
		BIR b = new BIR(new BIRBuilder().withBdb(minutiae.getBytes()));
		biType[0] = b;
		Score score[] = new Score[1];
		Score score2 = new Score();
		score2.setScaleScore(90);;
		score[0] = score2;
		CompositeScore c=new CompositeScore();
		c.setIndividualScores(score);
		when(ibioApi.compositeMatch(Mockito.any(), Mockito.any(), (KeyValuePair[]) Mockito.isNull())).thenReturn(c);
		assertThat(fingerprintValidator.validate(authenticationValidatorDTO), is(true));
	}*/

	@Test
	public void validateAuthTest() {
		assertNull(fingerprintValidator.validate("mosip", "123", true));
	}
}
