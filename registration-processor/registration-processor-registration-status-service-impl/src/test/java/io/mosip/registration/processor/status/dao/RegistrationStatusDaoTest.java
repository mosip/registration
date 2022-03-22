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
		RegistrationStatusEntity rEntity = registrationStatusDao.findById("1000.zip");
		assertEquals(registrationStatusEntity, rEntity);

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
		List<String> excludeStageNames = new ArrayList<>();
		excludeStageNames.add("PacketReceiverStage");
		Mockito.when(registrationStatusRepositary.createQuerySelect(Matchers.anyString(), Matchers.anyMap(),
				Matchers.anyInt())).thenReturn(list);
		List<RegistrationStatusEntity> rEntityList = registrationStatusDao.getUnProcessedPackets(2, 60000, 4,
				statusList, excludeStageNames);
		assertEquals(list, rEntityList);
	}

	@Test
	public void testgetUnProcessedPacketCount() {
		List<String> statusList = new ArrayList<>();
		statusList.add("SUCCESS");
		List<String> excludeStageNames = new ArrayList<>();
		excludeStageNames.add("PacketReceiverStage");
		int entityCount = list.size();
		Mockito.when(registrationStatusRepositary.createQuerySelect(Matchers.anyString(), Matchers.anyMap()))
				.thenReturn(list);
		int count = registrationStatusDao.getUnProcessedPacketsCount(6000, 4, statusList, excludeStageNames);
		assertEquals(count, entityCount);
	}

	@Test
	public void getByIdsAndTimestamp() {
		List<String> idList = new ArrayList<>();
		idList.add("1000");
		List<RegistrationStatusEntity> rEntityList = registrationStatusDao.getByIdsAndTimestamp(idList);
		assertEquals(list, rEntityList);
	}
}
