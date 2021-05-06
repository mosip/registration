package io.mosip.registration.processor.reprocessor.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.WorkFlowSearchException;
import io.mosip.registration.processor.core.exception.WorkflowActionException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.workflow.dto.PageResponseDto;
import io.mosip.registration.processor.core.workflow.dto.SearchDto;
import io.mosip.registration.processor.core.workflow.dto.SearchResponseDto;
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

	/** The module name. */
	public static String MODULE_NAME = ModuleName.WORKFLOW_ACTION_SERVICE.toString();

	/** The module id. */
	public static String MODULE_ID = PlatformSuccessMessages.RPR_WORKFLOW_ACTION_SERVICE_SUCCESS.getCode();

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(WorkflowActionService.class);

	public PageResponseDto<SearchResponseDto> searchRegistrationDetails(SearchDto searchDto)
			throws WorkFlowSearchException {

		PageResponseDto<SearchResponseDto> pageResponseDto = new PageResponseDto<>();
		List<SearchResponseDto> searchResDtos = new ArrayList<SearchResponseDto>();
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"WorkflowSearchService::searchRegistrationDetails()::entry");
		try {
			Page<InternalRegistrationStatusDto> pageDtos = registrationStatusService.searchRegistrationDetails(searchDto);

			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
					"WorkflowSearchService::searchRegistrationDetails()::exit");

			if (!pageDtos.getContent().isEmpty()) {
				for (InternalRegistrationStatusDto regs : pageDtos.getContent()) {
					SearchResponseDto searchRes = dtoMapper(regs);
					searchResDtos.add(searchRes);
				}

			}
			pageResponseDto = pageResponse(pageDtos);
			pageResponseDto.setData(searchResDtos);

		} catch (DataAccessLayerException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", e.getMessage() + ExceptionUtils.getStackTrace(e));
			logAndThrowError(e, PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getCode(),
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage(), null);
		}
		return pageResponseDto;

	}

	public static <T, D> PageResponseDto<D> pageResponse(Page<T> page) {
		PageResponseDto<D> pageResponse = new PageResponseDto<>();
		if (page != null) {
			long totalItem = page.getTotalElements();
			int pageSize = page.getSize();
			int start = (page.getNumber() * pageSize) + 1;
			pageResponse.setFromRecord(start);
			pageResponse.setToRecord((long) (start - 1) + page.getNumberOfElements());
			pageResponse.setTotalRecord(totalItem);
		}
		return pageResponse;
	}

	private SearchResponseDto dtoMapper(InternalRegistrationStatusDto regSt) {
		SearchResponseDto wfs = new SearchResponseDto();
		wfs.setRegistrationId(regSt.getRegistrationId());
		wfs.setCreateDateTime(regSt.getCreateDateTime().toString());
		wfs.setCreatedBy(regSt.getCreatedBy());
		wfs.setCurrentStageName(regSt.getRegistrationStageName());
		wfs.setDefaultResumeAction(regSt.getDefaultResumeAction());
		wfs.setResumeRemoveTags(regSt.getResumeRemoveTags());
		wfs.setStatusComment(regSt.getStatusComment());
		wfs.setStatusCode(regSt.getStatusCode());
		if (regSt.getResumeTimeStamp() != null) {
			wfs.setResumeTimeStamp(regSt.getResumeTimeStamp().toString());
		}
		wfs.setUpdatedBy(regSt.getUpdatedBy());
		if (regSt.getUpdateDateTime() != null) {
		wfs.setUpdateDateTime(regSt.getUpdateDateTime().toString());
		}
		return wfs;
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
		regProcLogger.error("Error in  processWorkflowSearch   {} {} {} {}",
				errorMessage, e.getMessage(), ExceptionUtils.getStackTrace(e));
		throw new WorkFlowSearchException(errorCode, errorMessage);
	}



}
