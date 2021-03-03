package io.mosip.registration.context;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.springframework.beans.factory.annotation.Value;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dto.AuthTokenDTO;
import io.mosip.registration.entity.id.RegCenterUserId;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;

/**
 * This class will load all the property files as bundles All application level
 * details will be loaded in a map
 * 
 * @author Taleev Aalam
 *
 */
public class ApplicationContext {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(ApplicationContext.class);

	/** The application context. */
	private static ApplicationContext applicationContext;

	/** The application map. */
	private static Map<String, Object> applicationMap = new HashMap<>();

	/** The application languge. */
	private String applicationLanguge;

	@Value("${mosip.mandatory-languages}")
	private String mandatoryLanguages;

	@Value("${mosip.optional-languages}")
	private String optionalLanguages;

	private static Map<String, ResourceBundle> resourceBundleMap = new HashMap<>();

	/**
	 * Checks if is primary language right to left.
	 *
	 * @return true, if is primary language right to left
	 */
	public boolean isPrimaryLanguageRightToLeft() {

		return isLanguageRightToLeft(applicationLanguge);
	}

	/**
	 * Checks if given language is right to left.
	 *
	 * @return true, if is language right to left
	 */
	public boolean isLanguageRightToLeft(String langCode) {
		String rightToLeft = (String) applicationContext.getApplicationMap().get("mosip.right_to_left_orientation");
		if (null != rightToLeft && rightToLeft.contains(langCode)) {
			return true;
		}
		return false;
	}

	/** The auth token DTO. */
	private AuthTokenDTO authTokenDTO;

	/**
	 * Instantiates a new application context.
	 */
	private ApplicationContext() {

	}

	/**
	 * here we will load the property files such as labels, messages and validation.
	 * 
	 * </P>
	 * <p>
	 * Based on those languages these property files will be loaded.
	 * </p>
	 * 
	 * @return
	 * 
	 * 
	 */
	public void loadResourceBundle() {
		try {
			if (applicationLanguge == null) {
				List<String> langList = Arrays
						.asList(((mandatoryLanguages != null ? mandatoryLanguages : RegistrationConstants.EMPTY)
								.concat(RegistrationConstants.COMMA)
								.concat((optionalLanguages != null ? optionalLanguages : RegistrationConstants.EMPTY)))
										.split(RegistrationConstants.COMMA));

				if (null != langList && !langList.isEmpty()) {

					setApplicationLanguage(langList.stream().filter(langCode -> !langCode.isBlank()).findFirst().get());
					for (String langCode : langList) {
						if (!langCode.isBlank()) {
							String labelLangCodeKey = String.format("%s_%s", langCode, RegistrationConstants.LABELS);
							Locale locale = new Locale(langCode != null ? langCode.substring(0, 2) : "");
							resourceBundleMap.put(labelLangCodeKey,
									ResourceBundle.getBundle(RegistrationConstants.LABELS, locale));
							String messageLangCodeKey = String.format("%s_%s", langCode,
									RegistrationConstants.MESSAGES);
							resourceBundleMap.put(messageLangCodeKey,
									ResourceBundle.getBundle(RegistrationConstants.MESSAGES, locale));
						}
					}
				}

			}
		} catch (RuntimeException exception) {
			LOGGER.error("Application Context", RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, exception.getMessage());
		}
	}

	/**
	 * @param langCode   language code
	 * @param bundleType messages or labels
	 * @return Resource Bundle
	 */
	public ResourceBundle getBundle(String langCode, String bundleType) {

		return resourceBundleMap.get(String.format("%s_%s", langCode, bundleType));

	}

	public void setApplicationLanguage(String applicationLanguage) {
		this.applicationLanguge = applicationLanguage;

	}

	/**
	 * Gets the single instance of ApplicationContext.
	 *
	 * @return single instance of ApplicationContext
	 */
	public static ApplicationContext getInstance() {
		if (applicationContext == null) {
			applicationContext = new ApplicationContext();
			applicationContext.authTokenDTO = new AuthTokenDTO();
			return applicationContext;
		} else {
			return applicationContext;
		}
	}

	/**
	 * Map.
	 *
	 * @return the map
	 */
	public static Map<String, Object> map() {
		return applicationContext.getApplicationMap();
	}

	/**
	 * Application language.
	 *
	 * @return the string
	 */
	public static String applicationLanguage() {
		return applicationContext.getApplicationLanguage();
	}

	/**
	 * Load resources.
	 */
	public static void loadResources() {
		applicationContext.loadResourceBundle();
	}

	/**
	 * Sets the auth token DTO.
	 *
	 * @param authTokenDTO the new auth token DTO
	 */
	public static void setAuthTokenDTO(AuthTokenDTO authTokenDTO) {
		applicationContext.authTokenDTO = authTokenDTO;
	}

	/**
	 * Auth token DTO.
	 *
	 * @return the auth token DTO
	 */
	public static AuthTokenDTO authTokenDTO() {
		return applicationContext.authTokenDTO;
	}

	/**
	 * Gets the application map.
	 *
	 * @return the applicationMap
	 */
	public Map<String, Object> getApplicationMap() {
		return applicationMap;
	}

	/**
	 * Sets the application map.
	 *
	 * @param applicationMap the applicationMap to set
	 */
	public static void setApplicationMap(Map<String, Object> applicationMap) {
		ApplicationContext.applicationMap.putAll(applicationMap);
	}

	/**
	 * Get application language.
	 *
	 * @return the application language
	 */
	public String getApplicationLanguage() {
		return applicationLanguge;
	}

	/**
	 * Sets the global config value of.
	 *
	 * @param code the code
	 * @param val  the val
	 */
	public static void setGlobalConfigValueOf(String code, String val) {
		applicationMap.put(code, val);
	}

	/**
	 * Removes the global config value of.
	 *
	 * @param code the code
	 */
	public static void removeGlobalConfigValueOf(String code) {
		applicationMap.remove(code);

	}

	/**
	 * Gets the integer value.
	 *
	 * @param code the code
	 * @return the integer value
	 */
	public static int getIntValueFromApplicationMap(String code) {

		return Integer.parseInt((String) applicationMap.get(code));

	}

	public static String getStringValueFromApplicationMap(String code) {

		return String.valueOf(applicationMap.get(code));

	}

	public static void setUpgradeServerURL(String url) {
		applicationMap.put("client.upgrade.server.url", url);
	}

	@Deprecated(since = "1.1.4")
	public static void setTPMUsageFlag(String tpmUsageFlag) {
		applicationMap.put("client.tpm.required", tpmUsageFlag);
	}

	public static String getUpgradeServerURL() {
		return applicationMap.get("client.upgrade.server.url") == null
				? String.format("https://%s", RegistrationAppHealthCheckUtil.getHostName())
				: String.valueOf(applicationMap.get("client.upgrade.server.url"));
	}

	@Deprecated(since = "1.1.4")
	public static String getTPMUsageFlag() {
		return applicationMap.get("client.tpm.required") == null ? "Y"
				: String.valueOf(applicationMap.get("client.tpm.required"));
	}

	public static String getDateFormat() {
		return applicationMap.get("mosip.default.date.format") == null ? "yyyy/MM/dd"
				: String.valueOf(applicationMap.get("mosip.default.date.format"));
	}

	public ResourceBundle getApplicationLanguageLabelBundle() {
		return getBundle(getApplicationLanguage(), RegistrationConstants.LABELS);
	}

	public ResourceBundle getApplicationLanguageMessagesBundle() {
		return getBundle(getApplicationLanguage(), RegistrationConstants.MESSAGES);
	}

}
