package io.mosip.registrationprocessor.credentialrequestor.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.credentialrequestor.dto.CredentialPartner;
import io.mosip.registration.processor.credentialrequestor.dto.CredentialPartnersList;
import io.mosip.registration.processor.credentialrequestor.util.CredentialPartnerUtil;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.service.SyncRegistrationService;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Utilities.class })
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "javax.net.*" })
public class CredentialPartnerUtilTest {

	private static final String REG_ID = "10001100770000320200720095022";
	private static final String WORKFLOW_INSTANCE_ID = "wf-001";
	private static final String CENTER_METADATA = "[{\"label\":\"centerId\",\"value\":\"10001\"}]";

	@InjectMocks
	private CredentialPartnerUtil credentialPartnerUtil;

	@Mock
	private PriorityBasedPacketManagerService packetManagerService;

	@Mock
	private SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationService;

	@Before
	public void setUp() {
		ReflectionTestUtils.setField(credentialPartnerUtil, "configServerFileStorageURL", "http://config/");
		ReflectionTestUtils.setField(credentialPartnerUtil, "partnerProfileFileName", "partners.json");
		ReflectionTestUtils.setField(credentialPartnerUtil, "objectMapper", new ObjectMapper());
		ReflectionTestUtils.setField(credentialPartnerUtil, "requiredIDObjectFieldNamesMap", new HashMap<String, String>());
		ReflectionTestUtils.setField(credentialPartnerUtil, "mandatoryLanguages", Collections.singletonList("eng"));
		ReflectionTestUtils.setField(credentialPartnerUtil, "noMatchIssuer", "");
	}

	@Test(expected = RegistrationProcessorCheckedException.class)
	public void loadPartnerDetails_GetJsonThrows_WrapsAsCheckedException() throws Exception {
		PowerMockito.mockStatic(Utilities.class);
		PowerMockito.when(Utilities.getJson(anyString(), anyString())).thenThrow(new RuntimeException("config down"));

		credentialPartnerUtil.loadPartnerDetails();
	}

	@Test
	public void getAllCredentialPartners_ReloadsWhenNull() throws Exception {
		PowerMockito.mockStatic(Utilities.class);
		PowerMockito.when(Utilities.getJson(anyString(), anyString())).thenReturn("{\"partners\":[]}");

		CredentialPartnersList result = credentialPartnerUtil.getAllCredentialPartners();

		assertNotNull(result);
		assertSame(result, credentialPartnerUtil.getAllCredentialPartners());
	}

	@Test
	public void getCredentialPartners_usesStoredPacketMetaData() throws Exception {
		preparePartnerFilter("digitalcardPartner", "centerId == '10001'", "NEW");
		SyncRegistrationEntity entity = new SyncRegistrationEntity();
		entity.setPacketMetaData(CENTER_METADATA);
		when(syncRegistrationService.findByWorkflowInstanceId(WORKFLOW_INSTANCE_ID)).thenReturn(entity);

		List<CredentialPartner> partners = credentialPartnerUtil.getCredentialPartners(
				REG_ID, "NEW", WORKFLOW_INSTANCE_ID, new JSONObject());

		assertEquals(1, partners.size());
		assertEquals("digitalcardPartner", partners.get(0).getId());
		assertEquals("partner-1", partners.get(0).getPartnerId());
		verify(syncRegistrationService, times(1)).findByWorkflowInstanceId(WORKFLOW_INSTANCE_ID);
		verify(packetManagerService, never()).getMetaInfo(anyString(), anyString(), any(ProviderStageName.class));
	}

	@Test
	public void getCredentialPartners_fallsBackToPacketManagerWhenColumnEmpty() throws Exception {
		preparePartnerFilter("digitalcardPartner", "centerId == '10001'", "NEW");
		SyncRegistrationEntity entity = new SyncRegistrationEntity();
		entity.setPacketMetaData(null);
		when(syncRegistrationService.findByWorkflowInstanceId(WORKFLOW_INSTANCE_ID)).thenReturn(entity);
		when(packetManagerService.getMetaInfo(REG_ID, "NEW", ProviderStageName.CREDENTIAL_REQUESTOR))
				.thenReturn(metaInfo(CENTER_METADATA));

		List<CredentialPartner> partners = credentialPartnerUtil.getCredentialPartners(
				REG_ID, "NEW", WORKFLOW_INSTANCE_ID, new JSONObject());

		assertEquals(1, partners.size());
		assertEquals("digitalcardPartner", partners.get(0).getId());
		verify(packetManagerService, times(1)).getMetaInfo(REG_ID, "NEW", ProviderStageName.CREDENTIAL_REQUESTOR);
	}

	@Test
	public void getCredentialPartners_fallsBackWhenRegistrationListRowMissing() throws Exception {
		preparePartnerFilter("digitalcardPartner", "centerId == '10001'", "NEW");
		when(syncRegistrationService.findByWorkflowInstanceId(WORKFLOW_INSTANCE_ID)).thenReturn(null);
		when(packetManagerService.getMetaInfo(REG_ID, "NEW", ProviderStageName.CREDENTIAL_REQUESTOR))
				.thenReturn(metaInfo(CENTER_METADATA));

		List<CredentialPartner> partners = credentialPartnerUtil.getCredentialPartners(
				REG_ID, "NEW", WORKFLOW_INSTANCE_ID, new JSONObject());

		assertEquals(1, partners.size());
		assertEquals("digitalcardPartner", partners.get(0).getId());
		verify(packetManagerService, times(1)).getMetaInfo(REG_ID, "NEW", ProviderStageName.CREDENTIAL_REQUESTOR);
	}

	@Test
	public void getCredentialPartners_fallsBackWhenWorkflowInstanceIdBlank() throws Exception {
		preparePartnerFilter("digitalcardPartner", "centerId == '10001'", "NEW");
		when(packetManagerService.getMetaInfo(REG_ID, "NEW", ProviderStageName.CREDENTIAL_REQUESTOR))
				.thenReturn(metaInfo(CENTER_METADATA));

		List<CredentialPartner> partners = credentialPartnerUtil.getCredentialPartners(
				REG_ID, "NEW", "  ", new JSONObject());

		assertEquals(1, partners.size());
		assertEquals("digitalcardPartner", partners.get(0).getId());
		verify(syncRegistrationService, never()).findByWorkflowInstanceId(anyString());
		verify(packetManagerService, times(1)).getMetaInfo(REG_ID, "NEW", ProviderStageName.CREDENTIAL_REQUESTOR);
	}

	@Test
	public void getCredentialPartners_excludesPartnerWhenMetadataDoesNotMatch() throws Exception {
		preparePartnerFilter("digitalcardPartner", "centerId == '10001'", "NEW");
		SyncRegistrationEntity entity = new SyncRegistrationEntity();
		entity.setPacketMetaData("[{\"label\":\"centerId\",\"value\":\"99999\"}]");
		when(syncRegistrationService.findByWorkflowInstanceId(WORKFLOW_INSTANCE_ID)).thenReturn(entity);

		List<CredentialPartner> partners = credentialPartnerUtil.getCredentialPartners(
				REG_ID, "NEW", WORKFLOW_INSTANCE_ID, new JSONObject());

		assertTrue(partners.isEmpty());
		verify(packetManagerService, never()).getMetaInfo(anyString(), anyString(), any(ProviderStageName.class));
	}

	@Test
	public void getCredentialPartners_excludesPartnerForDifferentProcess() throws Exception {
		preparePartnerFilter("digitalcardPartner", "centerId == '10001'", "UPDATE");
		SyncRegistrationEntity entity = new SyncRegistrationEntity();
		entity.setPacketMetaData(CENTER_METADATA);
		when(syncRegistrationService.findByWorkflowInstanceId(WORKFLOW_INSTANCE_ID)).thenReturn(entity);

		List<CredentialPartner> partners = credentialPartnerUtil.getCredentialPartners(
				REG_ID, "NEW", WORKFLOW_INSTANCE_ID, new JSONObject());

		assertTrue(partners.isEmpty());
	}

	@Test
	public void getCredentialPartners_usesNoMatchIssuerWhenNothingMatches() throws Exception {
		preparePartnerFilter("fallbackPartner", "centerId == '10001'", "NEW");
		ReflectionTestUtils.setField(credentialPartnerUtil, "noMatchIssuer", "fallbackPartner");
		SyncRegistrationEntity entity = new SyncRegistrationEntity();
		entity.setPacketMetaData("[{\"label\":\"centerId\",\"value\":\"99999\"}]");
		when(syncRegistrationService.findByWorkflowInstanceId(WORKFLOW_INSTANCE_ID)).thenReturn(entity);

		List<CredentialPartner> partners = credentialPartnerUtil.getCredentialPartners(
				REG_ID, "NEW", WORKFLOW_INSTANCE_ID, new JSONObject());

		assertEquals(1, partners.size());
		assertEquals("fallbackPartner", partners.get(0).getId());
	}

	@Test
	public void getCredentialPartners_emptyExpressions_skipsMetadataLookup() throws Exception {
		ReflectionTestUtils.setField(credentialPartnerUtil, "credentialPartnerExpression", new HashMap<String, String>());

		List<CredentialPartner> partners = credentialPartnerUtil.getCredentialPartners(
				REG_ID, "NEW", WORKFLOW_INSTANCE_ID, new JSONObject());

		assertTrue(partners.isEmpty());
		verify(syncRegistrationService, never()).findByWorkflowInstanceId(any());
		verify(packetManagerService, never()).getMetaInfo(anyString(), anyString(), any(ProviderStageName.class));
	}

	private void preparePartnerFilter(String partnerId, String expression, String process) {
		Map<String, String> expressions = new HashMap<>();
		expressions.put(partnerId, expression);
		ReflectionTestUtils.setField(credentialPartnerUtil, "credentialPartnerExpression", expressions);

		CredentialPartner partner = new CredentialPartner();
		partner.setId(partnerId);
		partner.setPartnerId("partner-1");
		partner.setCredentialType("digitalcard");
		partner.setProcess(Arrays.asList(process));
		CredentialPartnersList partnersList = new CredentialPartnersList();
		partnersList.setPartners(Collections.singletonList(partner));
		ReflectionTestUtils.setField(credentialPartnerUtil, "credentialPartners", partnersList);
	}

	private static Map<String, String> metaInfo(String metaData) {
		Map<String, String> metaInfo = new HashMap<>();
		metaInfo.put(JsonConstant.METADATA, metaData);
		return metaInfo;
	}
}
