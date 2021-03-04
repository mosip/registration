package io.mosip.registration.test.service.packet.encryption.rsa;

import static org.mockito.Mockito.when;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

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
import org.springframework.beans.factory.annotation.Autowired;

import io.mosip.kernel.core.crypto.spi.CryptoCoreSpec;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dao.UserOnboardDAO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.repositories.CenterMachineRepository;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.service.security.impl.RSAEncryptionServiceImpl;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ ApplicationContext.class })
public class RSAEncryptionServiceTest {

	@InjectMocks
	private RSAEncryptionServiceImpl rsaEncryptionServiceImpl;
	@Mock
	private UserOnboardDAO userOnboardDAO;
	@Mock
	private CenterMachineRepository centerMachineRepository;

	@Mock
	private MachineMasterRepository machineMasterRepository;
	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	@Mock@Autowired
    private CryptoCoreSpec<byte[], byte[], SecretKey, PublicKey, PrivateKey, String> cryptoCore;
	
	@Before
	public void init() throws Exception {
		Map<String,Object> appMap = new HashMap<>();
		appMap.put(RegistrationConstants.ASYMMETRIC_ALG_NAME, "RSA");

		PowerMockito.mockStatic(ApplicationContext.class);
		PowerMockito.doReturn(appMap).when(ApplicationContext.class, "map");
	}

	@Test
	public void rsaPacketCreation() throws RegBaseCheckedException {
		byte[] decodedbytes = "e".getBytes();
		byte[] sessionbytes = "sesseion".getBytes();
		
		Mockito.when(userOnboardDAO.getStationID(Mockito.anyString())).thenReturn("1001");
		Mockito.when(userOnboardDAO.getCenterID(Mockito.anyString())).thenReturn("1001");
		when(cryptoCore.asymmetricEncrypt(Mockito.any(PublicKey.class), Mockito.anyString().getBytes()))
				.thenReturn(decodedbytes);

		rsaEncryptionServiceImpl.encrypt(sessionbytes);
	}

	@Test(expected = RegBaseUncheckedException.class)
	public void testRuntimeException() throws RegBaseCheckedException {
		rsaEncryptionServiceImpl.encrypt("data".getBytes());
	}

	@Test(expected = RegBaseCheckedException.class)
	public void invalidKeySpecTest() throws RegBaseCheckedException {
		byte[] decodedbytes = "e".getBytes();
		byte[] sessionbytes = "sesseion".getBytes();

		Mockito.when(userOnboardDAO.getStationID(Mockito.anyString())).thenReturn("1001");
		Mockito.when(userOnboardDAO.getCenterID(Mockito.anyString())).thenReturn("1001");
		
		when(cryptoCore.asymmetricEncrypt(Mockito.any(PublicKey.class), Mockito.anyString().getBytes()))
				.thenReturn(decodedbytes);
		
		rsaEncryptionServiceImpl.encrypt(sessionbytes);
	}

	@Test(expected = RegBaseCheckedException.class)
	public void invalidAlgorithmTest() throws Exception {
		byte[] decodedbytes = "e".getBytes();
		byte[] sessionbytes = "sesseion".getBytes();

		Map<String,Object> appMap = new HashMap<>();
		appMap.put(RegistrationConstants.ASYMMETRIC_ALG_NAME, "AES");

		PowerMockito.mockStatic(ApplicationContext.class);
		PowerMockito.doReturn(appMap).when(ApplicationContext.class, "map");
		when(cryptoCore.asymmetricEncrypt(Mockito.any(PublicKey.class), Mockito.anyString().getBytes()))
				.thenReturn(decodedbytes);
		Mockito.when(userOnboardDAO.getStationID(Mockito.anyString())).thenReturn("1001");
		Mockito.when(userOnboardDAO.getCenterID(Mockito.anyString())).thenReturn("1001");

		rsaEncryptionServiceImpl.encrypt(sessionbytes);
	}

	@Test(expected = RegBaseCheckedException.class)
	public void rsaPublicKeyNotFound() throws RegBaseCheckedException {
		rsaEncryptionServiceImpl.encrypt("data".getBytes());
	}

	@Test(expected = RegBaseCheckedException.class)
	public void invalidDataForEncryption() throws RegBaseCheckedException {
		rsaEncryptionServiceImpl.encrypt(null);
	}

}