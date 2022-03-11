package io.mosip.registration.processor.cmdvalidator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.ValidationFailedException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.packet.dto.RegOsiDto;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.RegistartionCenterTimestampResponseDto;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.RegistrationCenterDto;
import io.mosip.registration.processor.core.packet.dto.regcentermachine.RegistrationCenterResponseDto;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.stages.cmdvalidator.CenterValidator;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest
public class CenterValidatorTest {
	@InjectMocks
	private CenterValidator centerValidator;

	@Spy
	private ObjectMapper mapper = new ObjectMapper();

	@Mock
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;
	 RegOsiDto regOsi=new RegOsiDto();
	@Before
	public void setup() throws ApisResourceAccessException {
		ReflectionTestUtils.setField(centerValidator, "isWorkingHourValidationRequired", true);
		
	}
	@Test
	public void validateTest() throws IOException, BaseCheckedException {
		regOsi.setRegcntrId("1001");
		regOsi.setRegId("10001");
		regOsi.setPacketCreationDate("2022-01-24T05:04:47.692Z");
		ResponseWrapper<RegistrationCenterResponseDto> responseWrapper=new ResponseWrapper<RegistrationCenterResponseDto>();
		RegistrationCenterDto registrationCenterDto=new RegistrationCenterDto();
		registrationCenterDto.setIsActive(true);
		registrationCenterDto.setId("1001");
		RegistrationCenterResponseDto registrationCenterResponseDto=new RegistrationCenterResponseDto();
		registrationCenterResponseDto.setRegistrationCentersHistory(List.of(registrationCenterDto));
		responseWrapper.setResponse(registrationCenterResponseDto);
		responseWrapper.setErrors(null);
		ResponseWrapper<RegistartionCenterTimestampResponseDto> responseWrapper1=new ResponseWrapper<RegistartionCenterTimestampResponseDto>();
		RegistartionCenterTimestampResponseDto registartionCenterTimestampResponseDto=new RegistartionCenterTimestampResponseDto();
		registartionCenterTimestampResponseDto.setStatus("Valid");
		responseWrapper1.setResponse(registartionCenterTimestampResponseDto);
		responseWrapper1.setErrors(null);
		Mockito.when(registrationProcessorRestService.getApi(any(), any(), anyString(), any(), any())).thenReturn(responseWrapper).thenReturn(responseWrapper1);
		centerValidator.validate("eng", regOsi, "10001");
	}
	@Test(expected=BaseCheckedException.class)
	public void validateCenterIdNotfoundTest() throws IOException, BaseCheckedException {
		regOsi.setRegId("10001");
		centerValidator.validate("eng", regOsi, "10001");
	}
	@Test(expected=ValidationFailedException.class)
	public void validationFailedExceptionTest() throws IOException, BaseCheckedException {
		regOsi.setRegcntrId("1001");
		regOsi.setRegId("10001");
		regOsi.setPacketCreationDate("2022-01-24T05:04:47.692Z");
		ResponseWrapper<RegistrationCenterResponseDto> responseWrapper=new ResponseWrapper<RegistrationCenterResponseDto>();
		RegistrationCenterDto registrationCenterDto=new RegistrationCenterDto();
		registrationCenterDto.setId("1001");
		registrationCenterDto.setIsActive(false);
		RegistrationCenterResponseDto registrationCenterResponseDto=new RegistrationCenterResponseDto();
		registrationCenterResponseDto.setRegistrationCentersHistory(List.of(registrationCenterDto));
		responseWrapper.setResponse(registrationCenterResponseDto);
		responseWrapper.setErrors(null);
		Mockito.when(registrationProcessorRestService.getApi(any(), any(), anyString(), any(), any())).thenReturn(responseWrapper);
		
		centerValidator.validate("eng", regOsi, "10001");
	}
	@Test(expected=BaseCheckedException.class)
	public void ErrorResponseTest() throws IOException, BaseCheckedException {
		regOsi.setRegcntrId("1001");
		regOsi.setRegId("10001");
		regOsi.setPacketCreationDate("2022-01-24T05:04:47.692Z");
		ResponseWrapper<RegistrationCenterResponseDto> responseWrapper=new ResponseWrapper<RegistrationCenterResponseDto>();
		ErrorDTO errorDTO=new ErrorDTO("", "");
		responseWrapper.setErrors(List.of(errorDTO));
		Mockito.when(registrationProcessorRestService.getApi(any(), any(), anyString(), any(), any())).thenReturn(responseWrapper);
		
		centerValidator.validate("eng", regOsi, "10001");
	}
	@Test(expected=ValidationFailedException.class)
	public void CenterIdAndTimestampValidationFailedExceptionTest() throws IOException, BaseCheckedException {
		regOsi.setRegcntrId("1001");
		regOsi.setRegId("10001");
		regOsi.setPacketCreationDate("2022-01-24T05:04:47.692Z");
		ResponseWrapper<RegistrationCenterResponseDto> responseWrapper=new ResponseWrapper<RegistrationCenterResponseDto>();
		RegistrationCenterDto registrationCenterDto=new RegistrationCenterDto();
		registrationCenterDto.setIsActive(true);
		registrationCenterDto.setId("1001");
		RegistrationCenterResponseDto registrationCenterResponseDto=new RegistrationCenterResponseDto();
		registrationCenterResponseDto.setRegistrationCentersHistory(List.of(registrationCenterDto));
		responseWrapper.setResponse(registrationCenterResponseDto);
		responseWrapper.setErrors(null);
		ResponseWrapper<RegistartionCenterTimestampResponseDto> responseWrapper1=new ResponseWrapper<RegistartionCenterTimestampResponseDto>();
		RegistartionCenterTimestampResponseDto registartionCenterTimestampResponseDto=new RegistartionCenterTimestampResponseDto();
		registartionCenterTimestampResponseDto.setStatus("inValid");
		responseWrapper1.setResponse(registartionCenterTimestampResponseDto);
		responseWrapper1.setErrors(null);
		Mockito.when(registrationProcessorRestService.getApi(any(), any(), anyString(), any(), any())).thenReturn(responseWrapper).thenReturn(responseWrapper1);
		centerValidator.validate("eng", regOsi, "10001");
	}
	@Test(expected=BaseCheckedException.class)
	public void CenterIdAndTimestampErrorResponseTest() throws IOException, BaseCheckedException {
		regOsi.setRegcntrId("1001");
		regOsi.setRegId("10001");
		regOsi.setPacketCreationDate("2022-01-24T05:04:47.692Z");
		ResponseWrapper<RegistrationCenterResponseDto> responseWrapper=new ResponseWrapper<RegistrationCenterResponseDto>();
		RegistrationCenterDto registrationCenterDto=new RegistrationCenterDto();
		registrationCenterDto.setIsActive(true);
		registrationCenterDto.setId("1001");
		RegistrationCenterResponseDto registrationCenterResponseDto=new RegistrationCenterResponseDto();
		registrationCenterResponseDto.setRegistrationCentersHistory(List.of(registrationCenterDto));
		responseWrapper.setResponse(registrationCenterResponseDto);
		responseWrapper.setErrors(null);
		ResponseWrapper<RegistartionCenterTimestampResponseDto> responseWrapper1=new ResponseWrapper<RegistartionCenterTimestampResponseDto>();
		ErrorDTO errorDTO=new ErrorDTO("", "");
		responseWrapper1.setErrors(List.of(errorDTO));
		Mockito.when(registrationProcessorRestService.getApi(any(), any(), anyString(), any(), any())).thenReturn(responseWrapper).thenReturn(responseWrapper1);
		
		centerValidator.validate("eng", regOsi, "10001");
	}
	
}
