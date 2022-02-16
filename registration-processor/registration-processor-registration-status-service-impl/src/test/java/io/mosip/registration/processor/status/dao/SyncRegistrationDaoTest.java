package io.mosip.registration.processor.status.dao;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import io.mosip.registration.processor.core.workflow.dto.SortInfo;
import io.mosip.registration.processor.status.dto.FilterInfo;
import io.mosip.registration.processor.status.dto.SyncStatusDto;
import io.mosip.registration.processor.status.dto.SyncTypeDto;
import io.mosip.registration.processor.status.entity.SaltEntity;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.repositary.SaltRepository;
import io.mosip.registration.processor.status.repositary.SyncRegistrationRepository;

/**
 * The Class SyncRegistrationDaoTest.
 * 
 * @author M1047487
 */
@RunWith(MockitoJUnitRunner.class)
public class SyncRegistrationDaoTest {

	/** The sync registration dao. */
	@InjectMocks
	SyncRegistrationDao syncRegistrationDao;

	/** The sync registration repository. */
	@Mock
	SyncRegistrationRepository<SyncRegistrationEntity, String> syncRegistrationRepository;

	@Mock
	SaltRepository saltRepository;

	/** The sync registration entity. */
	private SyncRegistrationEntity syncRegistrationEntity;

	private List<SyncRegistrationEntity> syncRegistrationEntityList;

	/**
	 * Sets the up.
	 */
	@Before
	public void setUp() {

		syncRegistrationEntityList = new ArrayList<>();

		syncRegistrationEntity = new SyncRegistrationEntity();
		syncRegistrationEntity.setWorkflowInstanceId("0c326dc2-ac54-4c2a-98b4-b0c620f1661f");
		syncRegistrationEntity.setRegistrationId("1001");
		syncRegistrationEntity.setRegistrationType(SyncTypeDto.NEW.getValue());

		syncRegistrationEntity.setStatusCode(SyncStatusDto.PRE_SYNC.toString());
		syncRegistrationEntity.setStatusComment("NEW");
		syncRegistrationEntity.setLangCode("eng");
		syncRegistrationEntity.setIsDeleted(false);
		syncRegistrationEntity.setCreateDateTime(LocalDateTime.now());
		syncRegistrationEntity.setUpdateDateTime(LocalDateTime.now());
		syncRegistrationEntity.setCreatedBy("MOSIP");
		syncRegistrationEntity.setUpdatedBy("MOSIP");
		syncRegistrationEntity.setEmail("mosip@gmail.com");
		syncRegistrationEntity.setName("mosip");
		syncRegistrationEntity.setPhone("9188556611");

		syncRegistrationEntityList.add(syncRegistrationEntity);

		Mockito.when(syncRegistrationRepository.save(any())).thenReturn(syncRegistrationEntity);

		Mockito.when(syncRegistrationRepository.createQuerySelect(any(), any())).thenReturn(syncRegistrationEntityList);
		Mockito.when(syncRegistrationRepository.findByAdditionalInfoReqId( any())).thenReturn(syncRegistrationEntityList);
		Mockito.when(syncRegistrationRepository.findByPacketId( any())).thenReturn(syncRegistrationEntityList);
		Mockito.when(syncRegistrationRepository.findByPacketIds( any())).thenReturn(syncRegistrationEntityList);
		Mockito.when(syncRegistrationRepository.findByRegistrationId( any())).thenReturn(syncRegistrationEntityList);
		Mockito.when(syncRegistrationRepository.findByRegistrationIdIdANDAdditionalInfoReqId( anyString(),anyString())).thenReturn(syncRegistrationEntityList);
		Mockito.when(syncRegistrationRepository.findByRegistrationIdIdANDRegType( anyString(),anyString())).thenReturn(syncRegistrationEntityList);
		Mockito.when(syncRegistrationRepository.findByRegistrationIds( any())).thenReturn(syncRegistrationEntityList);
		Mockito.when(syncRegistrationRepository.findByworkflowInstanceId( any())).thenReturn(syncRegistrationEntityList);

	}

	/**
	 * Save success test.
	 */
	@Test
	public void saveTest() {
		SyncRegistrationEntity syncRegistrationEntityResult = syncRegistrationDao.save(syncRegistrationEntity);
		assertEquals("Verifing Registration Id after saving in DB. Expected value is 1001",
				syncRegistrationEntity.getWorkflowInstanceId(), syncRegistrationEntityResult.getWorkflowInstanceId());
	}

	@Test
	public void updateTest() {
		SyncRegistrationEntity syncRegistrationEntityResult = syncRegistrationDao.update(syncRegistrationEntity);
		assertEquals("Verifing Registration Id after Updating in DB. Expected value is 1001",
				syncRegistrationEntity.getWorkflowInstanceId(), syncRegistrationEntityResult.getWorkflowInstanceId());
	}

	/**
	 * Find by id success test.
	 */
	@Test
	public void findByIdSuccessTest() {
		SyncRegistrationEntity syncRegistrationEntityResult = syncRegistrationDao.findByPacketId("1001");
		assertEquals("Check id Registration Id is present in DB, expected valie is 1001",
				syncRegistrationEntity.getRegistrationId(), syncRegistrationEntityResult.getRegistrationId());
	}

	@Test
	public void findByIdFailureTest() {
		syncRegistrationEntityList = new ArrayList<>();
		Mockito.when(syncRegistrationRepository.findByPacketId( any())).thenReturn(syncRegistrationEntityList);
		SyncRegistrationEntity syncRegistrationEntityResult = syncRegistrationDao.findByPacketId("1001");
		assertEquals("Check id Registration Id is present in DB, expected value is empty List", null,
				syncRegistrationEntityResult);
	}

	@Test
	public void getByIds() {
		List<String> idList = new ArrayList<>();
		idList.add("1000.zip");
		List<SyncRegistrationEntity> rEntityList = syncRegistrationDao.getByIds(idList);
		assertEquals(syncRegistrationEntityList, rEntityList);
	}

	@Test
	public void getById() {
		List<SyncRegistrationEntity> rEntityList = syncRegistrationDao.findById("1000.zip");
		assertEquals(syncRegistrationEntityList, rEntityList);
	}

	@Test
	public void deleteAdditionalInfoTest() {
		Mockito.when(syncRegistrationRepository.update(any())).thenReturn(syncRegistrationEntity);
		boolean status = syncRegistrationDao.deleteAdditionalInfo(syncRegistrationEntity);
		assertEquals(true, status);
	}

	@Test
	public void findByWorkflowInstanceIdTest() {
		SyncRegistrationEntity rEntityList = syncRegistrationDao.findByWorkflowInstanceId("0c326dc2-ac54-4c2a-98b4-b0c620f1661f");
		assertEquals(syncRegistrationEntity, rEntityList);
	}

	@Test
	public void findByRegistrationIdIdAndRegTypeTest() {
		SyncRegistrationEntity rEntityList = syncRegistrationDao.findByRegistrationIdIdAndRegType("1000.zip", "NEW");
		assertEquals(syncRegistrationEntity, rEntityList);
	}

	@Test
	public void findByAdditionalInfoReqIdTest() {
		List<SyncRegistrationEntity> rEntityList = syncRegistrationDao.findByAdditionalInfoReqId("1000.zip");
		assertEquals(syncRegistrationEntityList, rEntityList);
	}

	@Test
	public void findByRegistrationIdIdAndAdditionalInfoReqIdTest() {
		
		SyncRegistrationEntity syncRegistrationEntityResult = syncRegistrationDao
				.findByRegistrationIdIdAndAdditionalInfoReqId("1000", "NEW");
		assertEquals("Check id Registration Id is present in DB, expected valie is 1001",
				syncRegistrationEntity.getRegistrationId(), syncRegistrationEntityResult.getRegistrationId());
	}

	@Test
	public void getByPacketIdsTest() {
		List<String> packetIdList = new ArrayList<>();
		packetIdList.add("test1");
		List<SyncRegistrationEntity> rEntityList = syncRegistrationDao.getByPacketIds(packetIdList);
		assertEquals(syncRegistrationEntityList, rEntityList);
	}
	
	@Test
	public void getSearchResults() {
		List<FilterInfo> filterInfos = new ArrayList<FilterInfo>();
		List<SortInfo> sortInfos = new ArrayList<SortInfo>();
		List<String> testIdList = new ArrayList<String>();
		FilterInfo filterInfo=new FilterInfo();
		filterInfo.setColumnName("name");
		filterInfo.setValue("mosip");
		filterInfo.setType("equals");
		SortInfo sortInfo=new SortInfo();
		sortInfo.setSortField("createDateTime");
		sortInfo.setSortType("desc");
		filterInfos.add(filterInfo);
		sortInfos.add(sortInfo);
		testIdList.add("1001");
		List<SyncRegistrationEntity> idList = syncRegistrationDao.getSearchResults(filterInfos, sortInfos);
		assertEquals(idList.get(0).getRegistrationId(), testIdList.get(0));
	}

	@Test
	public void getSaltValueTest() {
		SaltEntity saltEntity = new SaltEntity();
		saltEntity.setSalt("qwfs");
		Mockito.when(saltRepository.findSaltById(any())).thenReturn(saltEntity);
		String salt = syncRegistrationDao.getSaltValue((long) 10);
		assertEquals("qwfs", salt);
	}
	@Test
	public void getSearchResultsTest() {
		List<FilterInfo> filterInfos = new ArrayList<FilterInfo>();
		List<SortInfo> sortInfos = new ArrayList<SortInfo>();
		List<String> testIdList = new ArrayList<String>();
		FilterInfo filterInfo=new FilterInfo();
		filterInfo.setColumnName("packetSize");
		filterInfo.setFromValue("1000");
		filterInfo.setToValue("2000");
		filterInfo.setType("between");
		SortInfo sortInfo=new SortInfo();
		sortInfo.setSortField("createDateTime");
		sortInfo.setSortType("desc");
		filterInfos.add(filterInfo);
		sortInfos.add(sortInfo);
		testIdList.add("1001");
		List<SyncRegistrationEntity> idList = syncRegistrationDao.getSearchResults(filterInfos, sortInfos);
		assertEquals(idList.get(0).getRegistrationId(), testIdList.get(0));
	}
}
