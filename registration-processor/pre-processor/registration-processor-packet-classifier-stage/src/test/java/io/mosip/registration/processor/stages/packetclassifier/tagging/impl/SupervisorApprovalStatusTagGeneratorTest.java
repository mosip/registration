package io.mosip.registration.processor.stages.packetclassifier.tagging.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.service.SyncRegistrationService;

/**
 * The Class PacketValidatorStageTest.
 */
@RefreshScope
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*", "com.sun.org.apache.xerces.*", 
	"javax.xml.*", "org.xml.*" })
public class SupervisorApprovalStatusTagGeneratorTest {

	private static String tagName = "SUPERVISOR_APPROVAL_STATUS";

	@InjectMocks
	private SupervisorApprovalStatusTagGenerator supervisorApprovalStatusTagGenerator;

	@Mock
    private SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationService;

	@Before
	public void setup() throws Exception {
		Whitebox.setInternalState(supervisorApprovalStatusTagGenerator, "tagName", tagName);
	}

	@Test
	public void testGenerateTagsForSuccess() throws BaseCheckedException {
		String supervisorStatus = "APPROVED";
		SyncRegistrationEntity syncRegistrationEntity = new SyncRegistrationEntity();
		syncRegistrationEntity.setSupervisorStatus(supervisorStatus);
		Mockito.when(syncRegistrationService.findByWorkflowInstanceId(anyString()))
			.thenReturn(syncRegistrationEntity);
		Map<String, String> tags = 
			supervisorApprovalStatusTagGenerator.generateTags("12345", "1234", "NEW", null, null, 0);
		assertEquals(supervisorStatus, tags.get(tagName));
	}

	@Test(expected = BaseCheckedException.class)
	public void testGenerateTagsForSyncRegistrationStatusNotAvailable() throws BaseCheckedException {
		Mockito.when(syncRegistrationService.findByWorkflowInstanceId(anyString()))
			.thenReturn(null);
		supervisorApprovalStatusTagGenerator.generateTags("12345", "1234", "NEW", null, null, 0);
	}
	
	@Test
	public void getRequiredIdObjectFieldNamesTest() throws Exception {
		List<String> result = supervisorApprovalStatusTagGenerator.getRequiredIdObjectFieldNames();
		assertEquals(result, null);
	}
	
}
