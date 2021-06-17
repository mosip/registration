package io.mosip.registration.processor.status.dao;

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
import org.mockito.runners.MockitoJUnitRunner;

import io.mosip.registration.processor.core.workflow.dto.SortInfo;
import io.mosip.registration.processor.status.dto.FilterInfo;
import io.mosip.registration.processor.status.dto.SyncStatusDto;
import io.mosip.registration.processor.status.dto.SyncTypeDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.repositary.RegistrationRepositary;

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
	RegistrationRepositary<SyncRegistrationEntity, String> syncRegistrationRepository;

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
		syncRegistrationEntity.setId("0c326dc2-ac54-4c2a-98b4-b0c620f1661f");
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

	}

	/**
	 * Save success test.
	 */
	@Test
	public void saveTest() {
		SyncRegistrationEntity syncRegistrationEntityResult = syncRegistrationDao.save(syncRegistrationEntity);
		assertEquals("Verifing Registration Id after saving in DB. Expected value is 1001",
				syncRegistrationEntity.getId(), syncRegistrationEntityResult.getId());
	}

	@Test
	public void updateTest() {
		SyncRegistrationEntity syncRegistrationEntityResult = syncRegistrationDao.update(syncRegistrationEntity);
		assertEquals("Verifing Registration Id after Updating in DB. Expected value is 1001",
				syncRegistrationEntity.getId(), syncRegistrationEntityResult.getId());
	}

	/**
	 * Find by id success test.
	 */
	@Test
	public void findByIdSuccessTest() {
		SyncRegistrationEntity syncRegistrationEntityResult = syncRegistrationDao.findById("1001");
		assertEquals("Check id Registration Id is present in DB, expected valie is 1001",
				syncRegistrationEntity.getRegistrationId(), syncRegistrationEntityResult.getRegistrationId());
	}

	@Test
	public void findByIdFailureTest() {
		syncRegistrationEntityList = new ArrayList<>();
		Mockito.when(syncRegistrationRepository.createQuerySelect(any(), any())).thenReturn(syncRegistrationEntityList);
		SyncRegistrationEntity syncRegistrationEntityResult = syncRegistrationDao.findById("1001");
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

}
