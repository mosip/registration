package io.mosip.registration.processor.status.validator;

import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.status.exception.RegStatusAppException;
import io.mosip.registration.processor.status.dto.PacketExternalStatusRequestDTO;
import io.mosip.registration.processor.status.dto.PacketExternalStatusSubRequestDTO;

@RunWith(MockitoJUnitRunner.class)
public class PacketExternalStatusRequestValidatorTest {
	
	@InjectMocks
	private PacketExternalStatusRequestValidator validator;
	
	@Mock
	private Environment env;
	
	PacketExternalStatusRequestDTO packetExternalStatusRequestDTO = new PacketExternalStatusRequestDTO();
	List<PacketExternalStatusSubRequestDTO> request = new ArrayList<PacketExternalStatusSubRequestDTO>();
	PacketExternalStatusSubRequestDTO dto = new PacketExternalStatusSubRequestDTO();
	
	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";
	
	@Before
	public void setup() throws FileNotFoundException {
		
		when(env.getProperty("mosip.registration.processor.packet.external.status.version")).thenReturn("1.0");
		when(env.getProperty("mosip.registration.processor.datetime.pattern")).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		when(env.getProperty("mosip.registration.processor.timezone")).thenReturn("GMT");
		ReflectionTestUtils.setField(validator, "gracePeriod", 10080);
		
		dto.setPacketId("test1");
		request.add(dto);
		packetExternalStatusRequestDTO.setRequest(request);
		packetExternalStatusRequestDTO.setId("mosip.registration.packet.external.status");
		packetExternalStatusRequestDTO.setRequesttime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
		packetExternalStatusRequestDTO.setVersion("1.0");
	}
	
	@Test
	public void validatorSuccessTest() throws Exception {
		validator.validate(packetExternalStatusRequestDTO, "mosip.registration.packet.external.status");
	}
	
	@Test(expected = RegStatusAppException.class)
	public void validatorFailedIDTest() throws Exception {
		validator.validate(packetExternalStatusRequestDTO, "mosip.registration.packet.status");
	}
	
	@Test(expected = RegStatusAppException.class)
	public void validatorFailedVersionTest() throws Exception {
		packetExternalStatusRequestDTO.setVersion(null);
		validator.validate(packetExternalStatusRequestDTO, "mosip.registration.packet.external.status");
	}
	
	@Test(expected = RegStatusAppException.class)
	public void validatorFailedRequestTimeTest() throws Exception {
		packetExternalStatusRequestDTO.setRequesttime(null);
		validator.validate(packetExternalStatusRequestDTO, "mosip.registration.packet.external.status");
	}
}
