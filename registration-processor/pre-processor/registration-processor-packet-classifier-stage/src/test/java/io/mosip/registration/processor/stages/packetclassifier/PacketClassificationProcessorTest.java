package io.mosip.registration.processor.stages.packetclassifier;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
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
import org.powermock.reflect.Whitebox;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.storage.exception.ParsingException;
import io.mosip.registration.processor.packet.storage.utils.IdSchemaUtil;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.stages.packetclassifier.tagging.TagGenerator;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationAdditionalInfoDTO;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.SyncRegistrationService;

/**
 * The Class PacketValidatorStageTest.
 */
@RefreshScope
@RunWith(PowerMockRunner.class)
@PrepareForTest({ JsonUtil.class })
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*", "com.sun.org.apache.xerces.*", 
	"javax.xml.*", "org.xml.*" })
public class PacketClassificationProcessorTest {

	private static String IDSchemaVersionLabel = "IDSchemaVersion";

	@InjectMocks
	private PacketClassificationProcessor packetClassificationProcessor;

	@Mock
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	@Mock
	AuditLogRequestBuilder auditLogRequestBuilder;

	@Mock
	private RegistrationProcessorRestClientService<Object> restClientService;

	@Mock
	private PacketManagerService packetManagerService;

	@Mock
	private PriorityBasedPacketManagerService priorityBasedPacketManagerService;

	@Mock
	private SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationService;

	@Mock
	RegistrationExceptionMapperUtil registrationStatusMapperUtil;

	@Mock
	private Utilities utility;

	@Mock
	private IdSchemaUtil idSchemaUtil;

	private ArrayList<TagGenerator> tagGenerators = new ArrayList<TagGenerator>();

	@Mock
	private TagGenerator tagGenerator;

	private MessageDTO messageDTO;
	private String stageName;
	private InternalRegistrationStatusDto registrationStatusDto;

	@Before
	public void setup() throws Exception {

		messageDTO = new MessageDTO();
		messageDTO.setRid("123456789");
		messageDTO.setInternalError(false);
		messageDTO.setIsValid(true);
		messageDTO.setReg_type(RegistrationType.NEW);
		stageName = "PacketClassifierStage";
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("123456789");

		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		Mockito.when(utility.getRegistrationProcessorMappingJson(anyString())).thenReturn(null);

		RegistrationAdditionalInfoDTO registrationAdditionalInfoDTO = new RegistrationAdditionalInfoDTO();
		registrationAdditionalInfoDTO.setName("abc");
		registrationAdditionalInfoDTO.setPhone("9898989898");
		registrationAdditionalInfoDTO.setEmail("abc@gmail.com");

		PowerMockito.mockStatic(JsonUtil.class);
		PowerMockito.when(JsonUtil.getJSONValue(any(), anyString())).thenReturn(IDSchemaVersionLabel);

		Map<String, String> fieldMap = new HashMap<>();
		fieldMap.put(IDSchemaVersionLabel, "0.1");
		Mockito.when(priorityBasedPacketManagerService.getFields(any(), any(), any(), any())).thenReturn(fieldMap);

		Map<String, String> fieldTypeMap = new HashMap<>();
		fieldTypeMap.put("gender", "simpleType");
		Mockito.when(idSchemaUtil.getIdSchemaFieldTypes(anyDouble())).thenReturn(fieldTypeMap);

		tagGenerators.add(tagGenerator);

		Whitebox.setInternalState(packetClassificationProcessor, "tagGenerators", tagGenerators);
		List<String> requiredIdObjectFieldNames = new ArrayList<String>();
		requiredIdObjectFieldNames.add("gender");
		requiredIdObjectFieldNames.add("city");
		Mockito.when(tagGenerator.getRequiredIdObjectFieldNames()).thenReturn(requiredIdObjectFieldNames);
		Map<String, String> tags = new HashMap<>();
		tags.put("city", "1234");
		tags.put("gender", "MALE");
		Mockito.when(tagGenerator.generateTags(any(), any(), any(), any())).thenReturn(tags);
	}

	@Test
	public void packetClassificationSuccessTest() throws Exception {
		Whitebox.invokeMethod(packetClassificationProcessor, "collectRequiredIdObjectFieldNames");
		assertTrue(packetClassificationProcessor.process(messageDTO, stageName).getIsValid());
	}

	@Test
	public void packetClassificationWithEmptyListOfTagGenerators() throws Exception {
		Whitebox.setInternalState(packetClassificationProcessor, "tagGenerators", new ArrayList<TagGenerator>());
		Whitebox.invokeMethod(packetClassificationProcessor, "collectRequiredIdObjectFieldNames");
		assertTrue(packetClassificationProcessor.process(messageDTO, stageName).getIsValid());
	}

	@Test
	public void packetClassificationWithTagGeneratorGetRequiredIdObjectFieldNamesMethodReturnNull() throws Exception {
		Mockito.when(tagGenerator.getRequiredIdObjectFieldNames()).thenReturn(null);
		Whitebox.invokeMethod(packetClassificationProcessor, "collectRequiredIdObjectFieldNames");
		assertTrue(packetClassificationProcessor.process(messageDTO, stageName).getIsValid());
	}

	@Test
	public void packetClassificationWithTagGeneratorGenerateTagsMethodReturnNull() throws Exception {
		Mockito.when(tagGenerator.generateTags(any(), any(), any(), any())).thenReturn(null);
		Whitebox.invokeMethod(packetClassificationProcessor, "collectRequiredIdObjectFieldNames");
		assertTrue(packetClassificationProcessor.process(messageDTO, stageName).getIsValid());
	}

	@Test
	public void packetClassificationPacketManagerExceptionTest() throws Exception {
		PacketManagerException exc = new PacketManagerException("", "");
		Whitebox.invokeMethod(packetClassificationProcessor, "collectRequiredIdObjectFieldNames");
		Mockito.when(tagGenerator.generateTags(any(), any(), any(), any())).thenThrow(exc);
		assertTrue(packetClassificationProcessor.process(messageDTO, stageName).getInternalError());
	}

	@Test
	public void packetClassificationIOExceptionTest() throws Exception {
		BaseCheckedException exc = new BaseCheckedException("", "", new IOException(""));
		Whitebox.invokeMethod(packetClassificationProcessor, "collectRequiredIdObjectFieldNames");
		Mockito.when(tagGenerator.generateTags(any(), any(), any(), any())).thenThrow(exc);
		assertTrue(packetClassificationProcessor.process(messageDTO, stageName).getInternalError());
	}

	@Test
	public void packetClassificationParsingExceptionTest() throws Exception {
		ParsingException exc = new ParsingException("", new Exception());
		Whitebox.invokeMethod(packetClassificationProcessor, "collectRequiredIdObjectFieldNames");
		Mockito.when(tagGenerator.generateTags(any(), any(), any(), any())).thenThrow(exc);
		assertTrue(packetClassificationProcessor.process(messageDTO, stageName).getInternalError());
	}

	@Test
	public void packetClassificationTablenotAccessibleExceptionTest() throws Exception {
		TablenotAccessibleException exc = new TablenotAccessibleException("");
		Whitebox.invokeMethod(packetClassificationProcessor, "collectRequiredIdObjectFieldNames");
		Mockito.when(tagGenerator.generateTags(any(), any(), any(), any())).thenThrow(exc);
		assertTrue(packetClassificationProcessor.process(messageDTO, stageName).getInternalError());
	}

	@Test
	public void packetClassificationBaseUncheckedExceptionTest() throws Exception {
		BaseUncheckedException exc = new BaseUncheckedException("", "");
		Whitebox.invokeMethod(packetClassificationProcessor, "collectRequiredIdObjectFieldNames");
		Mockito.when(tagGenerator.generateTags(any(), any(), any(), any())).thenThrow(exc);
		assertTrue(packetClassificationProcessor.process(messageDTO, stageName).getInternalError());
	}

	@Test
	public void packetClassificationBaseCheckedExceptionTest() throws Exception {
		BaseCheckedException exc = new BaseCheckedException("", "");
		Whitebox.invokeMethod(packetClassificationProcessor, "collectRequiredIdObjectFieldNames");
		Mockito.when(tagGenerator.generateTags(any(), any(), any(), any())).thenThrow(exc);
		assertTrue(packetClassificationProcessor.process(messageDTO, stageName).getInternalError());
	}
}
