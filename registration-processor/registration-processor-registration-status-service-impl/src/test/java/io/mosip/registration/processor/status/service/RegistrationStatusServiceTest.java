package io.mosip.registration.processor.status.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.dataaccess.hibernate.constant.HibernateErrorCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.workflow.dto.FilterInfo;
import io.mosip.registration.processor.core.workflow.dto.PaginationInfo;
import io.mosip.registration.processor.core.workflow.dto.SearchInfo;
import io.mosip.registration.processor.core.workflow.dto.SortInfo;
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
	private RegistrationStatusEntity registrationExternalStatusEntity1;
	private RegistrationStatusEntity registrationExternalStatusEntity2;
	private RegistrationStatusEntity registrationExternalStatusEntity3;
	private List<RegistrationStatusEntity> entities;
	private List<RegistrationStatusEntity> externalEntities;
	private List<RegistrationStatusEntity> externalEntities1;
	private List<RegistrationStatusEntity> externalEntities2;

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
		
		ReflectionTestUtils.setField(registrationStatusService, "mainProcess", Arrays.asList("NEW", "UPDATE", "LOST"));
		
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
		pk.setWorkflowInstanceId("WorkflowInstanceId");
		registrationStatusEntity.setRegId("1000");
		registrationStatusEntity.setIteration(1);
		registrationStatusEntity.setRegistrationType("NEW");
		registrationStatusEntity.setId(pk);
		registrationStatusEntity.setStatusCode("PACKET_UPLOADED_TO_LANDING_ZONE");
		registrationStatusEntity.setRetryCount(2);
		registrationStatusEntity.setRegistrationStageName("PacketValidatorStage");

		registrationStatusEntity.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
		entities = new ArrayList<>();
		entities.add(registrationStatusEntity);
		
		registrationExternalStatusEntity1 = new RegistrationStatusEntity();
		registrationExternalStatusEntity1.setIsActive(true);
		registrationExternalStatusEntity1.setRegId("1000");
		registrationExternalStatusEntity1.setIteration(1);
		registrationExternalStatusEntity1.setRegistrationType("NEW");
		registrationExternalStatusEntity1.setId(pk);
		registrationExternalStatusEntity1.setStatusCode("PAUSED_FOR_ADDITIONAL_INFO");
		registrationExternalStatusEntity1.setCreateDateTime(LocalDateTime.now());
		registrationExternalStatusEntity1.setRetryCount(2);
		registrationExternalStatusEntity1.setRegistrationStageName("PacketValidatorStage");

		registrationExternalStatusEntity1.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
		
		registrationExternalStatusEntity2 = new RegistrationStatusEntity();
		registrationExternalStatusEntity2.setIsActive(true);
		registrationExternalStatusEntity2.setRegId("1000");
		registrationExternalStatusEntity2.setIteration(1);
		registrationExternalStatusEntity2.setRegistrationType("CORRECTION");
		registrationExternalStatusEntity2.setId(pk);
		registrationExternalStatusEntity2.setStatusCode("REJECTED");
		registrationExternalStatusEntity2.setCreateDateTime(LocalDateTime.now().plusDays(1));
		registrationExternalStatusEntity2.setRetryCount(2);
		registrationExternalStatusEntity2.setRegistrationStageName("PacketValidatorStage");

		registrationExternalStatusEntity2.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());

		registrationExternalStatusEntity3 = new RegistrationStatusEntity();
		registrationExternalStatusEntity3.setIsActive(true);
		registrationExternalStatusEntity3.setRegId("1000");
		registrationExternalStatusEntity3.setIteration(1);
		registrationExternalStatusEntity3.setRegistrationType("NEW");
		registrationExternalStatusEntity3.setId(pk);
		registrationExternalStatusEntity3.setStatusCode("PROCESSED");
		registrationExternalStatusEntity3.setCreateDateTime(LocalDateTime.now());
		registrationExternalStatusEntity3.setRetryCount(2);
		registrationExternalStatusEntity3.setRegistrationStageName("PacketValidatorStage");

		registrationExternalStatusEntity3.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.PROCESSED.toString());
		
		externalEntities = new ArrayList<>();
		externalEntities.add(registrationExternalStatusEntity1);
		externalEntities.add(registrationExternalStatusEntity2);
		
		externalEntities1 = new ArrayList<>();
		externalEntities1.add(registrationExternalStatusEntity3);
		
		externalEntities2 = new ArrayList<>();
		externalEntities2.add(registrationExternalStatusEntity1);
		
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

	@Test
	public void testGetAllRegistrationStatusesSuccess() {
		Mockito.when(registrationStatusDao.findAll(anyString())).thenReturn(entities);
		List<InternalRegistrationStatusDto> dto = registrationStatusService.getAllRegistrationStatuses("1001");
		assertEquals(1, dto.size());
	}

	@Test(expected = TablenotAccessibleException.class)
	public void testGetAllRegistrationStatusesFailure() {
		DataAccessLayerException exp = new DataAccessLayerException(HibernateErrorCode.ERR_DATABASE.getErrorCode(),
				"errorMessage", new Exception());
		Mockito.when(registrationStatusDao.findAll(anyString())).thenThrow(exp);
		registrationStatusService.getAllRegistrationStatuses("1001");
	}

	@Test
	public void testGetRegStatusForMainProcessSuccess() {
		Mockito.when(registrationStatusDao.findAll(anyString())).thenReturn(entities);
		List<InternalRegistrationStatusDto> dto = registrationStatusService.getAllRegistrationStatuses("1001");
		assertEquals(1, dto.size());
	}

	@Test(expected = TablenotAccessibleException.class)
	public void testGetRegStatusForMainProcessFailure() {
		DataAccessLayerException exp = new DataAccessLayerException(HibernateErrorCode.ERR_DATABASE.getErrorCode(),
				"errorMessage", new Exception());
		Mockito.when(registrationStatusDao.findAll(anyString())).thenThrow(exp);
		registrationStatusService.getAllRegistrationStatuses("1001");
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
	public void testSearchRegistrationDetailsSuccess() {
		SearchInfo searchInfo = new SearchInfo();
		List<FilterInfo> filterInfos = new ArrayList<FilterInfo>();
		FilterInfo filterInfo=new FilterInfo();
		filterInfo.setColumnName("name");
		filterInfo.setValue("mosip");
		SortInfo sortInfo=new SortInfo();
		sortInfo.setSortField("createDateTime");
		sortInfo.setSortType("desc");
		filterInfos.add(filterInfo);
		searchInfo.setFilters(filterInfos);
		searchInfo.setPagination(new PaginationInfo(0,10));
		searchInfo.setSort(sortInfo);

		Page<RegistrationStatusEntity> pageDto = new PageImpl<RegistrationStatusEntity>(entities);
		Mockito.when(registrationStatusDao.getPagedSearchResults(any(), any(), any())).thenReturn(pageDto);
		Page<InternalRegistrationStatusDto> idList = registrationStatusService.searchRegistrationDetails(searchInfo);
		assertEquals(1, idList.getContent().size());
	}

	@Test(expected = TablenotAccessibleException.class)
	public void testSearchRegistrationDetailsFailure() {
		SearchInfo searchInfo = new SearchInfo();
		List<FilterInfo> filterInfos = new ArrayList<FilterInfo>();
		FilterInfo filterInfo=new FilterInfo();
		filterInfo.setColumnName("name");
		filterInfo.setValue("mosip");
		SortInfo sortInfo=new SortInfo();
		sortInfo.setSortField("createDateTime");
		sortInfo.setSortType("desc");
		filterInfos.add(filterInfo);
		searchInfo.setFilters(filterInfos);
		searchInfo.setPagination(new PaginationInfo(0,10));
		searchInfo.setSort(sortInfo);

		DataAccessLayerException exp = new DataAccessLayerException(HibernateErrorCode.ERR_DATABASE.getErrorCode(),
				"errorMessage", new Exception());

		Mockito.when(registrationStatusDao.getPagedSearchResults(any(), any(), any())).thenThrow(exp);
		registrationStatusService.searchRegistrationDetails(searchInfo);
	}

	@Test
	public void testUpdateRegistrationStatusSuccess() {
		registrationStatusService.updateRegistrationStatus(registrationStatusDto, "", "");

		InternalRegistrationStatusDto dto = registrationStatusService.getRegistrationStatus("1001", "NEW", 1, "");
		assertEquals("PACKET_UPLOADED_TO_LANDING_ZONE", dto.getStatusCode());
	}
	
	@Test
	public void testupdateRegistrationStatusForWorkflowEngineSuccess() {
		registrationStatusDto.setRefId("abc");
		registrationStatusService.updateRegistrationStatusForWorkflowEngine(registrationStatusDto, "", "");

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
	
	@Test
	public void testGetByIdsStatusCodeNull() {

		registrationStatusEntity.setStatusCode(null);
		entities = new ArrayList<>();
		entities.add(registrationStatusEntity);
		Mockito.when(registrationStatusDao.getByIds(any())).thenReturn(entities);
		RegistrationStatusSubRequestDto registrationId = new RegistrationStatusSubRequestDto();
		registrationId.setRegistrationId("1001");
		List<RegistrationStatusSubRequestDto> registrationIds = new ArrayList<>();
		registrationIds.add(registrationId);
		List<RegistrationStatusDto> list = registrationStatusService.getByIds(registrationIds);
		assertEquals(null, list.get(0).getStatusCode());
	}
	
	@Test
	public void testGetExternalStatusByIdsProcessingSuccess() {

		registrationExternalStatusEntity2.setStatusCode("REPROCESS");
		Mockito.when(registrationStatusDao.getByIds(any())).thenReturn(externalEntities);
		String registartionId="1001";
		List<String> registrationIds = new ArrayList<>();
		registrationIds.add(registartionId);
		List<RegistrationStatusDto> list = registrationStatusService.getExternalStatusByIds(registrationIds);
		assertEquals("PROCESSING", list.get(0).getStatusCode());
	}
	
	@Test
	public void testGetExternalStatusByIdsAwaitingInfoSuccess() {

		Mockito.when(registrationStatusDao.getByIds(any())).thenReturn(externalEntities);
		String registartionId="1001";
		List<String> registrationIds = new ArrayList<>();
		registrationIds.add(registartionId);
		List<RegistrationStatusDto> list = registrationStatusService.getExternalStatusByIds(registrationIds);
		assertEquals("AWAITING_INFORMATION", list.get(0).getStatusCode());
	}
	
	@Test
	public void testGetExternalStatusByIdsStatusCodeNull() {

		registrationExternalStatusEntity1.setStatusCode(null);
		externalEntities = new ArrayList<>();
		externalEntities.add(registrationExternalStatusEntity1);
		Mockito.when(registrationStatusDao.getByIds(any())).thenReturn(externalEntities);
		String registartionId="1001";
		List<String> registrationIds = new ArrayList<>();
		registrationIds.add(registartionId);
		List<RegistrationStatusDto> list = registrationStatusService.getExternalStatusByIds(registrationIds);
		assertEquals(null, list.get(0).getStatusCode());
	}
	
	@Test
	public void testGetExternalStatusByIdsProcessedSuccess() {

		Mockito.when(registrationStatusDao.getByIds(any())).thenReturn(externalEntities1);
		String registartionId="1001";
		List<String> registrationIds = new ArrayList<>();
		registrationIds.add(registartionId);
		List<RegistrationStatusDto> list = registrationStatusService.getExternalStatusByIds(registrationIds);
		assertEquals("UIN_GENERATED", list.get(0).getStatusCode());
	}
	
	@Test
	public void testGetExternalStatusByIdsReprocessSuccess() {

		registrationExternalStatusEntity3.setStatusCode("REPROCESS");
		Mockito.when(registrationStatusDao.getByIds(any())).thenReturn(externalEntities1);
		String registartionId="1001";
		List<String> registrationIds = new ArrayList<>();
		registrationIds.add(registartionId);
		List<RegistrationStatusDto> list = registrationStatusService.getExternalStatusByIds(registrationIds);
		assertEquals("PROCESSING", list.get(0).getStatusCode());
	}
	
	@Test
	public void testGetExternalStatusByIdsReprocessFailedSuccess() {

		registrationExternalStatusEntity3.setStatusCode("REPROCESS_FAILED");
		Mockito.when(registrationStatusDao.getByIds(any())).thenReturn(externalEntities1);
		String registartionId="1001";
		List<String> registrationIds = new ArrayList<>();
		registrationIds.add(registartionId);
		List<RegistrationStatusDto> list = registrationStatusService.getExternalStatusByIds(registrationIds);
		assertEquals("REREGISTER", list.get(0).getStatusCode());
	}
	
	@Test
	public void testGetExternalStatusByIdsChildNotPresentSuccess() {

		Mockito.when(registrationStatusDao.getByIds(any())).thenReturn(externalEntities2);
		String registartionId="1001";
		List<String> registrationIds = new ArrayList<>();
		registrationIds.add(registartionId);
		List<RegistrationStatusDto> list = registrationStatusService.getExternalStatusByIds(registrationIds);
		assertEquals("AWAITING_INFORMATION", list.get(0).getStatusCode());
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
	
	@Test(expected = TablenotAccessibleException.class)
	public void getExternalStatusByIdsFailureTest() {
		String registartionId="1001";
		List<String> registrationIds = new ArrayList<>();
		registrationIds.add(registartionId);

		DataAccessLayerException exp = new DataAccessLayerException(HibernateErrorCode.ERR_DATABASE.getErrorCode(),
				"errorMessage", new Exception());
		Mockito.when(registrationStatusDao.getByIds(any())).thenThrow(exp);

		registrationStatusService.getExternalStatusByIds(registrationIds);

	}

	@Test
	public void testGetUnProcessedPacketsCount() {
		List<String> statusList = new ArrayList<>();
		statusList.add("SUCCESS");
		statusList.add("REPROCESS");
		List<String> excludeStageNames = new ArrayList<>();
		excludeStageNames.add("PacketReceiverStage");
		Mockito.when(registrationStatusDao.getUnProcessedPacketsCount(anyLong(), anyInt(), anyList(), anyList())).thenReturn(1);
		int packetCount = registrationStatusService.getUnProcessedPacketsCount(21600, 3, statusList, excludeStageNames);
		assertEquals(1, packetCount);
	}

	@Test
	public void testGetUnProcessedPackets() {

		List<String> statusList = new ArrayList<>();
		statusList.add("SUCCESS");
		statusList.add("REPROCESS");
		List<String> excludeStageNames = new ArrayList<>();
		excludeStageNames.add("PacketReceiverStage");
		Mockito.when(registrationStatusDao.getUnProcessedPackets(anyInt(), anyLong(), anyInt(), anyList(), anyList()))
				.thenReturn(entities);
		List<InternalRegistrationStatusDto> dtolist = registrationStatusService.getUnProcessedPackets(1, 21600, 3,
				statusList, excludeStageNames);
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
		List<String> excludeStageNames = new ArrayList<>();
		excludeStageNames.add("PacketReceiverStage");
		DataAccessLayerException exp = new DataAccessLayerException(HibernateErrorCode.ERR_DATABASE.getErrorCode(),
				"errorMessage", new Exception());
		Mockito.when(registrationStatusDao.getUnProcessedPacketsCount(anyLong(), anyInt(), anyList(), anyList())).thenThrow(exp);

		registrationStatusService.getUnProcessedPacketsCount(21600, 3, statusList, excludeStageNames);
	}

	@Test(expected = TablenotAccessibleException.class)
	public void getUnProcessedPacketsFailureTest() {
		List<String> statusList = new ArrayList<>();
		statusList.add("SUCCESS");
		List<String> excludeStageNames = new ArrayList<>();
		excludeStageNames.add("PacketReceiverStage");
		DataAccessLayerException exp = new DataAccessLayerException(HibernateErrorCode.ERR_DATABASE.getErrorCode(),
				"errorMessage", new Exception());
		Mockito.when(registrationStatusDao.getUnProcessedPackets(anyInt(), anyLong(), anyInt(), anyList(), anyList()))
				.thenThrow(exp);

		registrationStatusService.getUnProcessedPackets(1, 21600, 3, statusList, excludeStageNames);
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
	
	@Test
	public void getRegStatusForMainProcessSuccessTest() {

		Mockito.when(registrationStatusDao.findAll(any())).thenReturn(entities);
		String registartionId="1000";
		InternalRegistrationStatusDto dto = registrationStatusService.getRegStatusForMainProcess(registartionId);
		assertEquals("1000", dto.getRegistrationId());
	}
	
	@Test(expected = TablenotAccessibleException.class)
	public void getRegStatusForMainProcessFailureTest() {
		DataAccessLayerException exp = new DataAccessLayerException(HibernateErrorCode.ERR_DATABASE.getErrorCode(),
				"errorMessage", new Exception());
		Mockito.when(registrationStatusDao.findAll(anyString())).thenThrow(exp);
		registrationStatusService.getRegStatusForMainProcess("1001");
	}
}