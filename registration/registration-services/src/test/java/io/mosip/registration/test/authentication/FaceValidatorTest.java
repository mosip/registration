package io.mosip.registration.test.authentication;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.kernel.core.bioapi.model.KeyValuePair;
//import io.mosip.kernel.core.bioapi.model.Score;
import io.mosip.kernel.core.bioapi.spi.IBioApi;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dao.UserDetailDAO;
import io.mosip.registration.dto.AuthenticationValidatorDTO;
import io.mosip.registration.dto.biometric.FaceDetailsDTO;
import io.mosip.registration.service.bio.BioService;
import io.mosip.registration.validator.FaceValidatorImpl;

public class FaceValidatorTest {
	
	@InjectMocks
	private FaceValidatorImpl faceValidatorImpl;
	
	@Mock
	private BioService bioService;
	
	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	
	@Mock
	private UserDetailDAO userDetailDAO;
	
	@Mock
	IBioApi ibioApi;
	
	private AuthenticationValidatorDTO authenticationValidatorDTO = new AuthenticationValidatorDTO();

	@Before
	public void initialize() {
		authenticationValidatorDTO.setUserId("mosip");
		
		FaceDetailsDTO faceDetailsDTO = new FaceDetailsDTO();
		
		Map<String, Object> appMap = new HashMap<>();
		ApplicationContext.getInstance().setApplicationMap(appMap);
		byte[] bytes = new byte[100];
		Arrays.fill(bytes, (byte) 1);

		faceDetailsDTO.setFace(bytes);
		authenticationValidatorDTO.setFaceDetail(faceDetailsDTO);
	}
	
	/*@Test
	public void validateTest() throws BiometricException {
		Score score[] = new Score[1];
		Score score2 = new Score();
		score2.setInternalScore(30);
		score[0] = score2;
		when(ibioApi.match(Mockito.any(), Mockito.any(), (KeyValuePair[]) Mockito.isNull())).thenReturn(score);
		assertThat(faceValidatorImpl.validate(authenticationValidatorDTO), is(false));
	}
	
	@Test
	public void validateTestGoodQuality() throws BiometricException {
		Score score[] = new Score[1];
		Score score2 = new Score();
		score2.setScaleScore(90);
		score[0] = score2;
		when(ibioApi.match(Mockito.any(), Mockito.any(), (KeyValuePair[]) Mockito.isNull())).thenReturn(score);
		assertThat(faceValidatorImpl.validate(authenticationValidatorDTO), is(true));
	}
	
	@Test
	public void validateTestException() throws BiometricException {
		Score score[] = new Score[1];
		Score score2 = new Score();
		score2.setInternalScore(30);
		score[0] = score2;
		when(ibioApi.match(Mockito.any(), Mockito.any(), (KeyValuePair[]) Mockito.isNull())).thenThrow(BiometricException.class);
		assertThat(faceValidatorImpl.validate(authenticationValidatorDTO), is(false));
	}*/
	
	@Test
	public void validateUserTest() {
		authenticationValidatorDTO.setUserId("");
		faceValidatorImpl.validate(authenticationValidatorDTO);
	}
	
	@Test
	public void validateAuthTest() {
		assertNull(faceValidatorImpl.validate("mosip","123", true));
	}

}
