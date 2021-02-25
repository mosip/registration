package io.mosip.registration.test.authentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.registration.util.common.OTPManager;
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

import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.registration.dto.AuthTokenDTO;
import io.mosip.registration.dto.AuthenticationValidatorDTO;
import io.mosip.registration.dto.UserDTO;
import io.mosip.registration.dto.UserPasswordDTO;
import io.mosip.registration.service.login.LoginService;
import io.mosip.registration.service.security.impl.AuthenticationServiceImpl;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ HMACUtils2.class, CryptoUtil.class})
public class AuthenticationServiceTest {

	@Mock
	OTPManager otpManager;
	
	@Mock
	private LoginService loginService;

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	
	@InjectMocks
	private AuthenticationServiceImpl authenticationServiceImpl;
	
	@Before
	public void initialize() {
		PowerMockito.mockStatic(HMACUtils2.class);
		PowerMockito.mockStatic(CryptoUtil.class);
	}
	
	@Test
	public void getOtpValidatorTest() {
		AuthTokenDTO authTokenDTO =new AuthTokenDTO();
		when(otpManager.validateOTP(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean()))
				.thenReturn(authTokenDTO);
		assertNotNull(authenticationServiceImpl.authValidator("otp", "mosip", "12345", true));
	}

	
	@Test
	public void validatePasswordTest() throws NoSuchAlgorithmException {
		UserDTO userDTO = new UserDTO();
		userDTO.setSalt("salt");
		UserPasswordDTO userPassword = new UserPasswordDTO();
		userPassword.setPwd("mosip");
		userDTO.setUserPassword(userPassword);
		
		AuthenticationValidatorDTO authenticationValidatorDTO=new AuthenticationValidatorDTO();
		authenticationValidatorDTO.setUserId("mosip");
		authenticationValidatorDTO.setPassword("mosip");
		
		Mockito.when(loginService.getUserDetail("mosip")).thenReturn(userDTO);		
		Mockito.when(CryptoUtil.decodeBase64("salt")).thenReturn("salt".getBytes());		
		Mockito.when(HMACUtils2.digestAsPlainTextWithSalt("mosip".getBytes(), "salt".getBytes())).thenReturn("mosip");
		
		assertEquals("Username and Password Match", authenticationServiceImpl.validatePassword(authenticationValidatorDTO));
		
	}
	
	@Test
	public void validatePasswordNotMatchTest() throws NoSuchAlgorithmException {
		UserDTO userDTO = new UserDTO();
		userDTO.setSalt("salt");
		UserPasswordDTO userPassword = new UserPasswordDTO();
		userPassword.setPwd("mosip");
		userDTO.setUserPassword(userPassword);
		
		AuthenticationValidatorDTO authenticationValidatorDTO=new AuthenticationValidatorDTO();
		authenticationValidatorDTO.setUserId("mosip");
		authenticationValidatorDTO.setPassword("mosip");
		
		Mockito.when(loginService.getUserDetail("mosip")).thenReturn(userDTO);		
		Mockito.when(CryptoUtil.decodeBase64("salt")).thenReturn("salt".getBytes());		
		Mockito.when(HMACUtils2.digestAsPlainTextWithSalt("mosip1".getBytes(), "salt".getBytes())).thenReturn("mosip1");
		
		assertEquals("Username and Password Not Match", authenticationServiceImpl.validatePassword(authenticationValidatorDTO));
		
	}
	
	@Test
	public void validatePasswordFailureTest() {
		UserDTO userDTO = new UserDTO();
		userDTO.setId("mosip");
		AuthenticationValidatorDTO authenticationValidatorDTO=new AuthenticationValidatorDTO();
		authenticationValidatorDTO.setUserId("mosip");
		authenticationValidatorDTO.setPassword("mosip");
	
		Mockito.when(loginService.getUserDetail("mosip")).thenReturn(userDTO);			
		assertEquals("Username and Password Not Match", authenticationServiceImpl.validatePassword(authenticationValidatorDTO));
		
	}
	
	@Test
	public void validatePasswordFailure1Test() {		
		AuthenticationValidatorDTO authenticationValidatorDTO=new AuthenticationValidatorDTO();
		
		Mockito.when(loginService.getUserDetail("mosip")).thenReturn(null);			
		assertEquals("Username and Password Not Match", authenticationServiceImpl.validatePassword(authenticationValidatorDTO));
		
	}

}
