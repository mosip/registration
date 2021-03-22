package io.mosip.registration.util.common;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.controller.reg.Validations;
import io.mosip.registration.dto.UiSchemaDTO;

/**
 * This class will give the Page Flow
 * 
 * @author Sravya Surampalli
 *
 */
@Component
public class PageFlow {

	/**
	 * Instance of LOGGER
	 */
	private static final Logger LOGGER = AppConfig.getLogger(PageFlow.class);

	private static Map<String, Map<String, Boolean>> regPageFlowMap;

	private static Map<String, Map<String, Boolean>> onBoardingPageFlowMap;

	@Autowired
	private Validations validations;

	/**
	 * This method sets the initial page flow for all the functionalities like New
	 * Registration, On-boarding, UIN Update.
	 * 
	 * <p>
	 * The page flow will be stored in a map, the page name as key and its
	 * visibility status (true/false) for a particular functionality as value.
	 * </p>
	 * 
	 * <p>
	 * After updating the maps with the page names and their visibility statuses,
	 * these maps will be stored in {@link ApplicationContext} so that they can be
	 * accessed from anywhere.
	 * </p>
	 */

	public void loadPageFlow() {

		LOGGER.info(LoggerConstants.LOG_REG_PAGE_FLOW, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID,
				"Preparing Page flow map for New Registration, Onboard, UIN Update");

		Map<String, Map<String, Boolean>> registrationMap = new LinkedHashMap<>();
		Map<String, Map<String, Boolean>> onboardMap = new LinkedHashMap<>();

		Map<String, Boolean> onboardUserParent = new LinkedHashMap<>();
		onboardUserParent.put(RegistrationConstants.VISIBILITY, true);
		onboardMap.put(RegistrationConstants.ONBOARD_USER_PARENT, onboardUserParent);

		Map<String, Boolean> demographicMap = new LinkedHashMap<>();
		demographicMap.put(RegistrationConstants.VISIBILITY, true);
		registrationMap.put(RegistrationConstants.DEMOGRAPHIC_DETAIL, demographicMap);

		String docType = "documentType";
		List<UiSchemaDTO> docList = null;
		if(validations != null && validations.getValidationMap() != null && !validations.getValidationMap().isEmpty()) {
			docList = validations.getValidationMap().values().stream()
					.filter(schemaDto -> schemaDto.getType() != null && schemaDto.getType().equalsIgnoreCase(docType))
					.collect(Collectors.toList());
		}

		Map<String, Boolean> docMap = new LinkedHashMap<>();
		docMap.put(RegistrationConstants.VISIBILITY, docList != null && !docList.isEmpty());
		docMap.put(RegistrationConstants.DOCUMENT_PANE, true);
		docMap.put(RegistrationConstants.EXCEPTION_PANE, true);
		registrationMap.put(RegistrationConstants.DOCUMENT_SCAN, docMap);

		Map<String, Boolean> guardianBioMap = new LinkedHashMap<>();
		guardianBioMap.put(RegistrationConstants.VISIBILITY, true);
		registrationMap.put(RegistrationConstants.GUARDIAN_BIOMETRIC, guardianBioMap);
		onboardMap.put(RegistrationConstants.GUARDIAN_BIOMETRIC, guardianBioMap);

		Map<String, Boolean> onBoardSuccessMap = new LinkedHashMap<>();
		onBoardSuccessMap.put(RegistrationConstants.VISIBILITY, true);
		onboardMap.put(RegistrationConstants.ONBOARD_USER_SUCCESS, onBoardSuccessMap);

		// updateRegMap(onboardMap, RegistrationConstants.ONBOARD);

		registrationPageFlow(registrationMap, RegistrationConstants.APPLICATION_NAME);
		Map<String, Boolean> previewMap = new LinkedHashMap<>();
		previewMap.put(RegistrationConstants.VISIBILITY, true);
		registrationMap.put(RegistrationConstants.REGISTRATION_PREVIEW, previewMap);

		Map<String, Boolean> authMap = new LinkedHashMap<>();
		authMap.put(RegistrationConstants.VISIBILITY, true);
		registrationMap.put(RegistrationConstants.OPERATOR_AUTHENTICATION, authMap);

		setOnBoardingMap(onboardMap);

		setRegMap(registrationMap);

		LOGGER.info(LoggerConstants.LOG_REG_PAGE_FLOW, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Updating Map and storing in Application Context");

	}

	private void registrationPageFlow(Map<String, Map<String, Boolean>> registrationMap, String page) {

		if (page.equalsIgnoreCase(RegistrationConstants.APPLICATION_NAME)) {
			updateDetailMap(registrationMap,
					String.valueOf(ApplicationContext.map().get(RegistrationConstants.DOC_DISABLE_FLAG)),
					RegistrationConstants.DOCUMENT_PANE, RegistrationConstants.DOCUMENT_SCAN, "");

		}

		LOGGER.info(LoggerConstants.LOG_REG_PAGE_FLOW, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Map values are updated based on Configuration");

	}

	private void updateDetailMap(Map<String, Map<String, Boolean>> detailMap, String flagVal, String pageId,
			String subPane, String childId) {

		LOGGER.info(LoggerConstants.LOG_REG_PAGE_FLOW, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Updating Visibility values based on Configuration");

		if (RegistrationConstants.DISABLE.equalsIgnoreCase(flagVal)) {

			if (pageId.equals(RegistrationConstants.DOCUMENT_PANE)) {
				detailMap.get(subPane).put(pageId, false);
			} else {
				detailMap.get(pageId).put(RegistrationConstants.VISIBILITY, false);

				if (!subPane.isEmpty()) {
					detailMap.get(subPane).put(childId, false);
				}
			}
		}

		LOGGER.info(LoggerConstants.LOG_REG_PAGE_FLOW, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Visibility values updated based on Configuration");
	}

	public boolean hasNextPage(String currentPage, List<String> pageList) {
		boolean isAvailabe = false;

		if (pageList.contains(currentPage)) {

			int currentPageIndex = pageList.indexOf(currentPage);

			isAvailabe = pageList.get(currentPageIndex + 1) != null;
		}

		return isAvailabe;
	}

	public boolean hasPreviousPage(String currentPage, List<String> pageList) {
		boolean isAvailabe = false;

		if (pageList.contains(currentPage)) {

			int currentPageIndex = pageList.indexOf(currentPage);

			isAvailabe = pageList.get(currentPageIndex - 1) != null;
		}

		return isAvailabe;
	}

	public String getPreviousPage(String currentPage, List<String> pageList) {
		String previousPage = "";

		if (pageList != null && !pageList.isEmpty() && hasPreviousPage(currentPage, pageList)) {
			previousPage = pageList.get((pageList.indexOf(currentPage)) - 1);
		}
		return previousPage;
	}

	public String getNextPage(String currentPage, List<String> pageList) {
		String nextPage = null;

		if (pageList != null && !pageList.isEmpty() && hasNextPage(currentPage, pageList)) {

			nextPage = pageList.get((pageList.indexOf(currentPage)) + 1);
		}

		return nextPage;
	}

	/**
	 * @param currentPage
	 *            registration page name
	 * @return returns registration next page name if current page and next page
	 *         found, else null
	 */
	public String getNextRegPage(String currentPage) {

		// Get Visible Registration Pages
		List<String> pageList = getVisiblePages(getRegMap());

		// Get Registration pages
		return getNextPage(currentPage, pageList);
	}

	/**
	 * @param currentPage
	 *            registration page name
	 * @return returns registration previous page name if current page and previous
	 *         page found, else null
	 */
	public String getPreviousRegPage(String currentPage) {
		// Get Visible Registration Pages
		List<String> pageList = getVisiblePages(getRegMap());

		// Get previous page
		return getPreviousPage(currentPage, pageList);
	}

	public String getNextOnboardPage(String currentPage) {

		// Get Visible onboard pages
		List<String> pageList = getVisiblePages(getOnBoardingMap());

		// Get next page
		return getNextPage(currentPage, pageList);
	}

	public String getPreviousOnboardPage(String currentPage) {

		// Get Visible onboard pages
		List<String> pageList = getVisiblePages(getOnBoardingMap());

		// Get previous page
		return getPreviousPage(currentPage, pageList);
	}

	private List<String> getVisiblePages(Map<String, Map<String, Boolean>> pageMap) {

		LinkedList<String> pageList = new LinkedList<>();

		if (pageMap != null && !pageMap.isEmpty()) {
			for (Map.Entry<String, Map<String, Boolean>> entry : pageMap.entrySet()) {

				if (entry.getValue().get(RegistrationConstants.VISIBILITY)) {
					pageList.add(entry.getKey());
				}
			}
		}

		return pageList;
	}

	public Map<String, Map<String, Boolean>> getRegMap() {
		return regPageFlowMap;
	}

	public void setRegMap(Map<String, Map<String, Boolean>> regMap) {
		PageFlow.regPageFlowMap = regMap;
	}

	public Map<String, Map<String, Boolean>> getOnBoardingMap() {
		return onBoardingPageFlowMap;
	}

	public void setOnBoardingMap(Map<String, Map<String, Boolean>> onBoardingMap) {
		PageFlow.onBoardingPageFlowMap = onBoardingMap;
	}

	/**
	 * @param page
	 *            page name
	 * @param key
	 *            to find attributes such as visibility
	 * @param val
	 *            boolean value to say true or false
	 */
	public void updateOnBoardingMap(String page, String key, boolean val) {

		if (onBoardingPageFlowMap.get(page) == null) {

			// If not exists create and update
			onBoardingPageFlowMap.put(page, new LinkedHashMap<>());
		}
		PageFlow.onBoardingPageFlowMap.get(page).put(key, val);
	}

	/**
	 * @param page
	 *            page name
	 * @param key
	 *            to find attributes such as visibility
	 * @param val
	 *            boolean value to say true or false
	 */
	public void updateRegMap(String page, String key, boolean val) {

		if (regPageFlowMap.get(page) == null) {

			// If not exists create and update
			regPageFlowMap.put(page, new LinkedHashMap<>());
		}
		PageFlow.regPageFlowMap.get(page).put(key, val);
	}

	/**
	 * @param page
	 *            Registration page Name
	 * @param attribute
	 *            attribute in the Registration page
	 * @return returns whether visible or not
	 */
	public boolean isVisibleInRegFlowMap(String page, String attribute) {
		boolean isVisible = false;

		if (regPageFlowMap.get(page) != null) {

			isVisible = regPageFlowMap.get(page).get(attribute);

		}

		return isVisible;
	}

	/**
	 * @param page
	 *            registration page Name
	 * @param attribute
	 *            attribute in the onBoard page
	 * @return returns whether visible or not
	 */
	public boolean isVisibleInOnBoardFlowMap(String page, String attribute) {
		boolean isVisible = false;

		if (onBoardingPageFlowMap.get(page) != null) {

			isVisible = onBoardingPageFlowMap.get(page).get(attribute);

		}

		return isVisible;
	}
}
