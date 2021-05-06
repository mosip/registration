package io.mosip.registration.processor.reprocessor.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.exception.WorkFlowSearchException;
import io.mosip.registration.processor.core.exception.WorkflowActionException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.workflow.dto.FilterInfo;
import io.mosip.registration.processor.core.workflow.dto.SearchInfo;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

@Component
public class WorkflowSearchService {

	/** The registration status service. */
	@Autowired
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The core audit request builder. */
	@Autowired
	AuditLogRequestBuilder auditLogRequestBuilder;

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(WorkflowActionService.class);

	public Page<InternalRegistrationStatusDto> searchRegistrationDetails(SearchInfo searchInfo)
			throws WorkFlowSearchException {
		Page<InternalRegistrationStatusDto> pageDtos = null;
		regProcLogger.debug(
				"WorkflowSearchService::searchRegistrationDetails()::entry");
		try {
			buildSearchInfoDto(searchInfo);
			pageDtos = registrationStatusService
					.searchRegistrationDetails(searchInfo);
			regProcLogger.debug("WorkflowSearchService::searchRegistrationDetails()::exit");
		} catch (DataAccessLayerException e) {
			regProcLogger.error(e.getMessage() + ExceptionUtils.getStackTrace(e));
			logAndThrowError(e, PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getCode(),
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage(), null);
		}
		return pageDtos;

	}



	private void buildSearchInfoDto(SearchInfo searchInfo) {
		for (FilterInfo filterInfo : searchInfo.getFilters()) {
			if (filterInfo.getColumnName().equals("workflowId")) {
				filterInfo.setColumnName("id");
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
	 * @param description    the description
	 * @throws WorkFlowSearchException
	 * @throws WorkflowActionException the workflow action exception
	 */
	private void logAndThrowError(Exception e, String errorCode, String errorMessage,
			LogDescription description) throws WorkFlowSearchException {
		description.setCode(errorCode);
		description.setMessage(errorMessage);
		regProcLogger.error("Error in  processWorkflowSearch",
				errorMessage, e.getMessage(), ExceptionUtils.getStackTrace(e));
		throw new WorkFlowSearchException(errorCode, errorMessage);
	}



}
