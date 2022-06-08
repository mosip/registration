package io.mosip.registration.processor.packet.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
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
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.commons.khazana.exception.FileNotFoundInDestinationException;
import io.mosip.commons.khazana.exception.ObjectStoreAdapterException;
import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.kernel.core.virusscanner.exception.VirusScannerException;
import io.mosip.kernel.core.virusscanner.spi.VirusScanner;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.packet.dto.AdditionalInfoRequestDto;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.decryptor.Decryptor;
import io.mosip.registration.processor.packet.manager.utils.ZipUtils;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.packet.uploader.service.PacketUploaderService;
import io.mosip.registration.processor.packet.uploader.service.impl.PacketUploaderServiceImpl;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.rest.client.audit.dto.AuditResponseDto;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.AdditionalInfoRequestService;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.SyncRegistrationService;

@RefreshScope
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ IOUtils.class, HMACUtils2.class, ZipUtils.class})
public class PacketUploaderServiceTest {

	@InjectMocks
	private PacketUploaderService<MessageDTO> packetuploaderservice = new PacketUploaderServiceImpl();
	@Mock
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;
	/** The audit log request builder. */
	@Mock
	private AuditLogRequestBuilder auditLogRequestBuilder;

	/** The registration status service. */
	@Mock
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The env. */
	@Mock
	private Environment env;

	@Mock
	private ObjectStoreAdapter objectStoreAdapter;
	
	/** The dto. */
	MessageDTO dto = new MessageDTO();

	private SyncRegistrationEntity regEntity;

	/** The entry. */
	InternalRegistrationStatusDto entry = new InternalRegistrationStatusDto();

	@Mock
	private InputStream is;

	@Mock
	private LogDescription description;

	@Mock
	private RegistrationExceptionMapperUtil registrationStatusMapperUtil;

	@Mock
	private Decryptor decryptor;

	@Mock
	private ObjectMapper mapper;

	@Mock
	private VirusScanner<Boolean, InputStream> virusScannerService;

	@Mock
	private SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationService;

	@Mock
    private AdditionalInfoRequestService additionalInfoRequestService;

	@Mock
    private Utilities utility;

	private File file;

	@Before
	public void setUp() throws IOException, ApisResourceAccessException, JsonProcessingException, NoSuchAlgorithmException {
		ReflectionTestUtils.setField(packetuploaderservice, "packetNames", "id,optional,evidence");
		ReflectionTestUtils.setField(packetuploaderservice, "landingZoneType", "DMZServer");
		ReflectionTestUtils.setField(packetuploaderservice, "landingZoneAccount", "LandingZoneAccount");
		
		file = new File("src/test/resources/1001.zip");
		dto.setRid("1001");
		dto.setReg_type("NEW");
		dto.setIteration(1);
		entry.setRegistrationId("1001");
		entry.setRetryCount(0);
		entry.setStatusComment("virus scan");
		regEntity = new SyncRegistrationEntity();
		regEntity.setCreateDateTime(LocalDateTime.now());
		regEntity.setCreatedBy("Mosip");
		regEntity.setWorkflowInstanceId("001");
		regEntity.setLangCode("eng");
		regEntity.setRegistrationId("0000");
		regEntity.setRegistrationType("NEW");
		regEntity.setStatusCode("NEW_REGISTRATION");
		regEntity.setStatusComment("registration begins");
		regEntity.setPacketHashValue("abcd1234");
		regEntity.setRegistrationType("NEW");
		BigInteger size = new BigInteger("2291584");
		regEntity.setPacketSize(size);
		is = new FileInputStream(file);
		IOUtils.toByteArray(is);
		PowerMockito.mockStatic(HMACUtils2.class);
		PowerMockito.when(HMACUtils2.digestAsPlainText(any())).thenReturn("abcd1234");
		Mockito.when(syncRegistrationService.findByWorkflowInstanceId(Mockito.any())).thenReturn(regEntity);
		ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<>();
		Mockito.doReturn(responseWrapper).when(auditLogRequestBuilder).createAuditRequestBuilder(
				"test case description", EventId.RPR_401.toString(), EventName.ADD.toString(),
				EventType.BUSINESS.toString(), "1234testcase", ApiName.AUDIT);
		Mockito.doNothing().when(description).setMessage(any());
		Mockito.when(description.getMessage()).thenReturn("hello");
		Mockito.when(registrationProcessorRestService.getApi(
				any(), anyList(), anyString(), any(), any())).thenReturn(new byte[2]);
		Mockito.when(objectStoreAdapter.putObject(any(), any(), any(), any(), any(), any())).thenReturn(true);
		Mockito.when(objectStoreAdapter.addObjectMetaData(any(), any(), any(), any(), any(), any())).thenReturn(new HashMap<>());
		Mockito.when(objectStoreAdapter.putObject(any(), any(), any(), any(), any(), any())).thenReturn(true);		
		Mockito.when(objectStoreAdapter.getObject(any(), any(), any(), any(), any())).thenReturn(new FileInputStream(file));		
		
		AdditionalInfoRequestDto additionalInfoRequestDto = new AdditionalInfoRequestDto();
		additionalInfoRequestDto.setAdditionalInfoReqId("1001-BIOMETRIC_CORRECTION-1");

		Mockito.when(additionalInfoRequestService.getAdditionalInfoRequestByRegIdAndProcessAndIteration(anyString(),
				anyString(), anyInt())).thenReturn(additionalInfoRequestDto);

		Map<String, Object> jsonObject = new LinkedHashMap<>();
		jsonObject.put("id", "2345");
		jsonObject.put("email", "mono@mono.com");
		Map<String, InputStream> entryMap = new HashMap<>();
		entryMap.put("id.zip", new ByteArrayInputStream("123".getBytes()));
		entryMap.put("id.json", new ByteArrayInputStream(JsonUtils.javaObjectToJsonString(jsonObject).getBytes()));

		Mockito.when(mapper.readValue(anyString(), any(Class.class))).thenReturn(jsonObject);
		PowerMockito.mockStatic(ZipUtils.class);
		PowerMockito.when(ZipUtils.unzipAndGetFiles(any())).thenReturn(entryMap);
		Mockito.when(objectStoreAdapter.exists(any(), any(), any(), any(), any())).thenReturn(false);
		Mockito.when(utility.getDefaultSource(any(), any())).thenReturn("REGISTRATION_CLIENT");

	}

	@Test
	public void testvalidateAndUploadPacketSuccess() throws Exception {
		Mockito.when(registrationStatusService.getRegistrationStatus(Mockito.any(),Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(entry);
		ReflectionTestUtils.setField(packetuploaderservice, "maxRetryCount", 3);

		Mockito.when(virusScannerService.scanFile(Mockito.any(InputStream.class))).thenReturn(Boolean.TRUE);
		Mockito.when(decryptor.decrypt(Mockito.any(), Mockito.any(),Mockito.any())).thenReturn(is);
		MessageDTO result = packetuploaderservice.validateAndUploadPacket(dto, "PacketUploaderStage");
		assertTrue(result.getIsValid());
	}

	@Test
	public void testvalidateAndUploadPacketAdditionalInfoSuccess() throws Exception {

		SyncRegistrationEntity regEntity= new SyncRegistrationEntity();
		regEntity.setCreateDateTime(LocalDateTime.now());
		regEntity.setCreatedBy("Mosip");
		regEntity.setWorkflowInstanceId("001");
		regEntity.setLangCode("eng");
		regEntity.setRegistrationId("0000");
		regEntity.setRegistrationType("BIOMETRIC_CORRECTION");
		regEntity.setStatusComment("registration begins");
		regEntity.setPacketHashValue("abcd1234");
		regEntity.setAdditionalInfoReqId("1001-BIOMETRIC_CORRECTION-1");
		BigInteger size = new BigInteger("2291584");
		regEntity.setPacketSize(size);
		Mockito.when(syncRegistrationService.findByWorkflowInstanceId(Mockito.any())).thenReturn(regEntity);

		Map<String, Object> jsonObject = new LinkedHashMap<>();
		jsonObject.put("id", "2345");
		jsonObject.put("email", "mono@mono.com");

		Map<String, InputStream> entryMap = new HashMap<>();
		entryMap.put("REGISTRATION/BIOMETRIC_CORRECTION/id.zip", new ByteArrayInputStream("123".getBytes()));
		entryMap.put("REGISTRATION/BIOMETRIC_CORRECTION/id.json", new ByteArrayInputStream(JsonUtils.javaObjectToJsonString(jsonObject).getBytes()));

		Mockito.when(mapper.readValue(anyString(), any(Class.class))).thenReturn(jsonObject);
		PowerMockito.mockStatic(ZipUtils.class);
		PowerMockito.when(ZipUtils.unzipAndGetFiles(any())).thenReturn(entryMap);

		Mockito.when(registrationStatusService.getRegistrationStatus(Mockito.any(),Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(entry);
		ReflectionTestUtils.setField(packetuploaderservice, "maxRetryCount", 3);

		Mockito.when(virusScannerService.scanFile(Mockito.any(InputStream.class))).thenReturn(Boolean.TRUE);
		Mockito.when(decryptor.decrypt(Mockito.any(), Mockito.any(),Mockito.any())).thenReturn(is);
		MessageDTO result = packetuploaderservice.validateAndUploadPacket(dto, "PacketUploaderStage");
		assertTrue(result.getIsValid());
	}

	@Test
	public void testvalidateAndUploadPacketFailureRetry() throws Exception {
		Mockito.when(registrationStatusService.getRegistrationStatus(Mockito.any(),Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(entry);
		Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.PACKET_UPLOAD_FAILED_ON_MAX_RETRY_CNT))
		.thenReturn("FAILED");
		Mockito.when(virusScannerService.scanFile(Mockito.any(InputStream.class))).thenReturn(Boolean.TRUE);
		Mockito.when(decryptor.decrypt(Mockito.any(), Mockito.any(),Mockito.any())).thenReturn(is);
		MessageDTO result = packetuploaderservice.validateAndUploadPacket(dto, "PacketUploaderStage");
		assertFalse(result.getIsValid());
		assertTrue(result.getInternalError());
	}

	@Test
	public void testTableNotaccessibleException() throws Exception {
		Mockito.when(registrationStatusService.getRegistrationStatus(Mockito.any(),Mockito.any(),Mockito.any(), Mockito.any()))
				.thenThrow(new TablenotAccessibleException());
		Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.TABLE_NOT_ACCESSIBLE_EXCEPTION))
		.thenReturn("REPROCESS");
		Mockito.when(virusScannerService.scanFile(Mockito.any(InputStream.class))).thenReturn(Boolean.TRUE);
		Mockito.when(decryptor.decrypt(Mockito.any(), Mockito.any(),Mockito.any())).thenReturn(is);
		MessageDTO result = packetuploaderservice.validateAndUploadPacket(dto, "PacketUploaderStage");
		assertTrue(result.getIsValid());
		assertTrue(result.getInternalError());
	}

	@Test
	public void testvalidateHashCodeFailed() throws Exception {
		PowerMockito.mockStatic(HMACUtils2.class);
		PowerMockito.when(HMACUtils2.digestAsPlainText(any())).thenReturn("abcd123");
		Mockito.when(registrationStatusService.getRegistrationStatus(Mockito.any(),Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(entry);
		ReflectionTestUtils.setField(packetuploaderservice, "maxRetryCount", 3);

		Mockito.when(virusScannerService.scanFile(Mockito.any(InputStream.class))).thenReturn(Boolean.TRUE);
		Mockito.when(decryptor.decrypt(Mockito.any(), Mockito.any(),Mockito.any())).thenReturn(is);
		MessageDTO result = packetuploaderservice.validateAndUploadPacket(dto, "PacketUploaderStage");
		assertFalse(result.getIsValid());
		assertFalse(result.getInternalError());
	}

	@Test
	public void testPacketNotFoundException() throws ApisResourceAccessException {
		Mockito.when(registrationStatusService.getRegistrationStatus(Mockito.any(),Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(entry);
		Mockito.when(registrationProcessorRestService.getApi(
				any(), anyList(), anyString(), any(), any())).thenThrow(
						new ApisResourceAccessException("exception", new HttpClientErrorException(HttpStatus.NOT_FOUND)));
		Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.PACKET_NOT_FOUND_EXCEPTION))
		.thenReturn("ERROR");
		MessageDTO result = packetuploaderservice.validateAndUploadPacket(dto, "PacketUploaderStage");
		assertFalse(result.getIsValid());
		assertTrue(result.getInternalError());
	}

	@Test
	public void testObjectStoreException() throws Exception {
		ReflectionTestUtils.setField(packetuploaderservice, "maxRetryCount", 3);
		Mockito.when(registrationStatusService.getRegistrationStatus(Mockito.any(),Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(entry);
		Mockito.when(virusScannerService.scanFile(Mockito.any(InputStream.class))).thenReturn(Boolean.TRUE);
		Mockito.when(decryptor.decrypt(Mockito.any(), Mockito.any(),Mockito.any())).thenReturn(is);
		Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.OBJECT_STORE_EXCEPTION))
		.thenReturn("REPROCESS");
		Mockito.when(objectStoreAdapter.putObject(any(), any(), any(), any(), any(), any())).thenThrow(FileNotFoundInDestinationException.class);

		MessageDTO result = packetuploaderservice.validateAndUploadPacket(dto, "PacketUploaderStage");

		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void testObjectStoreSaveFailed() throws Exception {
		ReflectionTestUtils.setField(packetuploaderservice, "maxRetryCount", 3);
		Mockito.when(registrationStatusService.getRegistrationStatus(Mockito.any(),Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(entry);
		Mockito.when(virusScannerService.scanFile(Mockito.any(InputStream.class))).thenReturn(Boolean.TRUE);
		Mockito.when(decryptor.decrypt(Mockito.any(), Mockito.any(),Mockito.any())).thenReturn(is);
		Mockito.when(objectStoreAdapter.putObject(any(), any(), any(), any(), any(), any())).thenReturn(false);
		Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.OBJECT_STORE_EXCEPTION))
		.thenReturn("REPROCESS");
		MessageDTO result = packetuploaderservice.validateAndUploadPacket(dto, "PacketUploaderStage");
		assertTrue(result.getIsValid());
		assertTrue(result.getInternalError());
	}

	@Test
	public void test() {
		Mockito.when(registrationStatusService.getRegistrationStatus(Mockito.any(),Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(entry);
		Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.EXCEPTION))
		.thenReturn("ERROR");
		MessageDTO result = packetuploaderservice.validateAndUploadPacket(dto, "PacketUploaderStage");
		assertFalse(result.getIsValid());
		assertTrue(result.getInternalError());
	}

	@Test
	public void testPacketDecryptionException() throws ApisResourceAccessException, PacketDecryptionFailureException {
		Mockito.when(registrationStatusService.getRegistrationStatus(Mockito.any(),Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(entry);
		Mockito.when(virusScannerService.scanFile(Mockito.any(InputStream.class))).thenReturn(Boolean.TRUE);
		Mockito.when(decryptor.decrypt(Mockito.any(), Mockito.any(),Mockito.any()))
				.thenThrow(new PacketDecryptionFailureException("", ""));
		Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.PACKET_DECRYPTION_FAILURE_EXCEPTION))
		.thenReturn("ERROR");
		MessageDTO result = packetuploaderservice.validateAndUploadPacket(dto, "PacketUploaderStage");
		assertFalse(result.getIsValid());
		assertTrue(result.getInternalError());
	}

	@Test
	public void testIOException() throws IOException, io.mosip.registration.processor.core.exception.PacketDecryptionFailureException, ApisResourceAccessException {
		ReflectionTestUtils.setField(packetuploaderservice, "maxRetryCount", 3);
		Mockito.when(registrationStatusService.getRegistrationStatus(Mockito.any(),Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(entry);
		Mockito.when(virusScannerService.scanFile(Mockito.any(InputStream.class))).thenReturn(Boolean.TRUE);
		Mockito.when(decryptor.decrypt(Mockito.any(), Mockito.any(),Mockito.any())).thenReturn(is);
		PowerMockito.mockStatic(IOUtils.class);
		PowerMockito.when(IOUtils.toByteArray(any(InputStream.class))).thenThrow(new IOException("IO execption occured"));
		Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.IOEXCEPTION))
		.thenReturn("ERROR");
		MessageDTO result = packetuploaderservice.validateAndUploadPacket(dto, "PacketUploaderStage");
		assertFalse(result.getIsValid());
		assertTrue(result.getInternalError());
	}

	@Test
	public void testUploadfailure() throws Exception {
		Mockito.when(registrationStatusService.getRegistrationStatus(Mockito.any(),Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(entry);
		ReflectionTestUtils.setField(packetuploaderservice, "maxRetryCount", 3);
		Mockito.when(virusScannerService.scanFile(Mockito.any(InputStream.class))).thenReturn(Boolean.TRUE);
		Mockito.when(decryptor.decrypt(Mockito.any(), Mockito.any(),Mockito.any())).thenReturn(is);
		Mockito.when(objectStoreAdapter.putObject(any(),any(), any(), any(), any(),any())).thenReturn(false);
		Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.OBJECT_STORE_EXCEPTION))
		.thenReturn("REPROCESS");
		MessageDTO result = packetuploaderservice.validateAndUploadPacket(dto, "PacketUploaderStage");
		assertTrue(result.getIsValid());
		assertTrue(result.getInternalError());
	}

	@Test
	public void testVirusScanFailedException() throws PacketDecryptionFailureException, ApisResourceAccessException {
		Mockito.when(registrationStatusService.getRegistrationStatus(
				Mockito.any(),Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(entry);
		
		Mockito.when(virusScannerService.scanFile(Mockito.any(InputStream.class))).thenReturn(Boolean.FALSE);
		Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.VIRUS_SCAN_FAILED_EXCEPTION))
		.thenReturn("FAILED");
		Mockito.when(decryptor.decrypt(Mockito.any(), Mockito.any(),Mockito.any())).thenReturn(is);
		MessageDTO result = packetuploaderservice.validateAndUploadPacket(dto, "");
		assertFalse(result.getIsValid());
		assertFalse(result.getInternalError());
	}

	@Test
	public void testScannerServiceFailedException() throws PacketDecryptionFailureException, ApisResourceAccessException {
		Mockito.when(registrationStatusService.getRegistrationStatus(Mockito.any(),Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(entry);
		Mockito.when(virusScannerService.scanFile(Mockito.any(InputStream.class)))
				.thenThrow(new VirusScannerException());
		Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.VIRUS_SCANNER_SERVICE_FAILED))
		.thenReturn("FAILED");
		Mockito.when(decryptor.decrypt(Mockito.any(), Mockito.any(),Mockito.any())).thenReturn(is);
		MessageDTO result = packetuploaderservice.validateAndUploadPacket(dto, "PacketUploaderStage");
		assertFalse(result.getIsValid());
		assertTrue(result.getInternalError());
	}

	@Test
	public void testScannerServiceAPIResourceException() throws PacketDecryptionFailureException, ApisResourceAccessException {
		Mockito.when(registrationStatusService.getRegistrationStatus(Mockito.any(),Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(entry);
		
		Mockito.when(virusScannerService.scanFile(Mockito.any(InputStream.class)))
				.thenReturn(true);
		Mockito.when(registrationProcessorRestService.getApi(
				any(), anyList(), anyString(), any(), any())).thenThrow(new ApisResourceAccessException("exception"));
		Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.NGINX_ACCESS_EXCEPTION))
		.thenReturn("REPROCESS");
		MessageDTO result = packetuploaderservice.validateAndUploadPacket(dto, "PacketUploaderStage");
		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());
	}
	@Test
	public void testException() {
		Mockito.when(registrationStatusService.getRegistrationStatus(Mockito.any(),Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(entry);
		Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.EXCEPTION))
		.thenReturn("ERROR");
		MessageDTO result = packetuploaderservice.validateAndUploadPacket(dto, "PacketUploaderStage");
		assertFalse(result.getIsValid());
		assertTrue(result.getInternalError());
	}

	@Test
	public void testPacketUploaderFailed() {
		Mockito.when(registrationStatusService.getRegistrationStatus(Mockito.any(),Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(entry);
		Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.EXCEPTION))
		.thenReturn("ERROR");
		MessageDTO result = packetuploaderservice.validateAndUploadPacket(dto, "PacketUploaderStage");
		assertFalse(result.getIsValid());
		assertTrue(result.getInternalError());
	}

	@Test
	public void testNginxServerException() throws ApisResourceAccessException {
		ApisResourceAccessException apiException = new ApisResourceAccessException("Packet not found in nginx");

		Mockito.when(registrationStatusService.getRegistrationStatus(Mockito.any(),Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(entry);
		Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.NGINX_ACCESS_EXCEPTION))
		.thenReturn("REPROCESS");
		Mockito.when(registrationProcessorRestService.getApi(
				any(), anyList(), anyString(), any(), any())).thenThrow(apiException);
		MessageDTO result = packetuploaderservice.validateAndUploadPacket(dto, "PacketUploaderStage");
		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void testGetPacketFromNginxFailed() throws ApisResourceAccessException {
		HttpClientErrorException e = new HttpClientErrorException(HttpStatus.NOT_FOUND);
		ApisResourceAccessException apiException = new ApisResourceAccessException("Packet not found in nginx", e);
		Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.PACKET_NOT_FOUND_EXCEPTION))
		.thenReturn("ERROR");
		Mockito.when(registrationStatusService.getRegistrationStatus(Mockito.any(),Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(entry);
		Mockito.when(registrationProcessorRestService.getApi(
				any(), anyList(), anyString(), any(), any())).thenThrow(apiException);
		MessageDTO result = packetuploaderservice.validateAndUploadPacket(dto, "PacketUploaderStage");
		assertTrue(result.getInternalError());
		assertFalse(result.getIsValid());
	}

	@Test
	public void testUnknownExceptionOccured() throws NoSuchAlgorithmException {
		BaseUncheckedException exception = new BaseUncheckedException("Unknown");
		Mockito.when(registrationStatusService.getRegistrationStatus(Mockito.any(),Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(entry);
		PowerMockito.mockStatic(HMACUtils2.class);
		PowerMockito.when(HMACUtils2.digestAsPlainText(any())).thenThrow(exception);
		Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.EXCEPTION))
		.thenReturn("ERROR");
		MessageDTO result = packetuploaderservice.validateAndUploadPacket(dto, "PacketUploaderStage");
		assertFalse(result.getIsValid());
		assertTrue(result.getInternalError());
	}

	@Test
	public void testNullPacketFromDMZ() throws ApisResourceAccessException {
		Mockito.when(registrationStatusService.getRegistrationStatus(Mockito.any(),Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(entry);
		Mockito.when(registrationProcessorRestService.getApi(
				any(), anyList(), anyString(), any(), any())).thenReturn(null);
		Mockito.when(registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.PACKET_NOT_FOUND_EXCEPTION))
		.thenReturn("ERROR");
		MessageDTO result = packetuploaderservice.validateAndUploadPacket(dto, "PacketUploaderStage");
		assertTrue(result.getInternalError());
		assertFalse(result.getIsValid());
	}
	@Test
	public void testObjectStorePacketFromDMZ() throws ApisResourceAccessException, PacketDecryptionFailureException {
		ReflectionTestUtils.setField(packetuploaderservice, "landingZoneType", "ObjectStore");
		Mockito.when(registrationStatusService.getRegistrationStatus(Mockito.any(),Mockito.any(),Mockito.any(), Mockito.any())).thenReturn(entry);
		ReflectionTestUtils.setField(packetuploaderservice, "maxRetryCount", 3);

		Mockito.when(virusScannerService.scanFile(Mockito.any(InputStream.class))).thenReturn(Boolean.TRUE);
		Mockito.when(decryptor.decrypt(Mockito.any(), Mockito.any(),Mockito.any())).thenReturn(is);
		MessageDTO result = packetuploaderservice.validateAndUploadPacket(dto, "PacketUploaderStage");
		assertTrue(result.getIsValid());
	}
	

	@Test
	public void testPacketNotFoundInLandingZoneButAlreadyPresentInObjectStore() throws ApisResourceAccessException {
		Mockito.when(registrationStatusService.getRegistrationStatus(Mockito.any(),Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(entry);

		Mockito.when(registrationProcessorRestService.getApi(
				any(), anyList(), anyString(), any(), any())).thenThrow(
				new ApisResourceAccessException("exception", new HttpClientErrorException(HttpStatus.NOT_FOUND)));
		Mockito.when(objectStoreAdapter.exists(any(), any(), any(), any(), any())).thenReturn(true);

		MessageDTO result = packetuploaderservice.validateAndUploadPacket(dto, "PacketUploaderStage");
		assertTrue(result.getIsValid());
	}

}
