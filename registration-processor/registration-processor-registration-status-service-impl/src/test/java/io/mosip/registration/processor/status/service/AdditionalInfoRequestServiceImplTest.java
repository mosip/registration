package io.mosip.registration.processor.status.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import io.mosip.registration.processor.core.packet.dto.AdditionalInfoRequestDto;
import io.mosip.registration.processor.status.entity.AdditionalInfoRequestEntity;
import io.mosip.registration.processor.status.entity.AdditionalInfoRequestPKEntity;
import io.mosip.registration.processor.status.repositary.BaseRegProcRepository;
import io.mosip.registration.processor.status.service.impl.AdditionalInfoRequestServiceImpl;
@RunWith(MockitoJUnitRunner.class)
public class AdditionalInfoRequestServiceImplTest {
	@Mock
    private BaseRegProcRepository<AdditionalInfoRequestEntity, String> additionalInfoRequestRepository;
	@InjectMocks
	private AdditionalInfoRequestService AdditionalInfoRequestServiceImpl=new AdditionalInfoRequestServiceImpl();
	private AdditionalInfoRequestDto additionalInfoRequestDto;
	private AdditionalInfoRequestEntity entity;
	@Before
	public void setup() {
		LocalDateTime timestamp=LocalDateTime.now();
		additionalInfoRequestDto=new AdditionalInfoRequestDto("1234", "12345", "1234567890", "CORRCETION", 1, timestamp);
		entity=new AdditionalInfoRequestEntity();
		entity.setAdditionalInfoIteration(1);
		entity.setAdditionalInfoProcess("CORRCETION");
		entity.setRegId("1234567890");
		entity.setTimestamp(timestamp);
		AdditionalInfoRequestPKEntity pk=new AdditionalInfoRequestPKEntity();
		pk.setAdditionalInfoReqId("1234");
		pk.setWorkflowInstanceId("12345");
		entity.setId(pk);
		Mockito.when(additionalInfoRequestRepository.getAdditionalInfoRequestByReqId(Mockito.anyString())).thenReturn(List.of(entity));
		Mockito.when(additionalInfoRequestRepository.getAdditionalInfoRequestByRegIdAndProcessAndIteration(Mockito.anyString(),Mockito.anyString(),Mockito.anyInt())).thenReturn(List.of(entity));
		Mockito.when(additionalInfoRequestRepository.getAdditionalInfoByRegId(Mockito.anyString())).thenReturn(List.of(entity));
		Mockito.when(additionalInfoRequestRepository.getAdditionalInfoRequestByRegIdAndProcess(Mockito.anyString(),Mockito.anyString())).thenReturn(List.of(entity));
		Mockito.when(additionalInfoRequestRepository.save(Mockito.any(AdditionalInfoRequestEntity.class))).thenReturn(entity);
		
	}
	
	@Test
	public void addAdditionalInfoRequestTest() {

		AdditionalInfoRequestServiceImpl.addAdditionalInfoRequest(additionalInfoRequestDto);
	}

	@Test
	public void getAdditionalInfoRequestByRegIdAndProcessTest() {

		assertEquals("1234",AdditionalInfoRequestServiceImpl.getAdditionalInfoRequestByRegIdAndProcess("1234567890", "CORRCETION").get(0).getAdditionalInfoReqId());
	}
	@Test
	public void getAdditionalInfoByRidTest() {

		assertEquals("1234",AdditionalInfoRequestServiceImpl.getAdditionalInfoByRid("1234567890").get(0).getAdditionalInfoReqId());
		
	}
	@Test
	public void getAdditionalInfoRequestByReqIdTest() {
		assertEquals("1234",AdditionalInfoRequestServiceImpl.getAdditionalInfoRequestByReqId("1234").getAdditionalInfoReqId());
		
	}
	
	@Test
	public void getAdditionalInfoRequestByRegIdAndProcessAndIterationTest() {
		assertEquals("1234",AdditionalInfoRequestServiceImpl.getAdditionalInfoRequestByRegIdAndProcessAndIteration("1234567890", "CORRCETION",1).getAdditionalInfoReqId());
		
	}
	
	@Test
	public void getAdditionalInfoRequestByReqIdFailureTest() {
		Mockito.when(additionalInfoRequestRepository.getAdditionalInfoRequestByReqId(Mockito.anyString())).thenReturn(null);
		assertNull(AdditionalInfoRequestServiceImpl.getAdditionalInfoRequestByReqId("1234"));
		
	}
	
	@Test
	public void getAdditionalInfoRequestByRegIdAndProcessAndIterationFailureTest() {
		Mockito.when(additionalInfoRequestRepository.getAdditionalInfoRequestByRegIdAndProcessAndIteration(Mockito.anyString(),Mockito.anyString(),Mockito.anyInt())).thenReturn(null);
		
		assertNull("1234",AdditionalInfoRequestServiceImpl.getAdditionalInfoRequestByRegIdAndProcessAndIteration("1234567890", "CORRCETION",1));
		
	}
	
}
