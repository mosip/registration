package io.mosip.registration.processor.reprocessor.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.code.WorkflowActionCode;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

@Component
public class WorkflowActionService {
	
	/** The registration status service. */
	@Autowired
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	@Autowired
	private PacketManagerService packetManagerService;

	@Value("${mosip.regproc.workflow.action.hotlisted.tag")
	private String hotListedTag;

	/** The Constant USER. */
	private static final String USER = "MOSIP_SYSTEM";

	public void processWorkflowAction(List<String> workflowIds, String workflowAction,
			MosipVerticleAPIManager workflowActionApiOrVerticle)
			throws TablenotAccessibleException {
		WorkflowActionCode workflowActionCode = WorkflowActionCode.valueOf(workflowAction);
		switch (workflowActionCode) {
		case RESUME_PROCESSING:
			processResumeProcessing(workflowIds, workflowActionApiOrVerticle, workflowActionCode);
		case RESUME_PROCESSING_AND_REMOVE_HOTLISTED_TAG:
			processResumeProcessingAndRemoveHotlistedTag(workflowIds, workflowActionApiOrVerticle, workflowActionCode);
		case RESUME_FROM_BEGINNING:
			processResumeFromBeginning(workflowIds, workflowActionApiOrVerticle, workflowActionCode);
		case RESUME_FROM_BEGINNING_AND_REMOVE_HOTLISTED_TAG:
			processResumeFromBeginningAndRemoveHotlistedTag(workflowIds, workflowActionApiOrVerticle,
					workflowActionCode);
		case STOP_PROCESSING:
			processStopProcessing(workflowIds, workflowActionApiOrVerticle, workflowActionCode);
		default:
			/*throw new BaseCheckedException(PlatformErrorMessages.RPR_PCM_UNKNOWN_SCHEMA_DATA_TYPE.getCode(),
					PlatformErrorMessages.RPR_PCM_UNKNOWN_SCHEMA_DATA_TYPE.getMessage() + " Field name: " + ""
							+ " type: " + "");*/
		}
	}

	private void processStopProcessing(List<String> workflowIds, MosipVerticleAPIManager workflowActionApiOrVerticle,
			WorkflowActionCode workflowActionCode) {
		// TODO Auto-generated method stub

	}

	private void processResumeFromBeginningAndRemoveHotlistedTag(List<String> workflowIds,
			MosipVerticleAPIManager workflowActionApiOrVerticle, WorkflowActionCode workflowActionCode) {
		// TODO Auto-generated method stub

	}

	private void processResumeFromBeginning(List<String> workflowIds,
			MosipVerticleAPIManager workflowActionApiOrVerticle, WorkflowActionCode workflowActionCode) {
		// TODO Auto-generated method stub

	}

	private void processResumeProcessingAndRemoveHotlistedTag(List<String> workflowIds,
			MosipVerticleAPIManager workflowActionApiOrVerticle, WorkflowActionCode workflowActionCode) {
		// TODO Auto-generated method stub

	}

	private void processResumeProcessing(List<String> workflowIds,
			MosipVerticleAPIManager workflowActionApiOrVerticle,WorkflowActionCode workflowActionCode) {
		if (!CollectionUtils.isEmpty(workflowIds)) {
			workflowIds.forEach(rid -> {
				InternalRegistrationStatusDto registrationStatusDto = registrationStatusService
						.getRegistrationStatus(rid);
				try {
					if (removeHotlistedTag(rid)) {
						registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
						registrationStatusDto.setStatusComment("process the workflowAction" + workflowActionCode);

						LocalDateTime updateTimeStamp = DateUtils.getUTCCurrentDateTime();
						registrationStatusDto.setUpdateDateTime(updateTimeStamp);
						registrationStatusDto.setUpdatedBy(USER);
					} else {

					}
				} catch (ApisResourceAccessException | JsonProcessingException | PacketManagerException
						| IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			});
		}

	}

	private boolean removeHotlistedTag(String rid)
			throws JsonParseException, JsonMappingException, ApisResourceAccessException, JsonProcessingException,
			PacketManagerException, com.fasterxml.jackson.core.JsonProcessingException, IOException {
		List<String> deleteTags = new ArrayList<String>();
		deleteTags.add(hotListedTag);
		return packetManagerService.deleteTags(rid, deleteTags);

	}
}
