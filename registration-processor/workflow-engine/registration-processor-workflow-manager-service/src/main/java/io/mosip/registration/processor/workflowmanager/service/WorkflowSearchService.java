package io.mosip.registration.processor.workflowmanager.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.exception.WorkFlowSearchException;
import io.mosip.registration.processor.core.exception.WorkflowActionException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.workflow.dto.FilterInfo;
import io.mosip.registration.processor.core.workflow.dto.SearchInfo;
import io.mosip.registration.processor.core.workflow.dto.WorkflowDetail;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

@Component
public class WorkflowSearchService {

	/** The registration status service. */
	@Autowired
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(WorkflowSearchService.class);

	public Page<WorkflowDetail> searchRegistrationDetails(SearchInfo searchInfo)
			throws WorkFlowSearchException {
		Page<InternalRegistrationStatusDto> pageDtos = null;
		List<WorkflowDetail> workflowDetails = new ArrayList<WorkflowDetail>();
		regProcLogger.debug(
				"WorkflowSearchService::searchRegistrationDetails()::entry");
		try {
			buildSearchInfoDto(searchInfo);

			pageDtos = registrationStatusService.searchRegistrationDetails(searchInfo);
			if (!pageDtos.getContent().isEmpty()) {
				for (InternalRegistrationStatusDto regs : pageDtos.getContent()) {
					WorkflowDetail workflowDetail = convertToWorkflowDetail(regs);
					workflowDetails.add(workflowDetail);
				}
			}
			regProcLogger.debug("WorkflowSearchService::searchRegistrationDetails()::exit");
		} catch (DataAccessLayerException e) {
			regProcLogger.error(e.getMessage() + ExceptionUtils.getStackTrace(e));
			logAndThrowError(e, PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getCode(),
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage());
		}
		return new PageImpl<>(workflowDetails,
				PageRequest.of(searchInfo.getPagination().getPageStart(), searchInfo.getPagination().getPageFetch()),
				pageDtos.getTotalElements());

	}

	private void buildSearchInfoDto(SearchInfo searchInfo) {
		for (FilterInfo filterInfo : searchInfo.getFilters()) {
			if (filterInfo.getColumnName().equals("id")) {
				filterInfo.setColumnName("regId");
			} else if (filterInfo.getColumnName().equals("workflowType")) {
				filterInfo.setColumnName("registrationType");
			}
		}
	}

	/**
	 * Log and throw error.
	 *
	 * @param e              the e
	 * @param errorCode      the error code
	 * @param errorMessage   the error message
	 * @param registrationId the registration id
	 * @throws WorkFlowSearchException
	 * @throws WorkflowActionException the workflow action exception
	 */
	private void logAndThrowError(Exception e, String errorCode, String errorMessage) throws WorkFlowSearchException {
		regProcLogger.error("Error in  processWorkflowSearch", errorMessage, e.getMessage(),
				ExceptionUtils.getStackTrace(e));
		throw new WorkFlowSearchException(errorCode, errorMessage);
	}

	private WorkflowDetail convertToWorkflowDetail(InternalRegistrationStatusDto regSt) {
		WorkflowDetail wfd = new WorkflowDetail();
		wfd.setWorkflowId(regSt.getRegistrationId());
		wfd.setCreateDateTime(regSt.getCreateDateTime().toString());
		wfd.setCreatedBy(regSt.getCreatedBy());
		wfd.setCurrentStageName(regSt.getRegistrationStageName());
		wfd.setDefaultResumeAction(regSt.getDefaultResumeAction());
		wfd.setPauseRuleIds(regSt.getPauseRuleIds());
		wfd.setLastSuccessStageName(regSt.getLastSuccessStageName());
		wfd.setStatusComment(regSt.getStatusComment());
		wfd.setStatusCode(regSt.getStatusCode());
		if (regSt.getResumeTimeStamp() != null) {
			wfd.setResumeTimestamp(regSt.getResumeTimeStamp().toString());
		}
		wfd.setWorkflowType(regSt.getRegistrationType());
		wfd.setUpdatedBy(regSt.getUpdatedBy());
		if (regSt.getUpdateDateTime() != null) {
			wfd.setUpdateDateTime(regSt.getUpdateDateTime().toString());
		}
		return wfd;
	}



}
