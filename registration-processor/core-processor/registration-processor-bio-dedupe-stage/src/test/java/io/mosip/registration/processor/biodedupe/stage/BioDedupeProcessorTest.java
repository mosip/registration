package io.mosip.registration.processor.biodedupe.stage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.constant.AbisConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.spi.biodedupe.BioDedupeService;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.storage.dao.PacketInfoDao;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.utils.ABISHandlerUtil;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.rest.client.audit.dto.AuditResponseDto;
import io.mosip.registration.processor.status.dao.RegistrationStatusDao;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.entity.RegistrationStatusEntity;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

/**
 * The Class BioDedupeStageTest.
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*","com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*" })
@PrepareForTest({ Utilities.class })
public class BioDedupeProcessorTest {

	/** The Constant ERROR. */
	private static final String ERROR = "ERROR";

	/** The Constant IDENTITY. */
	private static final String IDENTITY = "identity";

	/** The Constant ABIS_HANDLER_BUS_IN. */
	private static final String ABIS_HANDLER_BUS_IN = "abis-handler-bus-in";

	/** The registration status service. */
	@Mock
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The packet info manager. */
	@Mock
	private PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;

	/** The bio dedupe service. */
	@Mock
	private BioDedupeService bioDedupeService;

	/** The registration status dao. */
	@Mock
	private RegistrationStatusDao registrationStatusDao;

	/** The packet info dao. */
	@Mock
	private PacketInfoDao packetInfoDao;

	@Mock
	private IdRepoService idRepoService;

	/** The dto. */
	MessageDTO dto = new MessageDTO();

	/** The registration status dto. */
	InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();

	/** The audit log request builder. */
	@Mock
	private AuditLogRequestBuilder auditLogRequestBuilder;

	/** The matched reg ids. */
	List<String> matchedRegIds = new ArrayList<String>();

	/** The registration status mapper util. */
	@Mock
	RegistrationExceptionMapperUtil registrationStatusMapperUtil;

	/** The bio dedupe processor. */
	@InjectMocks
	private BioDedupeProcessor bioDedupeProcessor;


	/** The stage name. */
	private String stageName = "BioDedupeStage";

	/** The Constant CONFIG_SERVER_URL. */
	private static final String CONFIG_SERVER_URL = "url";
	/** The utilities. */
	@Mock
	Utilities utility;

	/** The rest client service. */
	@Mock
	private RegistrationProcessorRestClientService<Object> restClientService;

	/** The entity. */
	@Mock
	RegistrationStatusEntity entity = new RegistrationStatusEntity();

	/** The abis handler util. */
	@Mock
	private ABISHandlerUtil abisHandlerUtil;

	/** The map identity json string to object. */
	@Mock
	ObjectMapper mapIdentityJsonStringToObject;

	@Mock
	LogDescription description;

	@Mock
	private Environment env;

	@Mock
	private PacketManagerService packetManagerService;

	private static final String ISINFANTBIOTOABIS = "registration.processor.infant.bio.to.abis";

	/**
	 * Sets the up.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Before
	public void setUp() throws Exception {
		Mockito.when(packetManagerService.getField(any(),any(),any(),any())).thenReturn("field");
		when(utility.getDefaultSource()).thenReturn("reg-client");
		ReflectionTestUtils.setField(bioDedupeProcessor, "infantDedupe", "Y");
		ReflectionTestUtils.setField(bioDedupeProcessor, "ageLimit", "4");

		AuditResponseDto auditResponseDto = new AuditResponseDto();
		ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<>();
		responseWrapper.setResponse(auditResponseDto);
		Mockito.doReturn(responseWrapper).when(auditLogRequestBuilder).createAuditRequestBuilder(
				"test case description", EventId.RPR_405.toString(), EventName.UPDATE.toString(),
				EventType.BUSINESS.toString(), "1234testcase", ApiName.AUDIT);

		dto.setRid("reg1234");
		registrationStatusDto.setRegistrationId("reg1234");
		registrationStatusDto.setRegistrationType("new");

		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(any(), any(), any());
		Mockito.doNothing().when(description).setMessage(any());

		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.when(abisHandlerUtil.getPacketStatus(any())).thenReturn(AbisConstant.PRE_ABIS_IDENTIFICATION);
		Mockito.when(restClientService.getApi(any(), any(), anyString(), any(), any()))
				.thenReturn("1233445566".getBytes("UTF-16"));
		Mockito.when(registrationStatusMapperUtil.getStatusCode(any())).thenReturn(ERROR);
		Mockito.doNothing().when(packetInfoManager).saveManualAdjudicationData(any(), any(), any(), any(), any());
		Mockito.doNothing().when(packetInfoManager).saveRegLostUinDet(any(), any(), any(), any());


		ClassLoader classLoader = getClass().getClassLoader();

		File mappingJsonFile = new File(classLoader.getResource("RegistrationProcessorIdentity.json").getFile());
		InputStream is = new FileInputStream(mappingJsonFile);
		String value = IOUtils.toString(is);
		Mockito.when(utility.getRegistrationProcessorMappingJson()).thenReturn(JsonUtil
				.getJSONObject(JsonUtil.objectMapperReadValue(value, JSONObject.class), MappingJsonConstants.IDENTITY));
		Mockito.when(bioDedupeService.getFileByRegId(any(),any(),any())).thenReturn("test".getBytes());

	}

	/**
	 * Test bio dedupe success.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testNewInsertionPostProcessing() throws Exception {

		MessageDTO messageDto = bioDedupeProcessor.process(dto, stageName);
		assertTrue(messageDto.getMessageBusAddress().toString()
				.equalsIgnoreCase(MessageBusAddress.ABIS_HANDLER_BUS_IN.toString()));

	}

	/**
	 * Test new insertion to uin success.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testNewInsertionToUinSuccess() throws Exception {
		Mockito.when(bioDedupeService.getFileByRegId(anyString(),anyString(),anyString())).thenReturn(null);
		ReflectionTestUtils.setField(bioDedupeProcessor, "infantDedupe", "N");
		Mockito.when(restClientService.getApi(any(), any(), anyString(), any(), any())).thenReturn(null);
		Mockito.when(utility.getApplicantAge(any(),any(),any())).thenReturn(2);
		MessageDTO messageDto = bioDedupeProcessor.process(dto, stageName);

		assertTrue(messageDto.getIsValid());

	}

	/**
	 * Test new insertion adult CBEFF not found exception.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testNewInsertionAdultCBEFFNotFoundException() throws Exception {
		Mockito.when(bioDedupeService.getFileByRegId(anyString(),anyString(),anyString())).thenReturn(null);
		Mockito.when(restClientService.getApi(any(), any(), anyString(), any(), any())).thenReturn(null);
		Mockito.when(utility.getApplicantAge(any(),any(),any())).thenReturn(12);
		MessageDTO messageDto = bioDedupeProcessor.process(dto, stageName);
		assertTrue(messageDto.getInternalError());
	}

	/**
	 * Test new exception.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testNewException() throws Exception {
		Mockito.when(bioDedupeService.getFileByRegId(anyString(),anyString(),anyString())).thenReturn(null);

		ReflectionTestUtils.setField(bioDedupeProcessor, "ageLimit", "age");
		Mockito.when(restClientService.getApi(any(), any(), anyString(), any(), any())).thenReturn(null);
		Mockito.when(utility.getApplicantAge(any(),any(),any())).thenReturn(12);
		MessageDTO messageDto = bioDedupeProcessor.process(dto, stageName);
		assertTrue(messageDto.getInternalError());
	}

	/**
	 * Test new insertion IO exception.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testNewInsertionIOException() throws Exception {
		Mockito.when(bioDedupeService.getFileByRegId(anyString(),anyString(),anyString())).thenReturn(null);
		Mockito.when(restClientService.getApi(any(), any(), anyString(), any(), any())).thenReturn(null);
		Mockito.when(utility.getApplicantAge(any(),any(),any())).thenThrow(new IOException("IOException"));
		MessageDTO messageDto = bioDedupeProcessor.process(dto, stageName);
		assertTrue(messageDto.getInternalError());
	}

	/**
	 * Test data access exception.
	 */
	@Test
	public void testDataAccessException() {
		Mockito.when(registrationStatusService.getRegistrationStatus(any()))
				.thenThrow(new DataAccessException("DataAccessException") {
					private static final long serialVersionUID = 1L;
				});
		MessageDTO messageDto = bioDedupeProcessor.process(dto, stageName);
		assertTrue(messageDto.getInternalError());
	}

	/**
	 * Test new identify to UIN stage.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testNewIdentifyToUINStage() throws Exception {
		Mockito.when(abisHandlerUtil.getPacketStatus(any())).thenReturn(AbisConstant.POST_ABIS_IDENTIFICATION);
		MessageDTO messageDto = bioDedupeProcessor.process(dto, stageName);

		assertTrue(messageDto.getIsValid());

	}

	/**
	 * Test new identify to manual stage.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testNewIdentifyToManualStage() throws Exception {

		List<String> list = new ArrayList<>();
		list.add("1");
		Mockito.when(abisHandlerUtil.getPacketStatus(any())).thenReturn(AbisConstant.POST_ABIS_IDENTIFICATION);

		Mockito.when(abisHandlerUtil.getUniqueRegIds(any(), any(), any())).thenReturn(list);
		MessageDTO messageDto = bioDedupeProcessor.process(dto, stageName);

		assertFalse(messageDto.getIsValid());

	}

	/**
	 * Test update insertion to handler.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testUpdateInsertionToHandler() throws Exception {

		PowerMockito.mockStatic(Utilities.class);
		Mockito.when(utility.getGetRegProcessorDemographicIdentity()).thenReturn(IDENTITY);


		registrationStatusDto.setRegistrationId("reg1234");
		registrationStatusDto.setRegistrationType("Update");
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);

		MessageDTO messageDto = bioDedupeProcessor.process(dto, stageName);

		assertEquals(messageDto.getMessageBusAddress().getAddress(), ABIS_HANDLER_BUS_IN);
	}

	/**
	 * Test update insertion to UIN.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testUpdateInsertionToUIN() throws Exception {

		PowerMockito.mockStatic(Utilities.class);
		Mockito.when(utility.getGetRegProcessorDemographicIdentity()).thenReturn(IDENTITY);
		Mockito.when(packetManagerService.getField(any(),any(),any(),any())).thenReturn(null);

		registrationStatusDto.setRegistrationId("reg1234");
		registrationStatusDto.setRegistrationType("Update");
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);

		MessageDTO messageDto = bioDedupeProcessor.process(dto, stageName);

		assertTrue(messageDto.getIsValid());
	}

	/**
	 * Test bio de dup update packet handler processing success.
	 *
	 * @throws ApisResourceAccessException           the apis resource access
	 *                                               exception
	 * @throws IOException                           Signals that an I/O exception
	 *                                               has occurred.
	 * @throws                                       io.mosip.kernel.core.exception.IOException
	 * @throws PacketDecryptionFailureException
	 * @throws RegistrationProcessorCheckedException
	 */
	@Test
	public void testBioDeDupUpdatePacketHandlerProcessingSuccess() throws ApisResourceAccessException, IOException,
			io.mosip.kernel.core.exception.IOException, JsonProcessingException, PacketManagerException {

		registrationStatusDto.setRegistrationId("reg1234");
		registrationStatusDto.setRegistrationType("UPDATE");
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.when(abisHandlerUtil.getPacketStatus(any())).thenReturn(AbisConstant.POST_ABIS_IDENTIFICATION);

		List<String> matchedRidList = new ArrayList<>();
		Mockito.when(abisHandlerUtil.getUniqueRegIds(any(), any(), any())).thenReturn(matchedRidList);
		MessageDTO messageDto = bioDedupeProcessor.process(dto, stageName);
		assertTrue(messageDto.getIsValid());
	}

	/**
	 * Test bio de dup update packet handler processing failure.
	 *
	 * @throws ApisResourceAccessException           the apis resource access
	 *                                               exception
	 * @throws IOException                           Signals that an I/O exception
	 *                                               has occurred.
	 * @throws                                       io.mosip.kernel.core.exception.IOException
	 * @throws PacketDecryptionFailureException
	 * @throws RegistrationProcessorCheckedException
	 */
	@Test
	public void testBioDeDupUpdatePacketHandlerProcessingFailure() throws ApisResourceAccessException, IOException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException, JsonProcessingException, PacketManagerException {
		registrationStatusDto.setRegistrationId("reg1234");
		registrationStatusDto.setRegistrationType("UPDATE");
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.when(abisHandlerUtil.getPacketStatus(any())).thenReturn(AbisConstant.POST_ABIS_IDENTIFICATION);

		List<String> matchedRidList = new ArrayList<>();
		matchedRidList.add("27847657360002520190320095010");
		Mockito.when(abisHandlerUtil.getUniqueRegIds(any(), any(), any())).thenReturn(matchedRidList);

		MessageDTO messageDto = bioDedupeProcessor.process(dto, stageName);
		assertFalse(messageDto.getIsValid());
	}

	/**
	 * Test lost packet validation matched id empty.
	 *
	 * @throws ApisResourceAccessException           the apis resource access
	 *                                               exception
	 * @throws IOException                           Signals that an I/O exception
	 *                                               has occurred.
	 * @throws                                       io.mosip.kernel.core.exception.IOException
	 * @throws PacketDecryptionFailureException
	 * @throws RegistrationProcessorCheckedException
	 */
	@Test
	public void testLostPacketValidationMatchedIdEmpty() throws ApisResourceAccessException, IOException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException, JsonProcessingException, PacketManagerException {
		registrationStatusDto.setRegistrationId("reg1234");
		registrationStatusDto.setRegistrationType("LOST");
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.when(abisHandlerUtil.getPacketStatus(any())).thenReturn(AbisConstant.POST_ABIS_IDENTIFICATION);
		List<String> matchedRidList = new ArrayList<>();
		Mockito.when(abisHandlerUtil.getUniqueRegIds(any(), any(), any())).thenReturn(matchedRidList);

		MessageDTO messageDto = bioDedupeProcessor.process(dto, stageName);
		assertFalse(messageDto.getIsValid());
	}

	/**
	 * Test lost packet validation single matched reg id.
	 *
	 * @throws ApisResourceAccessException           the apis resource access
	 *                                               exception
	 * @throws IOException                           Signals that an I/O exception
	 *                                               has occurred.
	 * @throws                                       io.mosip.kernel.core.exception.IOException
	 * @throws PacketDecryptionFailureException
	 * @throws RegistrationProcessorCheckedException
	 */
	@Test
	public void testLostPacketValidationSingleMatchedRegId() throws ApisResourceAccessException, IOException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException, JsonProcessingException, PacketManagerException {
		registrationStatusDto.setRegistrationId("reg1234");
		registrationStatusDto.setRegistrationType("LOST");
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.when(abisHandlerUtil.getPacketStatus(any())).thenReturn(AbisConstant.POST_ABIS_IDENTIFICATION);
		List<String> matchedRidList = new ArrayList<>();
		matchedRidList.add("27847657360002520190320095010");
		Mockito.when(abisHandlerUtil.getUniqueRegIds(any(), any(), any())).thenReturn(matchedRidList);

		MessageDTO messageDto = bioDedupeProcessor.process(dto, stageName);
		assertTrue(messageDto.getIsValid());
	}

	/**
	 * Test lost packet validation multiple matched reg id.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testLostPacketValidationMultipleMatchedRegId() throws Exception {

		registrationStatusDto.setRegistrationId("reg1234");
		registrationStatusDto.setRegistrationType("LOST");
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.when(abisHandlerUtil.getPacketStatus(any())).thenReturn(AbisConstant.POST_ABIS_IDENTIFICATION);
		List<String> matchedRidList = new ArrayList<>();
		matchedRidList.add("27847657360002520190320095010");
		matchedRidList.add("27847657360002520190320095011");
		Mockito.when(abisHandlerUtil.getUniqueRegIds(any(), any(), any())).thenReturn(matchedRidList);

		Mockito.when(packetManagerService.getField("reg1234","gender","reg-client", "LOST")).thenReturn("MALE");
		Mockito.when(packetManagerService.getField("reg1234","dob","reg-client", "LOST")).thenReturn("2016/01/01");

		Map<String, String> map = new LinkedHashMap<>();
		map.put("gender", "MALE");
		map.put("dateOfBirth", "2016/01/01");
		JSONObject j1 = new JSONObject(map);

		Mockito.when(idRepoService.getIdJsonFromIDRepo(any(), any())).thenReturn(j1);
		Mockito.when(utility.getApplicantAge(any(),any(),any())).thenReturn(12);
		MessageDTO messageDto = bioDedupeProcessor.process(dto, stageName);

		assertFalse(messageDto.getIsValid());
	}

	/**
	 * Test lost packet validation multiple matched reg id demo match.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testLostPacketValidationSingleDemoMatch() throws Exception {

		registrationStatusDto.setRegistrationId("reg1234");
		registrationStatusDto.setRegistrationType("LOST");
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.when(abisHandlerUtil.getPacketStatus(any())).thenReturn(AbisConstant.POST_ABIS_IDENTIFICATION);
		List<String> matchedRidList = new ArrayList<>();
		matchedRidList.add("27847657360002520190320095010");
		matchedRidList.add("27847657360002520190320095011");
		Mockito.when(abisHandlerUtil.getUniqueRegIds(any(), any(), any())).thenReturn(matchedRidList);

		JSONObject obj1 = new JSONObject();
		obj1.put("dateOfBirth", "2016/01/01");

		JSONObject obj2 = new JSONObject();
		obj2.put("dateOfBirth", "2016/01/02");
		Mockito.when(idRepoService.getIdJsonFromIDRepo("27847657360002520190320095010", IDENTITY)).thenReturn(obj1);
		Mockito.when(idRepoService.getIdJsonFromIDRepo("27847657360002520190320095011", IDENTITY)).thenReturn(obj2);
		MessageDTO messageDto = bioDedupeProcessor.process(dto, stageName);

		assertFalse(messageDto.getInternalError());
	}
	
	@Test
	public void testPacketValidationSingleDemoMatch() throws Exception {
		registrationStatusDto.setRegistrationId("reg1234");
		registrationStatusDto.setRegistrationType("LOST");
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.when(abisHandlerUtil.getPacketStatus(any())).thenReturn(AbisConstant.POST_ABIS_IDENTIFICATION);
		List<String> matchedRidList = new ArrayList<>();
		matchedRidList.add("27847657360002520190320095010");
		matchedRidList.add("27847657360002520190320095011");
		Mockito.when(abisHandlerUtil.getUniqueRegIds(any(), any(), any())).thenReturn(matchedRidList);

		JSONObject obj1 = new JSONObject();
		obj1.put("dateOfBirth", "2016/01/01");

		JSONObject obj2 = new JSONObject();
		obj2.put("dateOfBirth", "2016/01/02");
		Mockito.when(utility.getGetRegProcessorDemographicIdentity()).thenReturn(IDENTITY);
		Mockito.when(idRepoService.getIdJsonFromIDRepo("27847657360002520190320095010", IDENTITY)).thenReturn(obj1);
		Mockito.when(idRepoService.getIdJsonFromIDRepo("27847657360002520190320095011", IDENTITY)).thenReturn(obj2);
		Mockito.when(packetManagerService.getField("reg1234","dob","reg-client", "LOST")).thenReturn("2016/01/01");
		MessageDTO messageDto = bioDedupeProcessor.process(dto, stageName);

		assertFalse(messageDto.getInternalError());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testLostPacketValidationMultipleDemoMatch() throws Exception {

		registrationStatusDto.setRegistrationId("reg1234");
		registrationStatusDto.setRegistrationType("LOST");
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.when(abisHandlerUtil.getPacketStatus(any())).thenReturn(AbisConstant.POST_ABIS_IDENTIFICATION);
		List<String> matchedRidList = new ArrayList<>();
		matchedRidList.add("27847657360002520190320095010");
		matchedRidList.add("27847657360002520190320095011");
		matchedRidList.add("27847657360002520190320095012");
		Mockito.when(abisHandlerUtil.getUniqueRegIds(any(), any(), any())).thenReturn(matchedRidList);

		JSONObject obj1 = new JSONObject();
		obj1.put("dateOfBirth", "2016/01/01");

		JSONObject obj2 = new JSONObject();
		obj2.put("dateOfBirth", "2016/01/02");
		Mockito.when(idRepoService.getIdJsonFromIDRepo("27847657360002520190320095010", IDENTITY)).thenReturn(obj1);
		Mockito.when(idRepoService.getIdJsonFromIDRepo("27847657360002520190320095011", IDENTITY)).thenReturn(obj2);
		Mockito.when(idRepoService.getIdJsonFromIDRepo("27847657360002520190320095012", IDENTITY)).thenReturn(obj1);
		MessageDTO messageDto = bioDedupeProcessor.process(dto, stageName);

		assertFalse(messageDto.getInternalError());
	}

	@Test
	public void testLostPacketValidationCbeffNotFound() throws Exception {

		registrationStatusDto.setRegistrationId("reg1234");
		registrationStatusDto.setRegistrationType("LOST");
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.when(abisHandlerUtil.getPacketStatus(any())).thenReturn(AbisConstant.POST_ABIS_IDENTIFICATION);
		Mockito.when(bioDedupeService.getFileByRegId(anyString(),anyString(),anyString())).thenReturn(null);
		Mockito.when(restClientService.getApi(any(), any(), anyString(), any(), any())).thenReturn(null);
		MessageDTO messageDto = bioDedupeProcessor.process(dto, stageName);
		assertTrue(messageDto.getInternalError());
	}

	@Test
	public void testLostPacketPreAbis() throws Exception {

		registrationStatusDto.setRegistrationId("reg1234");
		registrationStatusDto.setRegistrationType("LOST");
		Mockito.when(abisHandlerUtil.getPacketStatus(any())).thenReturn(AbisConstant.PRE_ABIS_IDENTIFICATION);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		MessageDTO messageDto = bioDedupeProcessor.process(dto, stageName);
		assertTrue(messageDto.getMessageBusAddress().toString()
				.equalsIgnoreCase(MessageBusAddress.ABIS_HANDLER_BUS_IN.toString()));
	}
	
	@Test
	public void testApisResourceAccessException() throws Exception {
		Mockito.when(bioDedupeService.getFileByRegId(anyString(),anyString(),anyString())).thenReturn(null);
		ApisResourceAccessException e=new ApisResourceAccessException();
	
		
		Mockito.doThrow(e).when(utility).getApplicantAge(any(),any(),any());
		MessageDTO messageDto = bioDedupeProcessor.process(dto, stageName);

		assertFalse(messageDto.getIsValid());

	}
}
