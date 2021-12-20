package io.mosip.registration.processor.status.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import io.mosip.registration.processor.core.packet.dto.AdditionalInfoRequestDto;
import io.mosip.registration.processor.status.entity.AdditionalInfoRequestEntity;
import io.mosip.registration.processor.status.entity.AdditionalInfoRequestPKEntity;
import io.mosip.registration.processor.status.repositary.BaseRegProcRepository;
import io.mosip.registration.processor.status.service.impl.AdditionalInfoRequestServiceImpl;

@RunWith(SpringJUnit4ClassRunner.class)
public class AdditionalInfoRequestServiceImplTest {

	@InjectMocks
	AdditionalInfoRequestServiceImpl additionalInfoRequestServiceImpl = new AdditionalInfoRequestServiceImpl();

	@Mock
	private BaseRegProcRepository<AdditionalInfoRequestEntity, String> additionalInfoRequestRepository;

	List<AdditionalInfoRequestEntity> additionalInfoRequestEntityList = new ArrayList<AdditionalInfoRequestEntity>();
	AdditionalInfoRequestEntity additionalInfoRequestEntity = new AdditionalInfoRequestEntity();
	
	@Before
	public void setup() throws Exception {
		AdditionalInfoRequestPKEntity entity = new AdditionalInfoRequestPKEntity();
		entity.setAdditionalInfoReqId("10011100120000620210727102631-BIOMETRIC_CORRECTION-1");
		entity.setWorkflowInstanceId("c39653b6-f07d-4942-838f-dca9f2c1a4fc");
		additionalInfoRequestEntity.setId(entity);
		additionalInfoRequestEntity.setAdditionalInfoIteration(1);
		additionalInfoRequestEntity.setRegId("10011100120000620210727102631");
		additionalInfoRequestEntity.setTimestamp(LocalDateTime.now());
		additionalInfoRequestEntityList.add(additionalInfoRequestEntity);
	}

	@Test
	public void getAdditionalInfoRequestByReqIdSuccessTest() {

		Mockito.when(additionalInfoRequestRepository.getAdditionalInfoRequestByReqId(anyString()))
				.thenReturn(additionalInfoRequestEntityList);
		AdditionalInfoRequestDto result = additionalInfoRequestServiceImpl
				.getAdditionalInfoRequestByReqId("10011100120000620210727102631");
		assertEquals(result.getRegId(), "10011100120000620210727102631");
	}

	@Test
	public void getAdditionalInfoRequestByReqIdFailureTest() {

		Mockito.when(additionalInfoRequestRepository.getAdditionalInfoRequestByReqId(anyString())).thenReturn(null);
		AdditionalInfoRequestDto result = additionalInfoRequestServiceImpl
				.getAdditionalInfoRequestByReqId("10011100120000620210727102631");
		assertEquals(result, null);
	}

	@Test
	public void getAdditionalInfoRequestByRegIdAndProcessAndIterationSuccessTest() {

		Mockito.when(additionalInfoRequestRepository.getAdditionalInfoRequestByRegIdAndProcessAndIteration(anyString(),
				anyString(), anyInt())).thenReturn(additionalInfoRequestEntityList);
		AdditionalInfoRequestDto result = additionalInfoRequestServiceImpl
				.getAdditionalInfoRequestByRegIdAndProcessAndIteration("10011100120000620210727102631",
						"BIOMETRIC_CORRECTION", 1);
		assertEquals(result.getRegId(), "10011100120000620210727102631");
	}

	@Test
	public void getAdditionalInfoRequestByRegIdAndProcessAndIterationFailureTest() {

		Mockito.when(additionalInfoRequestRepository.getAdditionalInfoRequestByRegIdAndProcessAndIteration(anyString(),
				anyString(), anyInt())).thenReturn(null);
		AdditionalInfoRequestDto result = additionalInfoRequestServiceImpl
				.getAdditionalInfoRequestByRegIdAndProcessAndIteration("10011100120000620210727102631",
						"BIOMETRIC_CORRECTION", 1);
		assertEquals(result, null);
	}
	
	@Test
	public void getAdditionalInfoRequestByRegIdAndProcessSuccessTest() {

		Mockito.when(additionalInfoRequestRepository.getAdditionalInfoRequestByRegIdAndProcess(anyString(),
				anyString())).thenReturn(additionalInfoRequestEntityList);
		List<AdditionalInfoRequestDto> result = additionalInfoRequestServiceImpl
				.getAdditionalInfoRequestByRegIdAndProcess("10011100120000620210727102631",
						"BIOMETRIC_CORRECTION");
		assertEquals(result.size(), 1);
	}

	@Test
	public void getAdditionalInfoRequestByRegIdAndProcessFailureTest() {

		Mockito.when(additionalInfoRequestRepository.getAdditionalInfoRequestByRegIdAndProcess(anyString(),
				anyString())).thenReturn(null);
		List<AdditionalInfoRequestDto> result = additionalInfoRequestServiceImpl
				.getAdditionalInfoRequestByRegIdAndProcess("10011100120000620210727102631",
						"BIOMETRIC_CORRECTION");
		assertEquals(result.size(), 0);
	}
	
	@Test
	public void getAdditionalInfoByRidTest() {

		Mockito.when(additionalInfoRequestRepository.getAdditionalInfoByRegId(anyString()))
				.thenReturn(additionalInfoRequestEntityList);
		List<AdditionalInfoRequestDto> result = additionalInfoRequestServiceImpl
				.getAdditionalInfoByRid("10011100120000620210727102631");
		assertEquals(result.size(), 1);
	}
	
	@Test
	public void addAdditionalInfoRequestSuccessTest() {
		AdditionalInfoRequestDto additionalInfoRequestDto = new AdditionalInfoRequestDto();
		additionalInfoRequestDto.setAdditionalInfoIteration(1);
		additionalInfoRequestDto.setAdditionalInfoProcess("BIOMETRIC_CORRECTION");
		additionalInfoRequestDto.setAdditionalInfoReqId("10011100120000620210727102631-BIOMETRIC_CORRECTION-1");
		additionalInfoRequestDto.setRegId("10011100120000620210727102631");
		additionalInfoRequestDto.setTimestamp(LocalDateTime.now());
		additionalInfoRequestDto.setWorkflowInstanceId("c39653b6-f07d-4942-838f-dca9f2c1a4fc");
		
		Mockito.when(additionalInfoRequestRepository.save(any())).thenReturn(additionalInfoRequestEntity);
		additionalInfoRequestServiceImpl
				.addAdditionalInfoRequest(additionalInfoRequestDto);
	}

}