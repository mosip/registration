package io.mosip.registration.processor.status.dao;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;

import io.mosip.registration.processor.core.workflow.dto.FilterInfo;
import io.mosip.registration.processor.core.workflow.dto.PaginationInfo;
import io.mosip.registration.processor.core.workflow.dto.SortInfo;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.entity.RegistrationStatusEntity;
import io.mosip.registration.processor.status.repositary.RegistrationRepositary;

@SuppressWarnings("deprecation")
@RunWith(MockitoJUnitRunner.class)
public class RegistrationStatusDaoTest {

	private RegistrationStatusEntity registrationStatusEntity;
	private List<RegistrationStatusEntity> list;

	@InjectMocks
	RegistrationStatusDao registrationStatusDao = new RegistrationStatusDao();
	@Mock
	RegistrationRepositary<RegistrationStatusEntity, String> registrationStatusRepositary;

	@Before
	public void setup() {
		registrationStatusEntity = new RegistrationStatusEntity();
		registrationStatusEntity.setIsActive(true);

		list = new ArrayList<>();
		list.add(registrationStatusEntity);
		Mockito.when(registrationStatusRepositary.createQuerySelect(Matchers.any(), Matchers.any())).thenReturn(list);
		Mockito.when(registrationStatusRepositary.save(Matchers.any())).thenReturn(registrationStatusEntity);
	}

	@Test
	public void getEnrolmentStatusByStatusCodeTest() {
		List<RegistrationStatusEntity> rEntityList = registrationStatusDao
				.getEnrolmentStatusByStatusCode(RegistrationStatusCode.PROCESSING.toString());
		assertEquals(list, rEntityList);
	}

	@Test
	public void getByIds() {
		List<String> idList = new ArrayList<>();
		idList.add("1000.zip");
		List<RegistrationStatusEntity> rEntityList = registrationStatusDao.getByIds(idList);
		assertEquals(list, rEntityList);
	}

	@Test
	public void findByIdTest() {
		RegistrationStatusEntity rEntity = registrationStatusDao.find("1000.zip", "NEW", 1, "");
		assertEquals(registrationStatusEntity, rEntity);
	}
	
	@Test
	public void findByIdworkFlowNullTest() {
		RegistrationStatusEntity rEntity = registrationStatusDao.find("1000.zip", "NEW", 1, null);
		assertEquals(registrationStatusEntity, rEntity);
	}
	
	@Test
	public void getInReprocessPacketsCountTest() {
		Mockito.when(registrationStatusRepositary.countByStatusCode(Matchers.any(), Matchers.any(), Matchers.any()))
				.thenReturn((long) 2);
		long response = registrationStatusDao.getInReprocessPacketsCount(20);
		assertEquals(2, response);
	}
	
	@Test
	public void findAllTest() {
		List<RegistrationStatusEntity> rEntity = registrationStatusDao.findAll("1000.zip");
		assertEquals(list, rEntity);

	}

	@Test
	public void save() {
		RegistrationStatusEntity rEntity = registrationStatusDao.save(registrationStatusEntity);
		assertEquals(registrationStatusEntity, rEntity);

	}

	@Test
	public void update() {
		RegistrationStatusEntity rEntity = registrationStatusDao.update(registrationStatusEntity);
		assertEquals(registrationStatusEntity, rEntity);

	}

	@Test
	public void testgetUnProcessedPackets() {
		List<String> statusList = new ArrayList<>();
		statusList.add("SUCCESS");
		Mockito.when(registrationStatusRepositary.createQuerySelect(Matchers.anyString(), Matchers.anyMap(),
				Matchers.anyInt())).thenReturn(list);
		List<RegistrationStatusEntity> rEntityList = registrationStatusDao.getUnProcessedPackets(2, 60000, 4,
				statusList);
		assertEquals(list, rEntityList);
	}

	@Test
	public void testgetUnProcessedPacketCount() {
		List<String> statusList = new ArrayList<>();
		statusList.add("SUCCESS");
		int entityCount = list.size();
		Mockito.when(registrationStatusRepositary.createQuerySelect(Matchers.anyString(), Matchers.anyMap()))
				.thenReturn(list);
		int count = registrationStatusDao.getUnProcessedPacketsCount(6000, 4, statusList);
		assertEquals(count, entityCount);
	}
	
	@Test
	public void testgetPausedPackets() {
		List<String> statusList = new ArrayList<>();
		statusList.add("SUCCESS");
		Mockito.when(registrationStatusRepositary.createQuerySelect(Matchers.anyString(), Matchers.anyMap(),
				Matchers.anyInt())).thenReturn(list);
		List<RegistrationStatusEntity> rEntityList = registrationStatusDao.getActionablePausedPackets(2);
		assertEquals(list, rEntityList);
	}

	@Test
	public void getByIdsAndTimestamp() {
		List<String> idList = new ArrayList<>();
		idList.add("1000");
		List<RegistrationStatusEntity> rEntityList = registrationStatusDao.getByIdsAndTimestamp(idList);
		assertEquals(list, rEntityList);
	}

	@Test
	public void testResumablePackets() {
		List<String> statusList = new ArrayList<>();
		statusList.add("SUCCESS");
		Mockito.when(registrationStatusRepositary.createQuerySelect(Matchers.anyString(), Matchers.anyMap(),
				Matchers.anyInt())).thenReturn(list);
		List<RegistrationStatusEntity> rEntityList = registrationStatusDao.getResumablePackets(2);
		assertEquals(list, rEntityList);
	}
	
	@Test
	public void getPagedSearchResultsTest() {
		List<FilterInfo> filters = new ArrayList<FilterInfo>();
		FilterInfo filterInfo = new FilterInfo("rid", "1000");
		filters.add(filterInfo);
		SortInfo sort = new SortInfo("rid", "desc");
		PaginationInfo pagination = new PaginationInfo(1, 5);
		Mockito.when(registrationStatusRepositary.createQuerySelect(Matchers.anyString(), Matchers.anyMap(),
				Matchers.anyInt())).thenReturn(list);
		Page<RegistrationStatusEntity> rEntityList = registrationStatusDao.getPagedSearchResults(filters,sort,pagination);
		assertEquals(registrationStatusEntity, rEntityList.getContent().get(0));
	}
	
	@Test
	public void checkUinAvailabilityForRidTest() {
		Mockito.when(registrationStatusRepositary.createQuerySelect(Matchers.anyString(), Matchers.anyMap())).thenReturn(list);
		Boolean status = registrationStatusDao.checkUinAvailabilityForRid("1000");
		assertEquals(true, status);
	}
}
