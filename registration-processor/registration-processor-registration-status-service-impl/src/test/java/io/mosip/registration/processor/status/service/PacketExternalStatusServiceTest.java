package io.mosip.registration.processor.status.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.registration.processor.core.code.RegistrationTransactionTypeCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.PacketExternalStatusDTO;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.service.impl.PacketExternalStatusServiceImpl;

@RunWith(MockitoJUnitRunner.class)
@DataJpaTest
@RefreshScope
@ContextConfiguration
public class PacketExternalStatusServiceTest {
	
	private List<SyncRegistrationEntity> syncRegistrationEntities;
	
	@Mock
	SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationService;
	
	@Mock
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;
	
	InternalRegistrationStatusDto internalRegistrationStatusDto;
	
	@InjectMocks
	PacketExternalStatusService packetExternalStatusService=new PacketExternalStatusServiceImpl();
	
	@Before
	public void setup() {
		List<String> transactionTypeCodesBeforeUploadingToObjectStoreList=new ArrayList<>();
		transactionTypeCodesBeforeUploadingToObjectStoreList.add("PACKET_RECEIVER");
		transactionTypeCodesBeforeUploadingToObjectStoreList.add("SECUREZONE_NOTIFICATION");
		List<String> transactionTypeCodeTimeBasesResendRequiredList=new ArrayList<>();
		transactionTypeCodeTimeBasesResendRequiredList.add("PACKET_RECEIVER");
		
		ReflectionTestUtils.setField(packetExternalStatusService, "transactionTypeCodesBeforeUploadingToObjectStore", transactionTypeCodesBeforeUploadingToObjectStoreList);
		ReflectionTestUtils.setField(packetExternalStatusService, "transactionTypeCodeUploadingToObjectStore", "UPLOAD_PACKET");
		ReflectionTestUtils.setField(packetExternalStatusService, "transactionTypeCodesTimeBasedResendRequired", transactionTypeCodeTimeBasesResendRequiredList);
		ReflectionTestUtils.setField(packetExternalStatusService, "maxRetryCount", 10);
		syncRegistrationEntities = new ArrayList<>();
		SyncRegistrationEntity syncRegistrationEntity = new SyncRegistrationEntity();
		syncRegistrationEntity.setWorkflowInstanceId("0c326dc2-ac54-4c2a-98b4-b0c620f1661f");
		syncRegistrationEntity.setRegistrationId("27847657360002520181208183050");
		syncRegistrationEntity.setRegistrationType("NEW");

		syncRegistrationEntity.setPacketId("packetId1");
		syncRegistrationEntities.add(syncRegistrationEntity);
		internalRegistrationStatusDto=new InternalRegistrationStatusDto();
		internalRegistrationStatusDto.setRegistrationId("27847657360002520181208183050");
		internalRegistrationStatusDto.setWorkflowInstanceId("0c326dc2-ac54-4c2a-98b4-b0c620f1661f");
		internalRegistrationStatusDto.setRegistrationType("NEW");
		internalRegistrationStatusDto.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.SECUREZONE_NOTIFICATION.toString());
		internalRegistrationStatusDto.setStatusCode("REPROCESS");
		internalRegistrationStatusDto.setRetryCount(0);
		Mockito.when(syncRegistrationService.getByPacketIds(any())).thenReturn(syncRegistrationEntities);
		Mockito.when(registrationStatusService.getRegistrationStatus(any(),any(),any(),any())).thenReturn(internalRegistrationStatusDto);
	}
	
	@Test
	public void testGetByPacketIdsSuccess() {
		List<String> packetIdList = new ArrayList<>();
		packetIdList.add("packetId1");
		List<PacketExternalStatusDTO> packetExternalStatusDTOList=packetExternalStatusService.getByPacketIds(packetIdList);

		assertEquals("RECEIVED", packetExternalStatusDTOList.get(0).getStatusCode());
	}
	
	@Test
	public void testGetByPacketIdsPacketReceiverSuccess() {
		internalRegistrationStatusDto.setLatestTransactionTimes(LocalDateTime.now().minusSeconds(100));
		internalRegistrationStatusDto
				.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.PACKET_RECEIVER.toString());
		List<String> packetIdList = new ArrayList<>();
		packetIdList.add("packetId1");
		List<PacketExternalStatusDTO> packetExternalStatusDTOList = packetExternalStatusService
				.getByPacketIds(packetIdList);

		assertEquals("RESEND", packetExternalStatusDTOList.get(0).getStatusCode());
	}
	
	@Test
	public void testGetByPacketIdsWithResend() {
		internalRegistrationStatusDto.setStatusCode("FAILED");
		List<String> packetIdList = new ArrayList<>();
		packetIdList.add("packetId1");
		List<PacketExternalStatusDTO> packetExternalStatusDTOList=packetExternalStatusService.getByPacketIds(packetIdList);

		assertEquals("RESEND", packetExternalStatusDTOList.get(0).getStatusCode());
	}
	
	@Test
	public void testGetByPacketIdsWithPaused() {
		internalRegistrationStatusDto.setStatusCode("PAUSED");
		internalRegistrationStatusDto.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.PACKET_CLASSIFICATION.toString());
		List<String> packetIdList = new ArrayList<>();
		packetIdList.add("packetId1");
		List<PacketExternalStatusDTO> packetExternalStatusDTOList=packetExternalStatusService.getByPacketIds(packetIdList);

		assertEquals("ACCEPTED", packetExternalStatusDTOList.get(0).getStatusCode());
	}
	@Test
	public void testGetByPacketIdsWithReprocessFailed() {
		internalRegistrationStatusDto.setStatusCode("REPROCESS_FAILED");
		List<String> packetIdList = new ArrayList<>();
		packetIdList.add("packetId1");
		List<PacketExternalStatusDTO> packetExternalStatusDTOList=packetExternalStatusService.getByPacketIds(packetIdList);

		assertEquals("RECEIVED", packetExternalStatusDTOList.get(0).getStatusCode());
	}
	@Test
	public void testGetByPacketIdsWithUploadPending() {
		Mockito.when(registrationStatusService.getRegistrationStatus(any(),any(),any(),any())).thenReturn(null);
		List<String> packetIdList = new ArrayList<>();
		packetIdList.add("packetId1");
		List<PacketExternalStatusDTO> packetExternalStatusDTOList=packetExternalStatusService.getByPacketIds(packetIdList);

		assertEquals("UPLOAD_PENDING", packetExternalStatusDTOList.get(0).getStatusCode());
	}
	@Test
	public void testGetByPacketIdsWithMaxRetryCount() {
		internalRegistrationStatusDto.setStatusCode("FAILED");
		internalRegistrationStatusDto.setRetryCount(11);
		List<String> packetIdList = new ArrayList<>();
		packetIdList.add("packetId1");
		List<PacketExternalStatusDTO> packetExternalStatusDTOList=packetExternalStatusService.getByPacketIds(packetIdList);

		assertEquals("REJECTED", packetExternalStatusDTOList.get(0).getStatusCode());
	}
	@Test
	public void testGetByPacketIdsWithUploadingToObjectStoreReprocessFailed() {
		internalRegistrationStatusDto.setStatusCode("REPROCESS_FAILED");
		internalRegistrationStatusDto.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.UPLOAD_PACKET.toString());
		List<String> packetIdList = new ArrayList<>();
		packetIdList.add("packetId1");
		List<PacketExternalStatusDTO> packetExternalStatusDTOList=packetExternalStatusService.getByPacketIds(packetIdList);

		assertEquals("ACCEPTED", packetExternalStatusDTOList.get(0).getStatusCode());
	}
}
