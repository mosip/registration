package io.mosip.registration.processor.workflowmanager.service.test;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.WebApplicationContext;

import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.registration.processor.core.exception.WorkFlowSearchException;
import io.mosip.registration.processor.core.exception.WorkflowActionException;
import io.mosip.registration.processor.core.workflow.dto.FilterInfo;
import io.mosip.registration.processor.core.workflow.dto.PaginationInfo;
import io.mosip.registration.processor.core.workflow.dto.SearchInfo;
import io.mosip.registration.processor.core.workflow.dto.WorkflowDetail;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.workflowmanager.service.WorkflowSearchService;

@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
public class WorkflowSearchServiceTest {

	@InjectMocks
	WorkflowSearchService WorkflowSearchService = new WorkflowSearchService();

	@Mock
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	@Mock
	AuditLogRequestBuilder auditLogRequestBuilder;
	SearchInfo searchInfo = new SearchInfo();

	@Before
	public void setUp() throws Exception {
		FilterInfo filterInfo = new FilterInfo("id", "45128164920495");
		searchInfo.setFilters(Arrays.asList(filterInfo));
		PaginationInfo pagination = new PaginationInfo(1, 5);
		searchInfo.setPagination(pagination);
	}

	@Test
	public void searchRegistrationDetailsSuccessTest() throws WorkflowActionException, WorkFlowSearchException {

		List<InternalRegistrationStatusDto> regList = new ArrayList<InternalRegistrationStatusDto>();
		InternalRegistrationStatusDto internalRegis = new InternalRegistrationStatusDto();
		internalRegis.setRegistrationId("45128164920495");
		internalRegis.setCreateDateTime(LocalDateTime.now());
		internalRegis.setCreatedBy("admin");
		internalRegis.setRegistrationType("New");
		internalRegis.setStatusCode("Success");
		internalRegis.setUpdateDateTime(LocalDateTime.now());
		regList.add(internalRegis);

		Page<InternalRegistrationStatusDto> pageDtos = new PageImpl<InternalRegistrationStatusDto>(regList);
		Mockito.when(registrationStatusService.searchRegistrationDetails(any())).thenReturn(pageDtos);
		Page<WorkflowDetail> result = WorkflowSearchService.searchRegistrationDetails(searchInfo);
		assertEquals(result.getContent().size(), 1);
	}

	@Test(expected = WorkFlowSearchException.class)
	public void searchRegistrationDetailsFailureTest() throws WorkFlowSearchException {

		DataAccessLayerException exc = new DataAccessLayerException("ERR-001", "Data access layer exception", null);
		Mockito.when(registrationStatusService.searchRegistrationDetails(any())).thenThrow(exc);
		WorkflowSearchService.searchRegistrationDetails(searchInfo);
	}

}
