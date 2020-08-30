package io.mosip.registration.cipher;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidAlgorithmParameterException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

import io.mosip.kernel.core.crypto.exception.InvalidDataException;
import io.mosip.kernel.core.crypto.exception.InvalidKeyException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.crypto.jce.constant.SecurityExceptionCodeConstant;
import io.mosip.kernel.crypto.jce.util.CryptoUtils;
import io.mosip.registration.config.SoftwareInstallationHandler;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.tpm.asymmetric.AsymmetricDecryptionService;
import io.mosip.registration.tpm.asymmetric.AsymmetricEncryptionService;
import io.mosip.registration.tpm.initialize.TPMInitialization;
import io.mosip.registration.util.LoggerFactory;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Decryption the Client Jar with Symmetric Key
 * 
 * @author Omsai Eswar M.
 *
 */
public class ClientJarDecryption extends Application {

	private static final String SLASH = "/";
	private static final String AES_ALGORITHM = "AES";
	private static final String MOSIP_CLIENT = "mosip-client.jar";
	private static final String MOSIP_SERVICES = "mosip-services.jar";
	private static final String MOSIP_PACKET_MANAGER = "mosip-packet-manager.jar";
	private static String libFolder = "lib/";
	private static String binFolder = "bin/";
	private static final String MOSIP_REGISTRATION_DB_KEY = "mosip.reg.db.key";
	private static final String MOSIP_REGISTRATION_HC_URL = "mosip.reg.healthcheck.url";
	private static final String MOSIP_REGISTRATION_APP_KEY = "mosip.reg.app.key";
	private static final String ENCRYPTED_KEY = "mosip.registration.key.encrypted";
	private static final String IS_KEY_ENCRYPTED = "Y";
	private static final String MOSIP_CLIENT_TPM_AVAILABILITY = "mosip.reg.client.tpm.availability";

	private static final Logger LOGGER = LoggerFactory.getLogger(ClientJarDecryption.class);

	private ProgressIndicator progressIndicator = new ProgressIndicator();
	private Stage primaryStage = new Stage();

	static String tempPath;
	private AsymmetricEncryptionService asymmetricEncryptionService = new AsymmetricEncryptionService();
	private AsymmetricDecryptionService asymmetricDecryptionService = new AsymmetricDecryptionService();

	private String IS_TPM_AVAILABLE = "Checking TPM Avaialbility";
	private String ENCRYPT_PROPERTIES = "Encrypting Properties";
	private String DB_CHECK = "Checking for DB Availability";
	private String CHECKING_FOR_JARS = "Checking for jars";
	private String FAILED_TO_LAUNCH = "Failed To Launch";
	private String LAUNCHING_CLIENT = "Launching Mosip-Client";
	private String RE_CHECKING_FOR_JARS = "Re-Checking Jars";
	private String INSTALLING_JARS = "Installing Jars";
	private String TERMINATING_APPLICATION = "Terminating Application";
	private String DB_NOT_FOUND = "DB Not Found";

	private Label downloadLabel;
	private String MOSIP_SCREEN_LOADED = "Mosip client Screen loaded";

	private String EXCEPTION_ENCOUNTERED = "Exception encountered during context initialization";

	private String UNABLE_TO_DOWNLOAD_JARS = "Unable to Download Jars Due To Slow or No Internet";

	/**
	 * Decrypt the bytes
	 * 
	 * @param Jar
	 *            bytes
	 * @throws UnsupportedEncodingException
	 */
	public byte[] decrypt(byte[] data, byte[] encodedString) {

		LOGGER.info(LoggerConstants.CLIENT_JAR_DECRYPTION, LoggerConstants.APPLICATION_NAME,
				LoggerConstants.APPLICATION_ID, "Decryption Started");

		// Generate AES Session Key
		SecretKey symmetricKey = new SecretKeySpec(encodedString, AES_ALGORITHM);

		return symmetricDecrypt(symmetricKey, data, null);
	}

	/**
	 * Decrypt and save the file in temp directory
	 * 
	 * @param args
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InterruptedException
	 * @throws io.mosip.kernel.core.exception.IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public static void main(String[] args) {

		LOGGER.info(LoggerConstants.CLIENT_JAR_DECRYPTION, LoggerConstants.APPLICATION_NAME,
				LoggerConstants.APPLICATION_ID, "Started run.jar");

		// Launch Reg-Client and perform necessary actions
		launch(args);

	}

	@Override
	public void start(Stage stage) {

		LOGGER.info(LoggerConstants.CLIENT_JAR_DECRYPTION, LoggerConstants.APPLICATION_NAME,
				LoggerConstants.APPLICATION_ID, "Started JavaFx start");

		showDialog();

		executeVerificationTask();
	}

	private void executeVerificationTask() {
		ClientJarDecryption aesDecrypt = new ClientJarDecryption();

		LOGGER.info(LoggerConstants.CLIENT_JAR_DECRYPTION, LoggerConstants.APPLICATION_NAME,
				LoggerConstants.APPLICATION_ID, "Started loading properties of mosip-application.properties");

		try (InputStream keyStream = ClientJarDecryption.class.getClassLoader()
				.getResourceAsStream("props/mosip-application.properties")) {
			Properties properties = new Properties();
			properties.load(keyStream);
			try {

				// TODO Check Internet Connectivity

				Task<Boolean> verificationTask = new Task<Boolean>() {

					/*
					 * (non-Javadoc)
					 * 
					 * @see javafx.concurrent.Task#call()
					 */
					@Override
					protected Boolean call() throws IOException, InterruptedException {

						/*String dbpath = new File(System.getProperty("user.dir")) + SLASH
								+ properties.getProperty("mosip.reg.dbpath");

						updateMessage(DB_CHECK);

						boolean isDbAvailable = dbCheck(dbpath);

						if (isDbAvailable) {*/

							updateMessage(IS_TPM_AVAILABLE);

							// // Encrypt the Keys
							// boolean isTPMAvailable = isTPMAvailable(properties);
							//
							// if (isTPMAvailable) {
							// updateMessage(ENCRYPT_PROPERTIES);
							//
							// encryptRequiredProperties(properties, propsFilePath);
							// }

							try {
								LOGGER.info(LoggerConstants.CLIENT_JAR_DECRYPTION, LoggerConstants.APPLICATION_NAME,
										LoggerConstants.APPLICATION_ID, "Started check for jars task");

								LOGGER.info(LoggerConstants.CLIENT_JAR_DECRYPTION, LoggerConstants.APPLICATION_NAME,
										LoggerConstants.APPLICATION_ID, "Checking for jars started");

								SoftwareInstallationHandler registrationUpdate = new SoftwareInstallationHandler();

								boolean hasJars = false;

								updateMessage(CHECKING_FOR_JARS);

								if (registrationUpdate.getCurrentVersion() != null
										&& registrationUpdate.hasRequiredJars()) {

									hasJars = true;

								} else {
									LOGGER.info(LoggerConstants.CLIENT_JAR_DECRYPTION, LoggerConstants.APPLICATION_NAME,
											LoggerConstants.APPLICATION_ID, "Installing of jars started");

									updateMessage(INSTALLING_JARS);

									registrationUpdate.installJars();

									updateMessage(RE_CHECKING_FOR_JARS);

									hasJars = (registrationUpdate.getCurrentVersion() != null
											&& registrationUpdate.hasRequiredJars());
								}

								LOGGER.info(LoggerConstants.CLIENT_JAR_DECRYPTION, LoggerConstants.APPLICATION_NAME,
										LoggerConstants.APPLICATION_ID, "Checking for jars Completed");

								if (hasJars) {

									LOGGER.info(LoggerConstants.CLIENT_JAR_DECRYPTION, LoggerConstants.APPLICATION_NAME,
											LoggerConstants.APPLICATION_ID, "Found all the required jars");

									updateMessage(LAUNCHING_CLIENT);
									File encryptedClientJar = new File(binFolder + MOSIP_CLIENT);

									File encryptedServicesJar = new File(binFolder + MOSIP_SERVICES);

									//File encryptedPacketManagerJar = new File(binFolder + MOSIP_PACKET_MANAGER);

									tempPath = FileUtils.getTempDirectoryPath();
									tempPath = tempPath + UUID.randomUUID();

									byte[] decryptedRegFileBytes;
									try {

										byte[] decryptedKey = aesDecrypt.getValue(MOSIP_REGISTRATION_APP_KEY,
												properties, isTPMAvailable(properties));

										LOGGER.info(LoggerConstants.CLIENT_JAR_DECRYPTION,
												LoggerConstants.APPLICATION_NAME, LoggerConstants.APPLICATION_ID,
												"Decrypting mosip-client");

										decryptedRegFileBytes = aesDecrypt.decrypt(
												FileUtils.readFileToByteArray(encryptedClientJar), decryptedKey);

										String clientJar = tempPath + SLASH + UUID.randomUUID();

										// Decrypt Client Jar
										FileUtils.writeByteArrayToFile(new File(clientJar + ".jar"),
												decryptedRegFileBytes);

										LOGGER.info(LoggerConstants.CLIENT_JAR_DECRYPTION,
												LoggerConstants.APPLICATION_NAME, LoggerConstants.APPLICATION_ID,
												"Decrypting mosip-services");

										byte[] decryptedRegServiceBytes = aesDecrypt.decrypt(
												FileUtils.readFileToByteArray(encryptedServicesJar), decryptedKey);

										// Decrypt Services Jar
										FileUtils.writeByteArrayToFile(
												new File(tempPath + SLASH + UUID.randomUUID() + ".jar"),
												decryptedRegServiceBytes);

										/*byte[] decryptedPacketManagerBytes = aesDecrypt.decrypt(
												FileUtils.readFileToByteArray(encryptedPacketManagerJar), decryptedKey);

										// Decrypt Packet manager Jar
										FileUtils.writeByteArrayToFile(
												new File(tempPath + SLASH + UUID.randomUUID() + ".jar"),
												decryptedPacketManagerBytes);*/

									} catch (RuntimeException | IOException runtimeException) {

										updateMessage(FAILED_TO_LAUNCH);

										LOGGER.error(LoggerConstants.CLIENT_JAR_DECRYPTION,
												LoggerConstants.APPLICATION_NAME, LoggerConstants.APPLICATION_ID,
												runtimeException.getMessage()
														+ ExceptionUtils.getStackTrace(runtimeException));

										try {

											LOGGER.info(LoggerConstants.CLIENT_JAR_DECRYPTION,
													LoggerConstants.APPLICATION_NAME, LoggerConstants.APPLICATION_ID,
													"Deleting manifest file, and jars and decrypted files");

											FileUtils.deleteDirectory(new File(tempPath));
											FileUtils.forceDelete(new File("MANIFEST.MF"));
											new SoftwareInstallationHandler();
										} catch (IOException ioException) {

											LOGGER.error(LoggerConstants.CLIENT_JAR_DECRYPTION,
													LoggerConstants.APPLICATION_NAME, LoggerConstants.APPLICATION_ID,
													runtimeException.getMessage()
															+ ExceptionUtils.getStackTrace(runtimeException));

										}

										LOGGER.info(LoggerConstants.CLIENT_JAR_DECRYPTION,
												LoggerConstants.APPLICATION_NAME, LoggerConstants.APPLICATION_ID,
												"Terminating the application");

										updateMessage(TERMINATING_APPLICATION);

										generateAlertAndTerminate(FAILED_TO_LAUNCH);
									}

									try {

										updateMessage(LAUNCHING_CLIENT);

										// String libPath = "\"" + new File("lib").getAbsolutePath() + "\"";

										FileUtils.copyDirectory(new File("lib"), new File(tempPath));

										LOGGER.info(LoggerConstants.CLIENT_JAR_DECRYPTION,
												LoggerConstants.APPLICATION_NAME, LoggerConstants.APPLICATION_ID,
												"Preparing command to launch the reg-client");

										String jrepath = new File(System.getProperty("user.dir")) + SLASH + "jre/jre/bin/java";
										String cmd = jrepath + " -Dfile.encoding=UTF-8" + " -cp " + tempPath + "/*;"
												+ "/* io.mosip.registration.controller.Initialization";

										Process process = Runtime.getRuntime().exec(cmd);

										LOGGER.info(LoggerConstants.CLIENT_JAR_DECRYPTION,
												LoggerConstants.APPLICATION_NAME, LoggerConstants.APPLICATION_ID,
												"Proccess Initiated");

										try (BufferedReader inputStreamReader = new BufferedReader(
												new InputStreamReader(process.getInputStream()))) {

											String info;
											while ((info = inputStreamReader.readLine()) != null) {

												LOGGER.info(LoggerConstants.CLIENT_JAR_DECRYPTION,
														LoggerConstants.APPLICATION_NAME,
														LoggerConstants.APPLICATION_ID, info);

												if (info.contains(MOSIP_SCREEN_LOADED)) {

													closeStage();
													break;
												}

												if (info.contains(EXCEPTION_ENCOUNTERED)) {

													updateMessage(TERMINATING_APPLICATION);

													process.destroyForcibly();

													generateAlertAndTerminate(FAILED_TO_LAUNCH);
												}

											}
										}

										process.getInputStream().close();
										process.getOutputStream().close();
										process.getErrorStream().close();

										if (0 == process.waitFor()) {

											LOGGER.info(LoggerConstants.CLIENT_JAR_DECRYPTION,
													LoggerConstants.APPLICATION_NAME, LoggerConstants.APPLICATION_ID,
													"Started Destroying proccess of reg-client and force deleting the decrypted jars");

											process.destroyForcibly();

											FileUtils.forceDelete(new File(tempPath));

											LOGGER.info(LoggerConstants.CLIENT_JAR_DECRYPTION,
													LoggerConstants.APPLICATION_NAME, LoggerConstants.APPLICATION_ID,
													"Completed Destroying proccess of reg-client and force deleting the decrypted jars");

											exit();
										}
									} catch (RuntimeException | InterruptedException | IOException runtimeException) {
										LOGGER.error(LoggerConstants.CLIENT_JAR_DECRYPTION,
												LoggerConstants.APPLICATION_NAME, LoggerConstants.APPLICATION_ID,
												runtimeException.getMessage()
														+ ExceptionUtils.getStackTrace(runtimeException));

										updateMessage(FAILED_TO_LAUNCH);

										generateAlertAndTerminate(FAILED_TO_LAUNCH);

									}
								} else {

									LOGGER.info(LoggerConstants.CLIENT_JAR_DECRYPTION, LoggerConstants.APPLICATION_NAME,
											LoggerConstants.APPLICATION_ID,
											"Not installed Fully, closing mosip run.jar screen");

									updateMessage(FAILED_TO_LAUNCH);
									updateMessage(TERMINATING_APPLICATION);

									generateAlertAndTerminate(UNABLE_TO_DOWNLOAD_JARS);

								}

							} catch (io.mosip.kernel.core.exception.IOException | IOException exception) {
								LOGGER.error(LoggerConstants.CLIENT_JAR_DECRYPTION, LoggerConstants.APPLICATION_NAME,
										LoggerConstants.APPLICATION_ID,
										exception.getMessage() + ExceptionUtils.getStackTrace(exception));

								LOGGER.info(LoggerConstants.CLIENT_JAR_DECRYPTION, LoggerConstants.APPLICATION_NAME,
										LoggerConstants.APPLICATION_ID,
										"Not installed Fully, closing mosip run.jar screen");

								updateMessage(FAILED_TO_LAUNCH);
								updateMessage(TERMINATING_APPLICATION);

								generateAlertAndTerminate(UNABLE_TO_DOWNLOAD_JARS);

							}

						/*} else {

							LOGGER.error(LoggerConstants.CLIENT_JAR_DECRYPTION, LoggerConstants.APPLICATION_NAME,
									LoggerConstants.APPLICATION_ID, "DB Not Found, Terminating Application");

							updateMessage(DB_NOT_FOUND);

							updateMessage(TERMINATING_APPLICATION);

							generateAlertAndTerminate(DB_NOT_FOUND);

						}*/

						return false;

					}
				};
				verificationTask.messageProperty()
						.addListener((obs, oldMessage, newMessage) -> downloadLabel.setText(newMessage));

				new Thread(verificationTask).start();

			} catch (RuntimeException runtimeException) {
				LOGGER.error(LoggerConstants.CLIENT_JAR_DECRYPTION, LoggerConstants.APPLICATION_NAME,
						LoggerConstants.APPLICATION_ID,
						runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
				closeStage();

				exit();

			}
		} catch (IOException ioException) {
			LOGGER.error(LoggerConstants.CLIENT_JAR_DECRYPTION, LoggerConstants.APPLICATION_NAME,
					LoggerConstants.APPLICATION_ID,
					ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
			closeStage();

			exit();

		}
	}

	private boolean isTPMAvailable(Properties properties) {
		LOGGER.info(LoggerConstants.CLIENT_JAR_DECRYPTION, LoggerConstants.APPLICATION_NAME,
				LoggerConstants.APPLICATION_ID, "Started tpm availability check");

		return properties.containsKey(MOSIP_CLIENT_TPM_AVAILABILITY)
				&& String.valueOf(properties.get(MOSIP_CLIENT_TPM_AVAILABILITY)).equalsIgnoreCase(IS_KEY_ENCRYPTED);
	}

	private void showDialog() {

		LOGGER.info(LoggerConstants.CLIENT_JAR_DECRYPTION, LoggerConstants.APPLICATION_NAME,
				LoggerConstants.APPLICATION_ID, "Started Loading mosip run.jar screen");

		StackPane stackPane = new StackPane();
		VBox vBox = new VBox();
		HBox hBox = new HBox();
		InputStream ins = this.getClass().getResourceAsStream("/img/logo-final.png");
		ImageView imageView = new ImageView(new Image(ins));
		imageView.setFitHeight(50);
		imageView.setFitWidth(50);
		hBox.setMinSize(200, 400);
		hBox.getChildren().add(imageView);
		downloadLabel = new Label();
		vBox.setAlignment(Pos.CENTER_LEFT);
		vBox.getChildren().add(progressIndicator);
		vBox.getChildren().add(downloadLabel);

		hBox.getChildren().add(vBox);
		hBox.setAlignment(Pos.CENTER_LEFT);

		stackPane.getChildren().add(hBox);
		Scene scene = new Scene(stackPane, 200, 150);
		primaryStage.initStyle(StageStyle.UNDECORATED);
		primaryStage.setScene(scene);

		primaryStage.show();

		LOGGER.info(LoggerConstants.CLIENT_JAR_DECRYPTION, LoggerConstants.APPLICATION_NAME,
				LoggerConstants.APPLICATION_ID, "Completed Loading mosip run.jar screen");

	}

	private byte[] getValue(String key, Properties properties, boolean isTPMAvailable) {
		byte[] value = CryptoUtil.decodeBase64(properties.getProperty(key));
		if (isTPMAvailable) {
			value = asymmetricDecryptionService.decryptUsingTPM(TPMInitialization.getTPMInstance(), value);
		}
		return value;
	}

	// private void encryptRequiredProperties(Properties properties, String
	// propertiesFilePath) throws IOException {
	// if (!(properties.containsKey(ENCRYPTED_KEY)
	// && properties.getProperty(ENCRYPTED_KEY).equals(IS_KEY_ENCRYPTED))) {
	// try (OutputStream propertiesFile = new FileOutputStream(propertiesFilePath))
	// {
	// properties.put(ENCRYPTED_KEY, IS_KEY_ENCRYPTED);
	// properties.put(MOSIP_REGISTRATION_APP_KEY, getEncryptedValue(properties,
	// MOSIP_REGISTRATION_APP_KEY));
	// properties.put(MOSIP_REGISTRATION_DB_KEY, getEncryptedValue(properties,
	// MOSIP_REGISTRATION_DB_KEY));
	// properties.store(propertiesFile, "Updated");
	// propertiesFile.flush();
	// }
	// }
	// }

	private String getEncryptedValue(Properties properties, String key) {
		return CryptoUtil.encodeBase64String(asymmetricEncryptionService.encryptUsingTPM(
				TPMInitialization.getTPMInstance(), Base64.getDecoder().decode(properties.getProperty(key))));
	}

	private void closeStage() {

		Platform.runLater(() -> primaryStage.close());

	}

	private void exit() {
		System.exit(0);
	}

	private boolean dbCheck(String dbPath) {

		LOGGER.info(LoggerConstants.CLIENT_JAR_DECRYPTION, LoggerConstants.APPLICATION_NAME,
				LoggerConstants.APPLICATION_ID, "Started DB availability check at path : " + dbPath);

		return new File(dbPath).exists();

	}

	private void generateAlertAndTerminate(String message) {
		Platform.runLater(() -> {
			Alert alert = new Alert(AlertType.ERROR, message, ButtonType.OK);
			alert.showAndWait();

			closeStage();

			exit();
		});
	}

	private static byte[] symmetricDecrypt(SecretKey key, byte[] data, byte[] aad) {
		Objects.requireNonNull(key, SecurityExceptionCodeConstant.MOSIP_INVALID_KEY_EXCEPTION.getErrorMessage());
		CryptoUtils.verifyData(data);
		byte[] output = null;
		try {
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

			byte[] randomIV = Arrays.copyOfRange(data, data.length - cipher.getBlockSize(), data.length);
			SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), AES_ALGORITHM);
			GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, randomIV);
			cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
			if (aad != null && aad.length != 0) {
				cipher.updateAAD(aad);
			}
			output = doFinal(Arrays.copyOf(data, data.length - cipher.getBlockSize()), cipher);
		} catch (java.security.InvalidKeyException e) {
			throw new InvalidKeyException(SecurityExceptionCodeConstant.MOSIP_INVALID_KEY_EXCEPTION.getErrorCode(),
					SecurityExceptionCodeConstant.MOSIP_INVALID_KEY_EXCEPTION.getErrorMessage(), e);
		} catch (InvalidAlgorithmParameterException e) {
			throw new InvalidKeyException(
					SecurityExceptionCodeConstant.MOSIP_INVALID_PARAM_SPEC_EXCEPTION.getErrorCode(),
					SecurityExceptionCodeConstant.MOSIP_INVALID_PARAM_SPEC_EXCEPTION.getErrorMessage(), e);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new InvalidDataException(
					SecurityExceptionCodeConstant.MOSIP_INVALID_DATA_LENGTH_EXCEPTION.getErrorCode(),
					SecurityExceptionCodeConstant.MOSIP_INVALID_DATA_LENGTH_EXCEPTION.getErrorMessage(), e);
		} catch (java.security.NoSuchAlgorithmException noSuchAlgorithmException) {
			throw new InvalidKeyException(
					SecurityExceptionCodeConstant.MOSIP_NO_SUCH_ALGORITHM_EXCEPTION.getErrorCode(),
					SecurityExceptionCodeConstant.MOSIP_NO_SUCH_ALGORITHM_EXCEPTION.getErrorMessage(),
					noSuchAlgorithmException);
		} catch (NoSuchPaddingException noSuchPaddingException) {
			throw new InvalidKeyException("No Such Padding Exception", "No Such Padding Exception",
					noSuchPaddingException);

		}
		return output;
	}

	private static byte[] doFinal(byte[] data, Cipher cipher) {
		try {
			return cipher.doFinal(data);
		} catch (IllegalBlockSizeException e) {
			throw new InvalidDataException(
					SecurityExceptionCodeConstant.MOSIP_INVALID_DATA_SIZE_EXCEPTION.getErrorCode(), e.getMessage(), e);
		} catch (BadPaddingException e) {
			throw new InvalidDataException(
					SecurityExceptionCodeConstant.MOSIP_INVALID_ENCRYPTED_DATA_CORRUPT_EXCEPTION.getErrorCode(),
					e.getMessage(), e);
		}
	}
}