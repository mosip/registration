package io.mosip.registration.context;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.springframework.beans.factory.annotation.Value;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dto.AuthTokenDTO;
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

	/** The application language bundle. */
	private ResourceBundle applicationLanguageBundle;

	/** The application messages bundle. */
	private ResourceBundle applicationMessagesBundle;

	/** The application map. */
	private static Map<String, Object> applicationMap = new HashMap<>();

	/** The application languge. */
	private String applicationLanguge;
	
	@Value("${mosip.mandatory-languages}")
	private String mandatoryLanguages;
	
	@Value("${mosip.optional-languages}")
	private String optionalLanguages;
	
	private String LABELS = "labels";
	private String MESSAGES = "messages";
	private static Map<String, ResourceBundle> resourceBundleMap = new HashMap<>();

	/**
	 * Checks if is primary language right to left.
	 *
	 * @return true, if is primary language right to left
	 */
	public boolean isPrimaryLanguageRightToLeft() {
		String rightToLeft = (String) applicationContext.getApplicationMap().get("mosip.right_to_left_orientation");
		if (null != rightToLeft && rightToLeft.contains(applicationLanguge)) {
			return true;
		}
		return false;
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
	 * <p>
	 * If we get primary and secondary languages
	 * </P>
	 * <p>
	 * Based on those languages these property files will be loaded.
	 * </p>
	 * <p>
	 * If we dont get primary and secondary languages
	 * </p>
	 * <p>
	 * Then the primary language will be English and the Secondary language will be
	 * Arabic by default and the property files will be loaded based on that
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
				for (String langCode : langList) {
					if (!langCode.isBlank()) {
						String labelLangCodeKey = String.format("%s_%s", langCode, LABELS);
						Locale locale = new Locale(langCode != null ? langCode.substring(0, 2) : "");
						resourceBundleMap.put(labelLangCodeKey, ResourceBundle.getBundle(LABELS, locale));
						String messageLangCodeKey = String.format("%s_%s", langCode, MESSAGES);
						resourceBundleMap.put(messageLangCodeKey, ResourceBundle.getBundle(MESSAGES, locale));
					}
				}

				if (null != langList && !langList.isEmpty()) {
					applicationLanguge = langList.stream().filter(langCode -> !langCode.isBlank()).findFirst().get();
				} else {
					applicationLanguge = Locale.getDefault().getDisplayLanguage() != null
							? Locale.getDefault().getDisplayLanguage().toLowerCase().substring(0, 3)
							: "eng";
				}
				
				String languageSupport = (String) applicationMap.get(RegistrationConstants.LANGUAGE_SUPPORT);
				if (StringUtils.isNotBlank(languageSupport)) {
					languageSupport = languageSupport.toLowerCase();
					applicationLanguge = applicationLanguge.toLowerCase();
					applicationLanguge = languageSupport.contains(applicationLanguge) ? applicationLanguge : "eng";
				}

				Locale applicationLanguageLocale = new Locale(
						applicationLanguge != null ? applicationLanguge.substring(0, 2) : "");

				applicationLanguageBundle = ResourceBundle.getBundle("labels", applicationLanguageLocale);
				applicationMessagesBundle = ResourceBundle.getBundle("messages", applicationLanguageLocale);
			}
		} catch (RuntimeException exception) {
			LOGGER.error("Application Context", RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, exception.getMessage());
		}
	}
	
	public void setApplicationLanguage(String applicationLanguage) {
		this.applicationLanguge = applicationLanguage;
		Locale applicationLanguageLocale = new Locale(
				applicationLanguge != null ? applicationLanguge.substring(0, 2) : "");

		applicationLanguageBundle = ResourceBundle.getBundle("labels", applicationLanguageLocale);
		applicationMessagesBundle = ResourceBundle.getBundle("messages", applicationLanguageLocale);
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
	 * Secondary language local.
	 *
	 * @return the string
	 */
	/*
	 * To return the local language code with two letter
	 */
	public static String secondaryLanguageLocal() {

		if (applicationContext.getLocalLanguage() != null && applicationContext.getLocalLanguage().length() > 2) {
			return applicationContext.getLocalLanguage().substring(0, 2);
		}
		return null;
	}

	/**
	 * Primary language local.
	 *
	 * @return the string
	 */
	/*
	 * To return the application language code with two letter
	 */
	public static String primaryLanguageLocal() {
		return applicationContext.getApplicationLanguage().substring(0, 2);
	}
	
	/**
	 * Application language bundle.
	 *
	 * @return the resource bundle
	 */
	public static ResourceBundle applicationLanguageBundle() {
		return applicationContext.getApplicationLanguageBundle();
	}
	
	/**
	 * Application messages bundle.
	 *
	 * @return the resource bundle
	 */
	public static ResourceBundle applicationMessagesBundle() {
		return applicationContext.getApplicationMessagesBundle();
	}
	
	//TODO - remove all local language related methods
	public static ResourceBundle localLanguageProperty() {
		return applicationContext.getLocalLanguageProperty();
	}
	
	public ResourceBundle getLocalLanguageProperty() {
		return localLanguageBundle();
	}

	public static String localLanguage() {
		return applicationContext.getLocalLanguage();
	}

	public static ResourceBundle localLanguageBundle() {
		return resourceBundleMap.get("ara_labels");
	}

	public static ResourceBundle localLanguageValidationBundle() {
		return applicationContext.getLocalMessagesBundle();
	}

	public static ResourceBundle localMessagesBundle() {
		return applicationContext.getLocalMessagesBundle();
	}
	
	public String getLocalLanguage() {
		return "ara";
	}
	
	public ResourceBundle getLocalMessagesBundle() {
		return resourceBundleMap.get("ara_messages");
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
	 * Gets the application language bundle.
	 *
	 * @return the application language bundle
	 */
	public ResourceBundle getApplicationLanguageBundle() {
		return applicationLanguageBundle;
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
	 * Gets the application messages bundle.
	 *
	 * @return the applicationMessagesBundle
	 */
	public ResourceBundle getApplicationMessagesBundle() {
		return applicationMessagesBundle;
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
		return applicationMap.get("client.upgrade.server.url") == null ?
				String.format("https://%s", RegistrationAppHealthCheckUtil.getHostName()) :
				String.valueOf(applicationMap.get("client.upgrade.server.url"));
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
}
