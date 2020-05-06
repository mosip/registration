package io.mosip.registration.util.common;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ io.mosip.registration.context.ApplicationContext.class })
public class PageFlowTest {

	private PageFlow pageFlow = new PageFlow();

	@Test
	public void testGetInitialPageDetails() {
		PowerMockito.mockStatic(ApplicationContext.class);
		Map<String, Object> map = new HashMap<>();
		map.put(RegistrationConstants.FINGERPRINT_DISABLE_FLAG, "n");
		map.put(RegistrationConstants.IRIS_DISABLE_FLAG, "n");
		map.put(RegistrationConstants.FACE_DISABLE_FLAG, "n");
		map.put(RegistrationConstants.DOC_DISABLE_FLAG, "n");
		when(ApplicationContext.map()).thenReturn(map);

		pageFlow = new PageFlow();
		pageFlow.loadPageFlow();
	}

	@Before
	public void loadPageDetails() {

		Map<String, Map<String, Boolean>> registrationMap = new LinkedHashMap<>();
		Map<String, Map<String, Boolean>> onboardMap = new LinkedHashMap<>();

		Map<String, Boolean> onboardUserParent = new LinkedHashMap<>();
		onboardUserParent.put(RegistrationConstants.VISIBILITY, true);
		onboardMap.put(RegistrationConstants.ONBOARD_USER_PARENT, onboardUserParent);

		Map<String, Boolean> demographicMap = new LinkedHashMap<>();
		demographicMap.put(RegistrationConstants.VISIBILITY, true);
		registrationMap.put(RegistrationConstants.DEMOGRAPHIC_DETAIL, demographicMap);

		Map<String, Boolean> docMap = new LinkedHashMap<>();
		docMap.put(RegistrationConstants.VISIBILITY, true);
		docMap.put(RegistrationConstants.DOCUMENT_PANE, true);
		docMap.put(RegistrationConstants.EXCEPTION_PANE, true);
		registrationMap.put(RegistrationConstants.DOCUMENT_SCAN, docMap);

		Map<String, Boolean> exceptionMap = new LinkedHashMap<>();
		exceptionMap.put(RegistrationConstants.VISIBILITY, true);
		exceptionMap.put(RegistrationConstants.FINGER_PANE, true);
		exceptionMap.put(RegistrationConstants.IRIS_PANE, true);
		registrationMap.put(RegistrationConstants.BIOMETRIC_EXCEPTION, exceptionMap);
		onboardMap.put(RegistrationConstants.BIOMETRIC_EXCEPTION, exceptionMap);

		Map<String, Boolean> guardianBioMap = new LinkedHashMap<>();
		guardianBioMap.put(RegistrationConstants.VISIBILITY, false);
		registrationMap.put(RegistrationConstants.GUARDIAN_BIOMETRIC, guardianBioMap);

		Map<String, Boolean> fingerPrintMap = new LinkedHashMap<>();
		fingerPrintMap.put(RegistrationConstants.VISIBILITY, true);
		onboardMap.put(RegistrationConstants.FINGERPRINT_CAPTURE, fingerPrintMap);

		Map<String, Boolean> irisMap = new LinkedHashMap<>();
		irisMap.put(RegistrationConstants.VISIBILITY, true);
		onboardMap.put(RegistrationConstants.IRIS_CAPTURE, irisMap);

		Map<String, Boolean> faceMap = new LinkedHashMap<>();
		faceMap.put(RegistrationConstants.VISIBILITY, true);
		onboardMap.put(RegistrationConstants.FACE_CAPTURE, faceMap);

		Map<String, Boolean> onBoardSuccessMap = new LinkedHashMap<>();
		onBoardSuccessMap.put(RegistrationConstants.VISIBILITY, true);
		onboardMap.put(RegistrationConstants.ONBOARD_USER_SUCCESS, onBoardSuccessMap);

		Map<String, Boolean> previewMap = new LinkedHashMap<>();
		previewMap.put(RegistrationConstants.VISIBILITY, true);
		registrationMap.put(RegistrationConstants.REGISTRATION_PREVIEW, previewMap);

		Map<String, Boolean> authMap = new LinkedHashMap<>();
		authMap.put(RegistrationConstants.VISIBILITY, true);
		registrationMap.put(RegistrationConstants.OPERATOR_AUTHENTICATION, authMap);

		pageFlow.setOnBoardingMap(onboardMap);
		pageFlow.setRegMap(registrationMap);

	}

	@Test
	public void getNextRegPageTest() {

		Assert.assertSame(pageFlow.getNextRegPage(RegistrationConstants.DEMOGRAPHIC_DETAIL),
				RegistrationConstants.DOCUMENT_SCAN);
	}

	@Test
	public void getPreviousRegPageTest() {

		Assert.assertSame(pageFlow.getPreviousRegPage(RegistrationConstants.DOCUMENT_SCAN),
				RegistrationConstants.DEMOGRAPHIC_DETAIL);
	}

	@Test
	public void getNextOnboardPageTest() {

		Assert.assertSame(pageFlow.getNextOnboardPage(RegistrationConstants.ONBOARD_USER_PARENT),
				RegistrationConstants.BIOMETRIC_EXCEPTION);
	}

	@Test
	public void getPreviousOnboardPageTest() {

		pageFlow.updateRegMap(RegistrationConstants.DOCUMENT_SCAN, RegistrationConstants.VISIBILITY, true);

		Assert.assertSame(pageFlow.getPreviousOnboardPage(RegistrationConstants.BIOMETRIC_EXCEPTION),
				RegistrationConstants.ONBOARD_USER_PARENT);
	}

	@Test
	public void isVisibleInRegFlowMapTest() {

		pageFlow.updateRegMap(RegistrationConstants.DOCUMENT_SCAN, RegistrationConstants.VISIBILITY, true);

		Assert.assertSame(
				pageFlow.isVisibleInRegFlowMap(RegistrationConstants.DOCUMENT_SCAN, RegistrationConstants.VISIBILITY),
				true);

		// Not in the map so that it should create
		pageFlow.updateRegMap(RegistrationConstants.DOCUMENT_SCAN + "PANE", RegistrationConstants.VISIBILITY, false);

		Assert.assertSame(pageFlow.isVisibleInRegFlowMap(RegistrationConstants.DOCUMENT_SCAN + "PANE",
				RegistrationConstants.VISIBILITY), false);

		// Not in the map so that it should fail
		Assert.assertSame(pageFlow.isVisibleInRegFlowMap(RegistrationConstants.DOCUMENT_SCAN + "PANE2",
				RegistrationConstants.VISIBILITY), false);
	}

	@Test
	public void isVisibleInOnBoardFlowMapTest() {

		pageFlow.updateOnBoardingMap(RegistrationConstants.ONBOARD_USER_PARENT, RegistrationConstants.VISIBILITY, true);

		Assert.assertSame(pageFlow.isVisibleInOnBoardFlowMap(RegistrationConstants.ONBOARD_USER_PARENT,
				RegistrationConstants.VISIBILITY), true);
		pageFlow.updateOnBoardingMap(RegistrationConstants.ONBOARD_USER_PARENT + "PANE",
				RegistrationConstants.VISIBILITY, false);

		// Not in the map so that it should create
		Assert.assertSame(pageFlow.isVisibleInOnBoardFlowMap(RegistrationConstants.ONBOARD_USER_PARENT + "PANE",
				RegistrationConstants.VISIBILITY), false);

		// Not in the map so that it should fail
		Assert.assertSame(pageFlow.isVisibleInOnBoardFlowMap(RegistrationConstants.ONBOARD_USER_PARENT + "PANE2",
				RegistrationConstants.VISIBILITY), false);
	}

}
