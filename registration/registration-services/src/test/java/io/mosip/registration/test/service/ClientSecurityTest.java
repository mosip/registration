package io.mosip.registration.test.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.mosip.registration.service.security.ClientSecurity;
import io.mosip.registration.service.security.impl.LocalClientSecurityImpl;



@RunWith(PowerMockRunner.class)
@PrepareForTest({ LocalClientSecurityImpl.class})
public class ClientSecurityTest {
	
	private static final String ALGORITHM = "RSA";
	private static final int KEY_LENGTH = 2048;
	private static final String KEYS_DIR = ".mosipkeys";
	private static final String PRIVATE_KEY = "reg.key";
	private static final String PUBLIC_KEY = "reg.pub";
	
	private static String tempDirectory; 
	
	private ClientSecurity clientSecurity;
	
	@Before
	public void setup() throws Exception {
		try {
			
			Path path = Files.createTempDirectory("mosiptest", new FileAttribute<?>[] {});			
			KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(ALGORITHM);
			keyGenerator.initialize(KEY_LENGTH, new SecureRandom());
			KeyPair keypair = keyGenerator.generateKeyPair();
			File directoy = new File(path.toAbsolutePath().toString()+ File.separator + KEYS_DIR);
			directoy.mkdirs();			
			
			PowerMockito.mockStatic(System.class);
			PowerMockito.doReturn(path.toAbsolutePath().toString()).when(System.class, "getProperty", "user.home", 
					path.toAbsolutePath().toString());
			
			tempDirectory = directoy.getAbsolutePath();
			
			createKeyFile(PRIVATE_KEY, keypair.getPrivate().getEncoded());
			createKeyFile(PUBLIC_KEY, keypair.getPublic().getEncoded());
			
			clientSecurity = new LocalClientSecurityImpl();
			
		} catch (NoSuchAlgorithmException | IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testEncryption() {
		String plainText = "Simple Text";
		byte[] cipherText = clientSecurity.asymmetricEncrypt(plainText.getBytes());		
		byte[] plainBytes = clientSecurity.asymmetricDecrypt(cipherText);		
		assertEquals(plainText, new String(plainBytes));
	}
	
	@Test
	public void testSignature() {
		String plainText = "Simple Text";
		byte[] signature = clientSecurity.signData(plainText.getBytes());		
		boolean isValidSignature = clientSecurity.validateSignature(signature, plainText.getBytes());		
		assertEquals(true, isValidSignature);
	}
	
	private void createKeyFile(String fileName, byte[] key) {
		try(FileOutputStream os = 
				new FileOutputStream(tempDirectory + File.separator + fileName)) {
			os.write(key);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
