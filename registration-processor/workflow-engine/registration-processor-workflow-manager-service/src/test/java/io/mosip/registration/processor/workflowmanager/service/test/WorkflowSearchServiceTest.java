package io.mosip.registration.processor.workflowmanager.service.test;

import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.exception.WorkFlowSearchException;
import io.mosip.registration.processor.core.workflow.dto.FilterInfo;
import io.mosip.registration.processor.core.workflow.dto.PaginationInfo;
import io.mosip.registration.processor.core.workflow.dto.SearchInfo;
import io.mosip.registration.processor.core.workflow.dto.WorkflowDetail;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.workflowmanager.service.WorkflowSearchService;
import org.assertj.core.util.Lists;
import org.junit.Assert;
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

import java.time.LocalDateTime;

@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
public class WorkflowSearchServiceTest {

    @InjectMocks
    private WorkflowSearchService service;

    @Mock
    private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

    @Test
    public void testSearchRegistrationDetails() throws WorkFlowSearchException {

        SearchInfo searchInfo = new SearchInfo();
        FilterInfo filterInfo = new FilterInfo();
        filterInfo.setValue("filter");
        filterInfo.setColumnName("id");
        searchInfo.setFilters(Lists.newArrayList(filterInfo));
        PaginationInfo paginationInfo = new PaginationInfo();
        paginationInfo.setPageFetch(1);
        paginationInfo.setPageStart(1);
        searchInfo.setPagination(paginationInfo);

        InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();
        registrationStatusDto.setIsActive(true);
        registrationStatusDto.setStatusCode("PACKET_UPLOADED_TO_VIRUS_SCAN");
        registrationStatusDto.setCreateDateTime(LocalDateTime.now());
        registrationStatusDto.setRegistrationStageName("PacketValidatorStage");
        registrationStatusDto.setReProcessRetryCount(0);
        registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());

        Page<InternalRegistrationStatusDto> page = new PageImpl<InternalRegistrationStatusDto>(Lists.newArrayList(registrationStatusDto));

        Mockito.when(registrationStatusService.searchRegistrationDetails(searchInfo)).thenReturn(page);

        Page<WorkflowDetail> result = service.searchRegistrationDetails(searchInfo);

        Assert.assertTrue(result.getContent().size() == 1);

    }


    @Test(expected = WorkFlowSearchException.class)
    public void testDataAccessException() throws WorkFlowSearchException {

        SearchInfo searchInfo = new SearchInfo();
        FilterInfo filterInfo = new FilterInfo();
        filterInfo.setValue("filter");
        filterInfo.setColumnName("workflowType");
        searchInfo.setFilters(Lists.newArrayList(filterInfo));
        PaginationInfo paginationInfo = new PaginationInfo();
        paginationInfo.setPageFetch(1);
        paginationInfo.setPageStart(1);
        searchInfo.setPagination(paginationInfo);


        Mockito.when(registrationStatusService.searchRegistrationDetails(searchInfo)).thenThrow(new DataAccessLayerException("ex", "ex", new BaseUncheckedException()));

        Page<WorkflowDetail> result = service.searchRegistrationDetails(searchInfo);

    }
}
