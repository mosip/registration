package io.mosip.registration.processor.status.validator;

import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import io.mosip.registration.processor.status.dto.RegistrationExternalStatusSubRequestDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.status.dto.RegistrationExternalStatusRequestDTO;
import io.mosip.registration.processor.status.dto.RegistrationStatusSubRequestDto;
import io.mosip.registration.processor.status.exception.RegStatusAppException;

@RunWith(MockitoJUnitRunner.class)
public class RegistrationExternalStatusRequestValidatorTest {
	
	@InjectMocks
	private RegistrationExternalStatusRequestValidator validator;
	
	@Mock
	private Environment env;
	
	RegistrationExternalStatusRequestDTO registrationExternalStatusRequestDTO = new RegistrationExternalStatusRequestDTO();
	List<RegistrationExternalStatusSubRequestDto> request = new ArrayList<RegistrationExternalStatusSubRequestDto>();
	RegistrationExternalStatusSubRequestDto dto = new RegistrationExternalStatusSubRequestDto();
	
	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";
	
	@Before
	public void setup() throws FileNotFoundException {
		
		when(env.getProperty("mosip.registration.processor.registration.external.status.version")).thenReturn("1.0");
		when(env.getProperty("mosip.registration.processor.datetime.pattern")).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		when(env.getProperty("mosip.registration.processor.timezone")).thenReturn("GMT");
		ReflectionTestUtils.setField(validator, "gracePeriod", 10080);
		
		dto.setRegistrationId("10005100010001120210531122249");
		request.add(dto);
		registrationExternalStatusRequestDTO.setRequest(request);
		registrationExternalStatusRequestDTO.setId("mosip.registration.external.status");
		registrationExternalStatusRequestDTO.setRequesttime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
		registrationExternalStatusRequestDTO.setVersion("1.0");
	}
	
	@Test
	public void validatorSuccessTest() throws Exception {
		validator.validate(registrationExternalStatusRequestDTO, "mosip.registration.external.status");
	}
	
	@Test(expected = RegStatusAppException.class)
	public void validatorFailedIDTest() throws Exception {
		validator.validate(registrationExternalStatusRequestDTO, "mosip.registration.status");
	}
	
	@Test(expected = RegStatusAppException.class)
	public void validatorFailedVersionTest() throws Exception {
		registrationExternalStatusRequestDTO.setVersion(null);
		validator.validate(registrationExternalStatusRequestDTO, "mosip.registration.external.status");
	}
	
	@Test(expected = RegStatusAppException.class)
	public void validatorFailedRequestTimeTest() throws Exception {
		registrationExternalStatusRequestDTO.setRequesttime(null);
		validator.validate(registrationExternalStatusRequestDTO, "mosip.registration.external.status");
	}

}
