package io.mosip.registration.processor.reprocessor.validator.test;

import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;

import io.mosip.registration.processor.core.exception.WorkflowActionRequestValidationException;
import io.mosip.registration.processor.core.workflow.dto.WorkflowActionDTO;
import io.mosip.registration.processor.reprocessor.validator.WorkflowActionRequestValidator;

@RunWith(SpringRunner.class)
public class WorkflowActionRequestValidatorTest {
	@Mock
	private Environment env;

	@InjectMocks
	WorkflowActionRequestValidator workflowActionRequestValidator;

	@Before
	public void setup() {
		when(env.getProperty("mosip.registration.processor.workflow.action.id"))
				.thenReturn("mosip.registration.processor.workflow.action");
		when(env.getProperty("mosip.registration.processor.workflow.action.version")).thenReturn("1.0");
	}

	@Test
	public void testValidateSuccess() throws WorkflowActionRequestValidationException {
		WorkflowActionDTO workflowActionDTO = new WorkflowActionDTO();
		workflowActionDTO.setId("mosip.registration.processor.workflow.action");
		workflowActionDTO.setVersion("1.0");
		workflowActionDTO.setRequesttime("2021-03-15T10:02:45.474Z");

		workflowActionRequestValidator.validate(workflowActionDTO);
	}

	@Test(expected = WorkflowActionRequestValidationException.class)
	public void testMissingId() throws WorkflowActionRequestValidationException {
		WorkflowActionDTO workflowActionDTO = new WorkflowActionDTO();

		workflowActionDTO.setVersion("1.0");
		workflowActionDTO.setRequesttime("2021-03-15T10:02:45.474Z");

		workflowActionRequestValidator.validate(workflowActionDTO);

	}

	@Test(expected = WorkflowActionRequestValidationException.class)
	public void testMissingVersion() throws WorkflowActionRequestValidationException {
		WorkflowActionDTO workflowActionDTO = new WorkflowActionDTO();
		workflowActionDTO.setId("mosip.registration.processor.workflow.action");

		workflowActionRequestValidator.validate(workflowActionDTO);
	}

	@Test(expected = WorkflowActionRequestValidationException.class)
	public void testMissingRequesttime() throws WorkflowActionRequestValidationException {
		WorkflowActionDTO workflowActionDTO = new WorkflowActionDTO();
		workflowActionDTO.setId("mosip.registration.processor.workflow.action");
		workflowActionDTO.setVersion("1.0");

		workflowActionRequestValidator.validate(workflowActionDTO);
	}

	@Test(expected = WorkflowActionRequestValidationException.class)
	public void testInValidId() throws WorkflowActionRequestValidationException {
		WorkflowActionDTO workflowActionDTO = new WorkflowActionDTO();
		workflowActionDTO.setId("mosip.registration.processor.workflow");

		workflowActionRequestValidator.validate(workflowActionDTO);

	}

	@Test(expected = WorkflowActionRequestValidationException.class)
	public void testInValidVersion() throws WorkflowActionRequestValidationException {
		WorkflowActionDTO workflowActionDTO = new WorkflowActionDTO();
		workflowActionDTO.setId("mosip.registration.processor.workflow.action");
		workflowActionDTO.setVersion("1");
		workflowActionRequestValidator.validate(workflowActionDTO);

	}

	@Test(expected = WorkflowActionRequestValidationException.class)
	public void testInValidRequestTime() throws WorkflowActionRequestValidationException {
		WorkflowActionDTO workflowActionDTO = new WorkflowActionDTO();
		workflowActionDTO.setId("mosip.registration.processor.workflow.action");
		workflowActionDTO.setVersion("1.0");
		workflowActionDTO.setRequesttime("2021-03-15T10:02:474Z");

		workflowActionRequestValidator.validate(workflowActionDTO);

	}
}
