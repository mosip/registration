package io.mosip.registration.processor.cmdvalidator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
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

	@Mock
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;

	@Test
	public void validateSuccessTest() throws IOException, BaseCheckedException {
		ResponseWrapper<MachineHistoryResponseDto> responseWrapper = new ResponseWrapper<MachineHistoryResponseDto>();
		MachineHistoryResponseDto machineHistoryResponseDto = new MachineHistoryResponseDto();
		MachineHistoryDto machineHistoryDto = new MachineHistoryDto();
		machineHistoryDto.setId("1001");
		machineHistoryDto.setIsActive(true);
		machineHistoryResponseDto.setMachineHistoryDetails(Arrays.asList(machineHistoryDto));
		responseWrapper.setResponse(machineHistoryResponseDto);
		responseWrapper.setErrors(null);

		Mockito.when(registrationProcessorRestService.getApi(any(), any(), anyString(), anyString(),
				eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		machineValidator.validate("1001", "eng", "2021-06-04T07:31:59.831Z", "45128164920495");
	}
	
	@Test(expected = ValidationFailedException.class)
	public void validateMachineInActiveFailureTest() throws IOException, BaseCheckedException {
		ResponseWrapper<MachineHistoryResponseDto> responseWrapper = new ResponseWrapper<MachineHistoryResponseDto>();
		MachineHistoryResponseDto machineHistoryResponseDto = new MachineHistoryResponseDto();
		MachineHistoryDto machineHistoryDto = new MachineHistoryDto();
		machineHistoryDto.setId("1001");
		machineHistoryDto.setIsActive(false);
		machineHistoryResponseDto.setMachineHistoryDetails(Arrays.asList(machineHistoryDto));
		responseWrapper.setResponse(machineHistoryResponseDto);
		responseWrapper.setErrors(null);

		Mockito.when(registrationProcessorRestService.getApi(any(), any(), anyString(), anyString(),
				eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		machineValidator.validate("1001", "eng", "2021-06-04T07:31:59.831Z", "45128164920495");
	}
	
	@Test(expected = BaseCheckedException.class)
	public void validateMachineNotFoundFailureTest() throws IOException, BaseCheckedException {
		ResponseWrapper<MachineHistoryResponseDto> responseWrapper = new ResponseWrapper<MachineHistoryResponseDto>();
		ErrorDTO error = new ErrorDTO();
		error.setErrorCode("ERR-001");
		error.setMessage("machine not found");
		responseWrapper.setResponse(null);
		responseWrapper.setErrors(Arrays.asList(error));

		Mockito.when(registrationProcessorRestService.getApi(any(), any(), anyString(), anyString(),
				eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		machineValidator.validate("1001", "eng", "2021-06-04T07:31:59.831Z", "45128164920495");
	}

}
