package io.mosip.registration.service.security.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.security.constants.MosipSecurityMethod;
import io.mosip.kernel.core.security.decryption.MosipDecryptor;
import io.mosip.kernel.core.security.encryption.MosipEncryptor;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.cryptosignature.impl.SignatureUtilImpl;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.service.security.ClientSecurity;

public class LocalClientSecurityImpl implements ClientSecurity {
	
	private static final Logger LOGGER = AppConfig.getLogger(LocalClientSecurityImpl.class);
	
	private static final String ALGORITHM = "RSA";
	private static final int KEY_LENGTH = 2048;
	private static final String SIGN_ALGORITHM = "SHA256withRSA";
	
	private static final String KEY_PATH = System.getProperty("user.home");
	private static final String KEYS_DIR = ".mosipkeys";
	private static final String PRIVATE_KEY = "reg.key";
	private static final String PUBLIC_KEY = "reg.pub";
	private static final String README = "readme.txt";
	

	public LocalClientSecurityImpl() throws Exception  {
		LOGGER.info(LoggerConstants.LOCAL_CLIENT_SECURITY_IMPL, RegistrationConstants.APPLICATION_ID,
				RegistrationConstants.APPLICATION_NAME, "Getting the instance of Local Security Impl");
		
		if(!doesKeyExists()) {			
			setupKeysDir();
			KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(ALGORITHM);
			keyGenerator.initialize(KEY_LENGTH, new SecureRandom());
			KeyPair keypair = keyGenerator.generateKeyPair();
			createKeyFile(PRIVATE_KEY, keypair.getPrivate().getEncoded());
			createKeyFile(PUBLIC_KEY, keypair.getPublic().getEncoded());
			createReadMe(keypair.getPublic());			
			
			LOGGER.info(LoggerConstants.LOCAL_CLIENT_SECURITY_IMPL, RegistrationConstants.APPLICATION_ID,
					RegistrationConstants.APPLICATION_NAME, "TPM NOT AVAILABLE - GENERATED NEW KEY PAIR SUCCESSFULLY.");
			
			LOGGER.info(LoggerConstants.LOCAL_CLIENT_SECURITY_IMPL, RegistrationConstants.APPLICATION_ID,
					RegistrationConstants.APPLICATION_NAME, "Check this file for publicKey and KeyIndex : " 
							+ getKeysDirPath() + File.separator + README);
			
			System.exit(0);
		}		
		LOGGER.info(LoggerConstants.LOCAL_CLIENT_SECURITY_IMPL, RegistrationConstants.APPLICATION_ID,
				RegistrationConstants.APPLICATION_NAME, "Completed getting the instance of Local Security Impl");
		LOGGER.info(LoggerConstants.LOCAL_CLIENT_SECURITY_IMPL, RegistrationConstants.APPLICATION_ID,
				RegistrationConstants.APPLICATION_NAME, "Check this file for publicKey and KeyIndex if already exist: " 
						+ getKeysDirPath() + File.separator + README);
	}	
	
	@Override
	public byte[] signData(byte[] dataToSign) {
		try {
			Signature sign = Signature.getInstance(SIGN_ALGORITHM);
			sign.initSign(getPrivateKey());
			
			try(ByteArrayInputStream in = new ByteArrayInputStream(dataToSign)) {
				byte[] buffer = new byte[2048];
				int len = 0;
				
				while((len = in.read(buffer)) != -1) {
					sign.update(buffer, 0, len);
				}				
				return sign.sign();
			}			
		} catch (Exception e) {
			throw new RegBaseUncheckedException(RegistrationExceptionConstants.NON_TPM_SIGN_ERROR.getErrorCode(),
					RegistrationExceptionConstants.NON_TPM_SIGN_ERROR.getErrorMessage(), e);
		}
	}

	@Override
	public boolean validateSignature(byte[] signature, byte[] actualData) {
		try {
			Signature sign = Signature.getInstance(SIGN_ALGORITHM);
			sign.initVerify(getPublicKey());
			
			try(ByteArrayInputStream in = new ByteArrayInputStream(actualData)) {
				byte[] buffer = new byte[2048];
				int len = 0;
				
				while((len = in.read(buffer)) != -1) {
					sign.update(buffer, 0, len);
				}				
				return sign.verify(signature);
			}			
		} catch (Exception e) {
			throw new RegBaseUncheckedException(RegistrationExceptionConstants.NON_TPM_SIGN_ERROR.getErrorCode(),
					RegistrationExceptionConstants.NON_TPM_SIGN_ERROR.getErrorMessage(), e);
		}
	}

	@Override
	public byte[] asymmetricEncrypt(byte[] plainData) {
		try {
			return MosipEncryptor.asymmetricPublicEncrypt(getPublicKey().getEncoded(), plainData, 
					MosipSecurityMethod.RSA_WITH_PKCS1PADDING);
		} catch(Exception e) {
			throw new RegBaseUncheckedException(RegistrationExceptionConstants.NON_TPM_ASYMMETRIC_ENCRYPT_ERROR.getErrorCode(),
					RegistrationExceptionConstants.NON_TPM_ASYMMETRIC_ENCRYPT_ERROR.getErrorMessage(), e);
		}		
	}

	@Override
	public byte[] asymmetricDecrypt(byte[] cipher) {
		try {
			return MosipDecryptor.asymmetricPrivateDecrypt(getPrivateKey().getEncoded(), cipher, 
					MosipSecurityMethod.RSA_WITH_PKCS1PADDING);
		} catch(Exception e) {
			throw new RegBaseUncheckedException(RegistrationExceptionConstants.NON_TPM_ASYMMETRIC_DECRYPT_ERROR.getErrorCode(),
					RegistrationExceptionConstants.NON_TPM_ASYMMETRIC_DECRYPT_ERROR.getErrorMessage(), e);
		}
	}

	@Override
	public byte[] getSigningPublicPart() {
		try {
			return getPublicKey().getEncoded();
		} catch (Exception e) {
			throw new RegBaseUncheckedException(RegistrationExceptionConstants.NON_TPM_GET_SIGN_KEY_ERROR.getErrorCode(),
					RegistrationExceptionConstants.NON_TPM_GET_SIGN_KEY_ERROR.getErrorMessage(), e);
		}
	}

	@Override
	public void closeSecurityInstance() throws RegBaseCheckedException {
		LOGGER.info(LoggerConstants.LOCAL_CLIENT_SECURITY_IMPL, RegistrationConstants.APPLICATION_ID,
				RegistrationConstants.APPLICATION_NAME, "Nothing to do, as Local NON-TPM Security Impl is in use");
	}
	
	private void setupKeysDir() {
		File keysDir = new File(getKeysDirPath());
		keysDir.mkdirs();
	}
	
	private boolean doesKeyExists() {
		File keysDir = new File(getKeysDirPath());
		if(keysDir.exists() && keysDir.list().length >= 2) {
			return true;
		}
		return false;
	}
	
	private String getKeysDirPath() {
		return KEY_PATH + File.separator + KEYS_DIR;
	}
	
	private void createKeyFile(String fileName, byte[] key) {
		try(FileOutputStream os = 
				new FileOutputStream(getKeysDirPath() + File.separator + fileName)) {
			os.write(key);
		} catch (IOException e) {
			LOGGER.error(LoggerConstants.LOCAL_CLIENT_SECURITY_IMPL, RegistrationConstants.APPLICATION_ID,
					RegistrationConstants.APPLICATION_NAME, ExceptionUtils.getStackTrace(e));
		}
	}
	
	private PrivateKey getPrivateKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] key = Files.readAllBytes(Paths.get(getKeysDirPath() + File.separator + PRIVATE_KEY));
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(key);
		KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
		return kf.generatePrivate(keySpec);
	}
	
	private PublicKey getPublicKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] key = Files.readAllBytes(Paths.get(getKeysDirPath() + File.separator + PUBLIC_KEY));
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(key);
		KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
		return kf.generatePublic(keySpec);
	}
	
	private void createReadMe(PublicKey publicKey) throws IOException {
		StringBuilder builder = new StringBuilder();
		builder.append("MachineName: ");		
		builder.append(InetAddress.getLocalHost().getHostName().toLowerCase());
		builder.append("\r\n");
		builder.append("PublicKey: ");		
		builder.append(CryptoUtil.encodeBase64String(publicKey.getEncoded()));
		builder.append("\r\n");
		builder.append("KeyIndex: ");
		builder.append(CryptoUtil.computeFingerPrint(publicKey.getEncoded(), null).toLowerCase());
		builder.append("\r\n");
		builder.append("Note : Use the above public key and machine name to create machine using admin API");
		builder.append("\r\n");
		builder.append("Note : If the keys are lost/deleted, keys are regenerated on next launch of reg-cli. Machine entry in server has to be deactivated and recreated once again.");
		builder.append("\r\n");
		
		Files.write(Paths.get(getKeysDirPath() + File.separator + README), builder.toString().getBytes(StandardCharsets.UTF_8), 
				StandardOpenOption.CREATE);	
	}

}
