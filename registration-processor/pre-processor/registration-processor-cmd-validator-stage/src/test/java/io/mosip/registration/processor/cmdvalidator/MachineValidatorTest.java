package io.mosip.registration.processor.cmdvalidator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.ValidationFailedException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.MachineHistoryDto;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.MachineHistoryResponseDto;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.stages.cmdvalidator.MachineValidator;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest
public class MachineValidatorTest {
	@InjectMocks
	private MachineValidator machineValidator;

	@Spy
	private ObjectMapper mapper = new ObjectMapper();

	@Mock
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;
	
	@Test(expected=BaseCheckedException.class)
	public void NullMachineIdTest() throws ApisResourceAccessException, IOException, BaseCheckedException {
		machineValidator.validate(null,"eng", "2022-01-25T03:25:37.018Z", "10001");
	}
	
	@Test
	public void validateTest() throws IOException, BaseCheckedException {
		MachineHistoryResponseDto machineHistoryResponseDto=new MachineHistoryResponseDto();
		MachineHistoryDto machineHistoryDto=new MachineHistoryDto();
		machineHistoryDto.setId("1001");
		machineHistoryDto.setIsActive(true);
		machineHistoryResponseDto.setMachineHistoryDetails(List.of(machineHistoryDto));
		ResponseWrapper<MachineHistoryResponseDto> responseWrapper=new ResponseWrapper<MachineHistoryResponseDto>();
		responseWrapper.setResponse(machineHistoryResponseDto);
		responseWrapper.setErrors(null);
		Mockito.when(registrationProcessorRestService.getApi(any(), any(), anyString(), any(), any())).thenReturn(responseWrapper);
		machineValidator.validate("1001","eng", "2022-01-25T03:25:37.018Z", "10001");
	}
	
	@Test(expected=ValidationFailedException.class)
	public void ValidationFailedExceptionTest() throws IOException, BaseCheckedException {
		MachineHistoryResponseDto machineHistoryResponseDto=new MachineHistoryResponseDto();
		MachineHistoryDto machineHistoryDto=new MachineHistoryDto();
		machineHistoryDto.setId("1001");
		machineHistoryDto.setIsActive(false);
		machineHistoryResponseDto.setMachineHistoryDetails(List.of(machineHistoryDto));
		ResponseWrapper<MachineHistoryResponseDto> responseWrapper=new ResponseWrapper<MachineHistoryResponseDto>();
		responseWrapper.setResponse(machineHistoryResponseDto);
		responseWrapper.setErrors(null);
		Mockito.when(registrationProcessorRestService.getApi(any(), any(), anyString(), any(), any())).thenReturn(responseWrapper);
		machineValidator.validate("1001","eng", "2022-01-25T03:25:37.018Z", "10001");
	}
	
	@Test(expected=BaseCheckedException.class)
	public void machineNotfoundTest() throws IOException, BaseCheckedException {
		MachineHistoryResponseDto machineHistoryResponseDto=new MachineHistoryResponseDto();
		MachineHistoryDto machineHistoryDto=new MachineHistoryDto();
		machineHistoryResponseDto.setMachineHistoryDetails(List.of(machineHistoryDto));
		ResponseWrapper<MachineHistoryResponseDto> responseWrapper=new ResponseWrapper<MachineHistoryResponseDto>();
		responseWrapper.setResponse(machineHistoryResponseDto);
		responseWrapper.setErrors(null);
		Mockito.when(registrationProcessorRestService.getApi(any(), any(), anyString(), any(), any())).thenReturn(responseWrapper);
		machineValidator.validate("1001","eng", "2022-01-25T03:25:37.018Z", "10001");
	}
	
	@Test(expected=BaseCheckedException.class)
	public void FailureResponseTest() throws IOException, BaseCheckedException {
		
		ResponseWrapper<MachineHistoryResponseDto> responseWrapper=new ResponseWrapper<MachineHistoryResponseDto>();
		responseWrapper.setResponse(null);
		ErrorDTO errorDTO=new ErrorDTO("", "");
		responseWrapper.setErrors(List.of(errorDTO));
		Mockito.when(registrationProcessorRestService.getApi(any(), any(), anyString(), any(), any())).thenReturn(responseWrapper);
		machineValidator.validate("1001","eng", "2022-01-25T03:25:37.018Z", "10001");
	}
	
}
