package io.mosip.registration.processor.workflowmanager.service;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import io.mosip.registration.processor.status.service.impl.SyncRegistrationServiceImpl;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionTypeCode;
import io.mosip.registration.processor.core.exception.AdditionalInfoIdNotFoundException;
import io.mosip.registration.processor.core.exception.WorkflowInstanceException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.AdditionalInfoRequestDto;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.workflow.dto.WorkflowInstanceRequestDTO;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dao.SyncRegistrationDao;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.encryptor.Encryptor;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.AdditionalInfoRequestService;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

/**
 * The Class WorkflowInstanceService.
 */
@Component
public class WorkflowInstanceService {

    /** The registration status service. */
    @Autowired
    RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

    /** The sync registration dao. */
    @Autowired
    private SyncRegistrationDao syncRegistrationDao;

    /** The core audit request builder. */
    @Autowired
    AuditLogRequestBuilder auditLogRequestBuilder;

    /** The encryptor. */
    @Autowired
    private Encryptor encryptor;

    @Autowired
    private AdditionalInfoRequestService additionalInfoRequestService;

    /** The resume from beginning stage. */
    @Value("${mosip.regproc.workflow-manager.instance-beginning-stage:PacketValidatorStage}")
    private String beginningStage;

    @Value("#{'${registration.processor.main-processes}'.split(',')}")
    private List<String> mainProcesses;

    @Autowired
    private Utilities utility;

    @Autowired
    private SyncRegistrationServiceImpl syncRegistrationService;

    /** The module name. */
    public static String MODULE_NAME = ModuleName.WORKFLOW_INSTANCE_SERVICE.toString();

    /** The module id. */
    public static String MODULE_ID = PlatformSuccessMessages.RPR_WORKFLOW_INSTANCE_SERVICE_SUCCESS.getCode();

    /** The reg proc logger. */
    private static Logger regProcLogger = RegProcessorLogger.getLogger(WorkflowInstanceService.class);

    /**
     * Add record to registration table
     * @throws WorkflowInstanceException
     */
    public InternalRegistrationStatusDto createWorkflowInstance(WorkflowInstanceRequestDTO regRequest, String user) throws WorkflowInstanceException {
        regProcLogger.debug("createWorkflowInstance called for request {}", regRequest.toString());
        LogDescription description = new LogDescription();
        boolean isTransactionSuccessful = false;
        String rid = regRequest.getRegistrationId();
        InternalRegistrationStatusDto dto = new InternalRegistrationStatusDto();
        try {
            String referenceId=null;
            String timeStamp=DateUtils.formatToISOString(LocalDateTime.now());

            String additionalInfo = JsonUtils.javaObjectToJsonString(regRequest.getNotificationInfo());
            byte[] encryptedInfo = encryptor.encrypt(additionalInfo, utility.getRefId(regRequest.getRegistrationId(), referenceId), timeStamp);

            String workflowInstanceId = UUID.randomUUID().toString();
            SyncRegistrationEntity syncRegistrationEntity = new SyncRegistrationEntity();
//            syncRegistrationEntity =syncRegistrationService.convertDtoToEntity()
            syncRegistrationEntity.setWorkflowInstanceId(workflowInstanceId);
            syncRegistrationEntity.setRegistrationId(rid);
            syncRegistrationEntity.setSupervisorStatus("APPROVED");
            syncRegistrationEntity.setRegistrationType(regRequest.getProcess());
            syncRegistrationEntity.setLangCode("eng");
            syncRegistrationEntity.setCreatedBy(user);
            syncRegistrationEntity.setCreateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
            syncRegistrationEntity.setIsDeleted(false);
            syncRegistrationEntity.setPacketHashValue("");
            syncRegistrationEntity.setPacketSize(BigInteger.valueOf(0));
            syncRegistrationEntity.setOptionalValues(encryptedInfo);
            syncRegistrationDao.save(syncRegistrationEntity);

            int iteration = getIterationForSyncRecord(regRequest);
            dto.setRegistrationId(regRequest.getRegistrationId());
            dto.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.WORKFLOW_RESUME.toString());
            dto.setRegistrationStageName(beginningStage);
            dto.setRegistrationType(regRequest.getProcess());
            dto.setReferenceRegistrationId(null);
            dto.setStatusCode(RegistrationStatusCode.RESUMABLE.toString());
            dto.setLangCode("eng");
            dto.setStatusComment(PlatformSuccessMessages.RPR_WORKFLOW_INSTANCE_SERVICE_SUCCESS.getMessage());
            dto.setSubStatusCode(StatusUtil.WORKFLOW_INSTANCE_SERVICE_SUCCESS.getCode());
            dto.setReProcessRetryCount(0);
            dto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
            dto.setIsActive(true);
            dto.setCreatedBy(user);
            dto.setIsDeleted(false);
            dto.setSource(regRequest.getSource());
            dto.setIteration(iteration);
            dto.setWorkflowInstanceId(workflowInstanceId);

            registrationStatusService.addRegistrationStatus(dto, MODULE_ID, MODULE_NAME);
            description
                    .setMessage(PlatformSuccessMessages.RPR_WORKFLOW_INSTANCE_SERVICE_SUCCESS.getMessage());
            isTransactionSuccessful = true;
        } catch (TablenotAccessibleException e) {
            logAndThrowError(e, e.getErrorCode(), e.getMessage(), rid, description);
        } catch (Exception e) {
            logAndThrowError(e, PlatformErrorMessages.RPR_WIS_UNKNOWN_EXCEPTION.getCode(),
                    PlatformErrorMessages.RPR_WIS_UNKNOWN_EXCEPTION.getMessage(), rid, description);

        } finally {
            regProcLogger.debug("WorkflowInstanceService status for registration id {} {}", rid,
                    description.getMessage());
            updateAudit(description, rid, isTransactionSuccessful);
        }

        regProcLogger.debug("createWorkflowInstance call ended for request {}", regRequest.toString());
        return dto;
    }

    /**
     * Update audit.
     *
     * @param description             the description
     * @param registrationId          the registration id
     * @param isTransactionSuccessful the is transaction successful
     */
    private void updateAudit(LogDescription description, String registrationId, boolean isTransactionSuccessful) {

        String moduleId = isTransactionSuccessful ? MODULE_ID : description.getCode();

        String eventId = isTransactionSuccessful ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
        String eventName = isTransactionSuccessful ? EventName.UPDATE.toString() : EventName.EXCEPTION.toString();
        String eventType = isTransactionSuccessful ? EventType.BUSINESS.toString() : EventType.SYSTEM.toString();

        auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
                moduleId, MODULE_NAME, registrationId);
    }

    /**
     * Log and throw error.
     *
     * @param e              the e
     * @param errorCode      the error code
     * @param errorMessage   the error message
     * @param registrationId the registration id
     * @param description    the description
     * @throws WorkflowInstanceException the workflow instance exception
     */
    private void logAndThrowError(Exception e, String errorCode, String errorMessage, String registrationId,
                                  LogDescription description) throws WorkflowInstanceException {
        description.setCode(errorCode);
        description.setMessage(errorMessage);
        regProcLogger.error("Error in  createWorkflowInstance  for registration id  {} {} {} {}", registrationId,
                errorMessage, e.getMessage(), ExceptionUtils.getStackTrace(e));
        throw new WorkflowInstanceException(errorCode, errorMessage);
    }

    private int getIterationForSyncRecord(WorkflowInstanceRequestDTO regEntity) {
        if (mainProcesses.contains(regEntity.getProcess()))
            return 1;

        AdditionalInfoRequestDto additionalInfoRequestDto = additionalInfoRequestService
                .getAdditionalInfoRequestByReqId(regEntity.getAdditionalInfoReqId());
        if (additionalInfoRequestDto == null)
            throw new AdditionalInfoIdNotFoundException();

        return additionalInfoRequestDto.getAdditionalInfoIteration();
    }

}