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
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
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

	@Mock
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;

	@Test
	public void validateWithoutTimestampSuccessTest() throws IOException, BaseCheckedException {
		ReflectionTestUtils.setField(centerValidator, "isWorkingHourValidationRequired", false);
		RegOsiDto regOsi = new RegOsiDto();
		regOsi.setRegId("45128164920495");
		regOsi.setRegcntrId("1001");
		regOsi.setPacketCreationDate("2021-06-04T07:31:59.831Z");

		ResponseWrapper<RegistrationCenterResponseDto> responseWrapper = new ResponseWrapper<RegistrationCenterResponseDto>();
		RegistrationCenterResponseDto rcpdto = new RegistrationCenterResponseDto();
		RegistrationCenterDto registrationCenterHistory = new RegistrationCenterDto();
		registrationCenterHistory.setId("1001");
		registrationCenterHistory.setIsActive(true);
		rcpdto.setRegistrationCentersHistory(Arrays.asList(registrationCenterHistory));
		responseWrapper.setResponse(rcpdto);
		responseWrapper.setErrors(null);

		Mockito.when(registrationProcessorRestService.getApi(any(), any(), anyString(), anyString(),
				eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		centerValidator.validate("eng", regOsi, "45128164920495");
	}

	@Test(expected = ValidationFailedException.class)
	public void validateWithoutTimestampCenterInActiveFailureTest() throws IOException, BaseCheckedException {
		ReflectionTestUtils.setField(centerValidator, "isWorkingHourValidationRequired", false);
		RegOsiDto regOsi = new RegOsiDto();
		regOsi.setRegId("45128164920495");
		regOsi.setRegcntrId("1001");
		regOsi.setPacketCreationDate("2021-06-04T07:31:59.831Z");

		ResponseWrapper<RegistrationCenterResponseDto> responseWrapper = new ResponseWrapper<RegistrationCenterResponseDto>();
		RegistrationCenterResponseDto rcpdto = new RegistrationCenterResponseDto();
		RegistrationCenterDto registrationCenterHistory = new RegistrationCenterDto();
		registrationCenterHistory.setId("1001");
		registrationCenterHistory.setIsActive(false);
		rcpdto.setRegistrationCentersHistory(Arrays.asList(registrationCenterHistory));
		responseWrapper.setResponse(rcpdto);
		responseWrapper.setErrors(null);

		Mockito.when(registrationProcessorRestService.getApi(any(), any(), anyString(), anyString(),
				eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		centerValidator.validate("eng", regOsi, "45128164920495");
	}

	@Test(expected = BaseCheckedException.class)
	public void validateWithoutTimestampCenterNotFoundFailureTest() throws IOException, BaseCheckedException {
		ReflectionTestUtils.setField(centerValidator, "isWorkingHourValidationRequired", false);
		RegOsiDto regOsi = new RegOsiDto();
		regOsi.setRegId("45128164920495");
		regOsi.setRegcntrId("1001");
		regOsi.setPacketCreationDate("2021-06-04T07:31:59.831Z");

		ResponseWrapper<RegistrationCenterResponseDto> responseWrapper = new ResponseWrapper<RegistrationCenterResponseDto>();
		ErrorDTO error = new ErrorDTO();
		error.setErrorCode("ERR-001");
		error.setMessage("center not found");
		responseWrapper.setResponse(null);
		responseWrapper.setErrors(Arrays.asList(error));

		Mockito.when(registrationProcessorRestService.getApi(any(), any(), anyString(), anyString(),
				eq(ResponseWrapper.class))).thenReturn(responseWrapper);
		centerValidator.validate("eng", regOsi, "45128164920495");
	}

	@Test
	public void validateWithTimestampSuccessTest() throws IOException, BaseCheckedException {
		ReflectionTestUtils.setField(centerValidator, "isWorkingHourValidationRequired", true);
		RegOsiDto regOsi = new RegOsiDto();
		regOsi.setRegId("45128164920495");
		regOsi.setRegcntrId("1001");
		regOsi.setPacketCreationDate("2021-06-04T07:31:59.831Z");

		ResponseWrapper<RegistrationCenterResponseDto> responseWrapper = new ResponseWrapper<RegistrationCenterResponseDto>();
		RegistrationCenterResponseDto rcpdto = new RegistrationCenterResponseDto();
		RegistrationCenterDto registrationCenterHistory = new RegistrationCenterDto();
		registrationCenterHistory.setId("1001");
		registrationCenterHistory.setIsActive(true);
		rcpdto.setRegistrationCentersHistory(Arrays.asList(registrationCenterHistory));
		responseWrapper.setResponse(rcpdto);
		responseWrapper.setErrors(null);

		ResponseWrapper<RegistartionCenterTimestampResponseDto> responseWrapper2 = new ResponseWrapper<RegistartionCenterTimestampResponseDto>();
		RegistartionCenterTimestampResponseDto centerTimestampResponseDto = new RegistartionCenterTimestampResponseDto();
		centerTimestampResponseDto.setErrors(null);
		centerTimestampResponseDto.setStatus("Valid");
		responseWrapper2.setResponse(centerTimestampResponseDto);
		responseWrapper2.setErrors(null);

		Mockito.when(registrationProcessorRestService.getApi(any(), any(), anyString(), anyString(),
				eq(ResponseWrapper.class))).thenReturn(responseWrapper).thenReturn(responseWrapper2);
		centerValidator.validate("eng", regOsi, "45128164920495");
	}
	
	@Test(expected = ValidationFailedException.class)
	public void validateWithTimestampCenterNotFoundFailureTest() throws IOException, BaseCheckedException {
		ReflectionTestUtils.setField(centerValidator, "isWorkingHourValidationRequired", true);
		RegOsiDto regOsi = new RegOsiDto();
		regOsi.setRegId("45128164920495");
		regOsi.setRegcntrId("1001");
		regOsi.setPacketCreationDate("2021-06-04T07:31:59.831Z");

		ResponseWrapper<RegistrationCenterResponseDto> responseWrapper = new ResponseWrapper<RegistrationCenterResponseDto>();
		RegistrationCenterResponseDto rcpdto = new RegistrationCenterResponseDto();
		RegistrationCenterDto registrationCenterHistory = new RegistrationCenterDto();
		registrationCenterHistory.setId("1001");
		registrationCenterHistory.setIsActive(true);
		rcpdto.setRegistrationCentersHistory(Arrays.asList(registrationCenterHistory));
		responseWrapper.setResponse(rcpdto);
		responseWrapper.setErrors(null);
		
		ResponseWrapper<RegistartionCenterTimestampResponseDto> responseWrapper2 = new ResponseWrapper<RegistartionCenterTimestampResponseDto>();
		RegistartionCenterTimestampResponseDto centerTimestampResponseDto = new RegistartionCenterTimestampResponseDto();
		centerTimestampResponseDto.setErrors(null);
		centerTimestampResponseDto.setStatus("Invalid");
		responseWrapper2.setResponse(centerTimestampResponseDto);
		responseWrapper2.setErrors(null);

		Mockito.when(registrationProcessorRestService.getApi(any(), any(), anyString(), anyString(),
				eq(ResponseWrapper.class))).thenReturn(responseWrapper).thenReturn(responseWrapper2);
		centerValidator.validate("eng", regOsi, "45128164920495");
	}

}
