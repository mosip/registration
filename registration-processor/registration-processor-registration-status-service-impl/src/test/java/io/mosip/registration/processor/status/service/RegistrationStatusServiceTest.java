package io.mosip.registration.processor.status.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;

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

import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.dataaccess.hibernate.constant.HibernateErrorCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationExternalStatusCode;
import io.mosip.registration.processor.status.dao.RegistrationStatusDao;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusSubRequestDto;
import io.mosip.registration.processor.status.dto.TransactionDto;
import io.mosip.registration.processor.status.entity.BaseRegistrationPKEntity;
import io.mosip.registration.processor.status.entity.RegistrationStatusEntity;
import io.mosip.registration.processor.status.entity.TransactionEntity;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.impl.RegistrationStatusServiceImpl;
import io.mosip.registration.processor.status.utilities.RegistrationExternalStatusUtility;

@RunWith(MockitoJUnitRunner.class)
@DataJpaTest
@RefreshScope
@ContextConfiguration
public class RegistrationStatusServiceTest {

	private InternalRegistrationStatusDto registrationStatusDto;
	private RegistrationStatusEntity registrationStatusEntity;
	private List<RegistrationStatusEntity> entities;

	@InjectMocks
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService = new RegistrationStatusServiceImpl();

	@Mock
	TransactionService<TransactionDto> transcationStatusService;
	@Mock
	private RegistrationStatusDao registrationStatusDao;

	@Mock
	private AuditLogRequestBuilder auditLogRequestBuilder;

	@Mock
	private RegistrationExternalStatusUtility regexternalstatusUtil;

	@Mock
	LogDescription description;

	List<RegistrationStatusDto> registrations = new ArrayList<>();

	List<String> statusList;

	@Before
	public void setup()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setIsActive(true);
		registrationStatusDto.setStatusCode("PACKET_UPLOADED_TO_VIRUS_SCAN");
		registrationStatusDto.setCreateDateTime(LocalDateTime.now());
		registrationStatusDto.setRegistrationStageName("PacketValidatorStage");
		registrationStatusDto.setReProcessRetryCount(0);
		registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
		registrationStatusEntity = new RegistrationStatusEntity();
		registrationStatusEntity.setIsActive(true);
		BaseRegistrationPKEntity pk = new BaseRegistrationPKEntity();
		pk.setId("1000");
		pk.setIteration(1);
		pk.setRegistrationType("NEW");
		registrationStatusEntity.setId(pk);
		registrationStatusEntity.setStatusCode("PACKET_UPLOADED_TO_LANDING_ZONE");
		registrationStatusEntity.setRetryCount(2);
		registrationStatusEntity.setRegistrationStageName("PacketValidatorStage");

		registrationStatusEntity.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
		entities = new ArrayList<>();
		entities.add(registrationStatusEntity);

		Mockito.when(registrationStatusDao.find(any(),any(),any(),any())).thenReturn(registrationStatusEntity);

		TransactionEntity transactionEntity = new TransactionEntity();
		transactionEntity.setStatusCode("PROCESSING");
		transactionEntity.setId("1001");
		Mockito.when(transcationStatusService.addRegistrationTransaction(any())).thenReturn(transactionEntity);
		// Mockito.when(registrationStatusMapUtil.getExternalStatus(ArgumentMatchers.any(),
		// ArgumentMatchers.any()))
		// .thenReturn(RegistrationExternalStatusCode.RESEND);
		Mockito.when(registrationStatusDao.getByIds(any())).thenReturn(entities);

	}

	@Test
	public void testGetRegistrationStatusSuccess() {

		InternalRegistrationStatusDto dto = registrationStatusService.getRegistrationStatus("1001", "NEW", 1, "");
		assertEquals("PACKET_UPLOADED_TO_LANDING_ZONE", dto.getStatusCode());

	}

	@Test(expected = TablenotAccessibleException.class)
	public void getRegistrationStatusFailureTest() throws TablenotAccessibleException {
		DataAccessLayerException exp = new DataAccessLayerException(HibernateErrorCode.ERR_DATABASE.getErrorCode(),
				"errorMessage", new Exception());
			Mockito.when(registrationStatusDao.find(any(),any(),any(), any())).thenThrow(exp);
			registrationStatusService.getRegistrationStatus("1001", "NEW", 1, "");
	}

	@Test
	public void testAddRegistrationStatusSuccess() {

		registrationStatusService.addRegistrationStatus(registrationStatusDto, "", "");
		InternalRegistrationStatusDto dto = registrationStatusService.getRegistrationStatus("1001", "NEW", 1, "");
		assertEquals("PACKET_UPLOADED_TO_LANDING_ZONE", dto.getStatusCode());
	}

	@Test(expected = TablenotAccessibleException.class)
	public void addRegistrationFailureTest() {
		DataAccessLayerException exp = new DataAccessLayerException(HibernateErrorCode.ERR_DATABASE.getErrorCode(),
				"errorMessage", new Exception());
		Mockito.when(registrationStatusDao.save(any())).thenThrow(exp);
		registrationStatusService.addRegistrationStatus(registrationStatusDto, "", "");
	}

	@Test
	public void testUpdateRegistrationStatusSuccess() {
		registrationStatusService.updateRegistrationStatus(registrationStatusDto, "", "");

		InternalRegistrationStatusDto dto = registrationStatusService.getRegistrationStatus("1001", "NEW", 1, "");
		assertEquals("PACKET_UPLOADED_TO_LANDING_ZONE", dto.getStatusCode());
	}

	@Test(expected = TablenotAccessibleException.class)
	public void updateRegistrationStatusFailureTest() {
		DataAccessLayerException exp = new DataAccessLayerException(HibernateErrorCode.ERR_DATABASE.getErrorCode(),
				"errorMessage", new Exception());

		Mockito.when(registrationStatusDao.save(any())).thenThrow(exp);
		registrationStatusService.updateRegistrationStatus(registrationStatusDto, "", "");
	}

	@Test
	public void testGetByStatusSuccess() {
		Mockito.when(registrationStatusDao.getEnrolmentStatusByStatusCode(any())).thenReturn(entities);
		List<InternalRegistrationStatusDto> list = registrationStatusService
				.getByStatus("PACKET_UPLOADED_TO_LANDING_ZONE");
		assertEquals("PACKET_UPLOADED_TO_LANDING_ZONE", list.get(0).getStatusCode());
	}

	@Test(expected = TablenotAccessibleException.class)
	public void getByStatusFailureTest() {
		DataAccessLayerException exp = new DataAccessLayerException(HibernateErrorCode.ERR_DATABASE.getErrorCode(),
				"errorMessage", new Exception());
		Mockito.when(registrationStatusDao.getEnrolmentStatusByStatusCode(any())).thenThrow(exp);
		registrationStatusService.getByStatus("PACKET_UPLOADED_TO_LANDING_ZONE");
	}

	@Test
	public void testGetByIdsSuccess() {

		Mockito.when(registrationStatusDao.getByIds(any())).thenReturn(entities);
		Mockito.when(regexternalstatusUtil.getExternalStatus(any()))
				.thenReturn(RegistrationExternalStatusCode.PROCESSED);
		RegistrationStatusSubRequestDto registrationId = new RegistrationStatusSubRequestDto();
		registrationId.setRegistrationId("1001");
		List<RegistrationStatusSubRequestDto> registrationIds = new ArrayList<>();
		registrationIds.add(registrationId);
		List<RegistrationStatusDto> list = registrationStatusService.getByIds(registrationIds);
		assertEquals("PROCESSED", list.get(0).getStatusCode());
	}

	@Test(expected = TablenotAccessibleException.class)
	public void getByIdsFailureTest() {
		RegistrationStatusSubRequestDto registrationId = new RegistrationStatusSubRequestDto();
		registrationId.setRegistrationId("1001");
		List<RegistrationStatusSubRequestDto> registrationIds = new ArrayList<>();
		registrationIds.add(registrationId);

		DataAccessLayerException exp = new DataAccessLayerException(HibernateErrorCode.ERR_DATABASE.getErrorCode(),
				"errorMessage", new Exception());
		Mockito.when(registrationStatusDao.getByIds(any())).thenThrow(exp);

		registrationStatusService.getByIds(registrationIds);

	}

	@Test
	public void testGetUnProcessedPacketsCount() {
		List<String> statusList = new ArrayList<>();
		statusList.add("SUCCESS");
		statusList.add("REPROCESS");
		Mockito.when(registrationStatusDao.getUnProcessedPacketsCount(anyLong(), anyInt(), anyList())).thenReturn(1);
		int packetCount = registrationStatusService.getUnProcessedPacketsCount(21600, 3, statusList);
		assertEquals(1, packetCount);
	}

	@Test
	public void testGetUnProcessedPackets() {

		List<String> statusList = new ArrayList<>();
		statusList.add("SUCCESS");
		statusList.add("REPROCESS");
		Mockito.when(registrationStatusDao.getUnProcessedPackets(anyInt(), anyLong(), anyInt(), anyList()))
				.thenReturn(entities);
		List<InternalRegistrationStatusDto> dtolist = registrationStatusService.getUnProcessedPackets(1, 21600, 3,
				statusList);
		assertEquals("REPROCESS", dtolist.get(0).getLatestTransactionStatusCode());
	}
	
	@Test
	public void testGetPausedPackets() {
		registrationStatusEntity.setStatusCode("PAUSED");
		Mockito.when(registrationStatusDao.getActionablePausedPackets( anyInt() ))
				.thenReturn(List.of(registrationStatusEntity));
		List<InternalRegistrationStatusDto> dtolist = registrationStatusService.getActionablePausedPackets(1);
		assertEquals("PAUSED", dtolist.get(0).getStatusCode());
	}
	
	@Test(expected = TablenotAccessibleException.class)
	public void testGetPausedPacketsFailure() {
		DataAccessLayerException exp = new DataAccessLayerException(HibernateErrorCode.ERR_DATABASE.getErrorCode(),
				"errorMessage", new Exception());
		Mockito.when(registrationStatusDao.getActionablePausedPackets( anyInt() ))
				.thenThrow(exp);
		 registrationStatusService.getActionablePausedPackets(1);
		
	}

	@Test(expected = TablenotAccessibleException.class)
	public void getUnProcessedPacketsCountFailureTest() {
		List<String> statusList = new ArrayList<>();
		statusList.add("SUCCESS");
		DataAccessLayerException exp = new DataAccessLayerException(HibernateErrorCode.ERR_DATABASE.getErrorCode(),
				"errorMessage", new Exception());
		Mockito.when(registrationStatusDao.getUnProcessedPacketsCount(anyLong(), anyInt(), anyList())).thenThrow(exp);

		registrationStatusService.getUnProcessedPacketsCount(21600, 3, statusList);
	}

	@Test(expected = TablenotAccessibleException.class)
	public void getUnProcessedPacketsFailureTest() {
		List<String> statusList = new ArrayList<>();
		statusList.add("SUCCESS");
		DataAccessLayerException exp = new DataAccessLayerException(HibernateErrorCode.ERR_DATABASE.getErrorCode(),
				"errorMessage", new Exception());
		Mockito.when(registrationStatusDao.getUnProcessedPackets(anyInt(), anyLong(), anyInt(), anyList()))
				.thenThrow(exp);

		registrationStatusService.getUnProcessedPackets(1, 21600, 3, statusList);
	}

	@Test
	public void testGetByIdsAndTimestampSuccess() {

		Mockito.when(registrationStatusDao.getByIdsAndTimestamp(any())).thenReturn(entities);

		List<String> ids = new ArrayList<>();
		ids.add("1001");
		List<InternalRegistrationStatusDto> list = registrationStatusService.getByIdsAndTimestamp(ids);
		assertEquals("PACKET_UPLOADED_TO_LANDING_ZONE", list.get(0).getStatusCode());
	}

	@Test(expected = TablenotAccessibleException.class)
	public void getByIdsAndTimestampFailureTest() {

		DataAccessLayerException exp = new DataAccessLayerException(HibernateErrorCode.ERR_DATABASE.getErrorCode(),
				"errorMessage", new Exception());
		Mockito.when(registrationStatusDao.getByIdsAndTimestamp(any())).thenThrow(exp);
		List<String> ids = new ArrayList<>();
		ids.add("1001");
		registrationStatusService.getByIdsAndTimestamp(ids);

	}

	@Test
	public void testGetResumablePackets()
	{
		registrationStatusEntity.setStatusCode("PAUSED");
		Mockito.when(registrationStatusDao.getResumablePackets(anyInt()))
				.thenReturn(List.of(registrationStatusEntity));
		List<InternalRegistrationStatusDto> dtolist = registrationStatusService.getResumablePackets(1);
		assertEquals("PAUSED", dtolist.get(0).getStatusCode());
	}

	@Test(expected = TablenotAccessibleException.class)
	public void testGetResumablePacketsFailure() {
		DataAccessLayerException exp = new DataAccessLayerException(HibernateErrorCode.ERR_DATABASE.getErrorCode(),
				"errorMessage", new Exception());
		Mockito.when(registrationStatusDao.getResumablePackets(anyInt())).thenThrow(exp);
		registrationStatusService.getResumablePackets(1);

	}

	@Test
	public void testUpdateRegistrationStatusForWorkFlowSuccess() {
		registrationStatusService.updateRegistrationStatusForWorkflow(registrationStatusDto, "", "");

		InternalRegistrationStatusDto dto = registrationStatusService.getRegistrationStatus("1001", "NEW", 1, "");
		assertEquals("PACKET_UPLOADED_TO_LANDING_ZONE", dto.getStatusCode());
	}

	@Test(expected = TablenotAccessibleException.class)
	public void updateRegistrationStatusForWorkFlowFailureTest() {
		DataAccessLayerException exp = new DataAccessLayerException(HibernateErrorCode.ERR_DATABASE.getErrorCode(),
				"errorMessage", new Exception());

		Mockito.when(registrationStatusDao.save(any())).thenThrow(exp);
		registrationStatusService.updateRegistrationStatusForWorkflow(registrationStatusDto, "", "");
	}
}