package io.mosip.registration.processor.workflowmanager.service.test;

import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;

import io.mosip.registration.processor.core.exception.WorkflowInstanceRequestValidationException;
import io.mosip.registration.processor.core.workflow.dto.WorkflowInstanceDTO;
import io.mosip.registration.processor.workflowmanager.validator.WorkflowInstanceRequestValidator;

@RunWith(SpringRunner.class)
public class WorkflowInstanceRequestValidatorTest {
    @Mock
    private Environment env;

    @InjectMocks
    WorkflowInstanceRequestValidator workflowInstanceRequestValidator;

    @Before
    public void setup() {
        when(env.getProperty("mosip.regproc.workflow-manager.instance.api-id"))
                .thenReturn("mosip.registration.processor.workflow.create");
        when(env.getProperty("mosip.regproc.workflow-manager.instance.version")).thenReturn("1.0");
    }

    @Test
    public void testValidateSuccess() throws WorkflowInstanceRequestValidationException {
        WorkflowInstanceDTO workflowInstanceDTO = new WorkflowInstanceDTO();
        workflowInstanceDTO.setId("mosip.registration.processor.workflow.create");
        workflowInstanceDTO.setVersion("1.0");
        workflowInstanceDTO.setRequesttime("2021-03-15T10:02:45.474Z");

        workflowInstanceRequestValidator.validate(workflowInstanceDTO);
    }

    @Test(expected = WorkflowInstanceRequestValidationException.class)
    public void testMissingId() throws WorkflowInstanceRequestValidationException {
        WorkflowInstanceDTO workflowInstanceDTO = new WorkflowInstanceDTO();

        workflowInstanceDTO.setVersion("1.0");
        workflowInstanceDTO.setRequesttime("2021-03-15T10:02:45.474Z");

        workflowInstanceRequestValidator.validate(workflowInstanceDTO);

    }

    @Test(expected = WorkflowInstanceRequestValidationException.class)
    public void testMissingVersion() throws WorkflowInstanceRequestValidationException {
        WorkflowInstanceDTO workflowInstanceDTO = new WorkflowInstanceDTO();
        workflowInstanceDTO.setId("mosip.registration.processor.workflow.create");

        workflowInstanceRequestValidator.validate(workflowInstanceDTO);
    }

    @Test(expected = WorkflowInstanceRequestValidationException.class)
    public void testMissingRequesttime() throws WorkflowInstanceRequestValidationException {
        WorkflowInstanceDTO workflowInstanceDTO = new WorkflowInstanceDTO();
        workflowInstanceDTO.setId("mosip.registration.processor.workflow.create");
        workflowInstanceDTO.setVersion("1.0");

        workflowInstanceRequestValidator.validate(workflowInstanceDTO);
    }

    @Test(expected = WorkflowInstanceRequestValidationException.class)
    public void testInValidId() throws WorkflowInstanceRequestValidationException {
        WorkflowInstanceDTO workflowInstanceDTO = new WorkflowInstanceDTO();
        workflowInstanceDTO.setId("mosip.registration.processor.workflow");

        workflowInstanceRequestValidator.validate(workflowInstanceDTO);

    }

    @Test(expected = WorkflowInstanceRequestValidationException.class)
    public void testInValidVersion() throws WorkflowInstanceRequestValidationException {
        WorkflowInstanceDTO workflowInstanceDTO = new WorkflowInstanceDTO();
        workflowInstanceDTO.setId("mosip.registration.processor.workflow.create");
        workflowInstanceDTO.setVersion("1");
        workflowInstanceRequestValidator.validate(workflowInstanceDTO);

    }

    @Test(expected = WorkflowInstanceRequestValidationException.class)
    public void testInValidRequestTime() throws WorkflowInstanceRequestValidationException {
        WorkflowInstanceDTO workflowInstanceDTO = new WorkflowInstanceDTO();
        workflowInstanceDTO.setId("mosip.registration.processor.workflow.create");
        workflowInstanceDTO.setVersion("1.0");
        workflowInstanceDTO.setRequesttime("2021-03-15T10:02:474Z");

        workflowInstanceRequestValidator.validate(workflowInstanceDTO);

    }
}