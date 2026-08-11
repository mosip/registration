package io.mosip.registration.processor.packet.manager.idreposervice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.assertj.core.util.Lists;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.packet.manager.dto.CreateDraftV2RequestDto;
import io.mosip.registration.processor.packet.manager.dto.IdRequestDto;
import io.mosip.registration.processor.packet.manager.dto.IdResponseDTO;
import io.mosip.registration.processor.packet.manager.dto.RequestDto;
import io.mosip.registration.processor.packet.manager.dto.ResponseDTO;
import io.mosip.registration.processor.packet.manager.exception.IdrepoDraftException;
import io.mosip.registration.processor.packet.manager.exception.IdrepoDraftReprocessableException;

@Service
public class IdrepoDraftService {

    private static final String UIN = "UIN";
    private static final Integer IDREPO_DRAFT_FOUND = 200;
    private static final Integer IDREPO_DRAFT_NOT_FOUND = 204;
    private static Logger regProcLogger = RegProcessorLogger.getLogger(IdrepoDraftService.class);
    private static final String ID_REPO_KEY_MANAGER_ERROR = "IDR-IDS-003";
    private static final String DRAFT_UIN_DETAILS_NOT_FOUND = "IDR-IDC-015";
    private static final String RECORD_ALREADY_EXISTS_ERROR = "IDR-IDC-012";

    @Autowired
    private ObjectMapper mapper;

    /**
     * The registration processor rest client service.
     */
    @Autowired
    private RegistrationProcessorRestClientService<Object> registrationProcessorRestClientService;

    @Value("${mosip.registration.processor.idrepo.uin.stamp.max-retry:3}")
    private int uinStampMaxRetry;

    @Value("${mosip.registration.processor.idrepo.uin.stamp.retry-delay-ms:1000}")
    private long uinStampRetryDelayMs;

    public boolean idrepoHasDraft(String id) throws ApisResourceAccessException, IdrepoDraftException {
        regProcLogger.debug("idrepoHasDraft entry " + id);

        Integer result = registrationProcessorRestClientService.headApi
                (ApiName.IDREPOHASDRAFT, Lists.newArrayList(id), null, null);

        if (result == null || (result.intValue() != IDREPO_DRAFT_FOUND && result.intValue() != IDREPO_DRAFT_NOT_FOUND)) {
            regProcLogger.error("idrepoHasDraft failed to get result for id " + id + " result received is " + result);
            throw new IdrepoDraftException(PlatformErrorMessages.DRAFT_CHECK_FAILED.getCode(), PlatformErrorMessages.DRAFT_CHECK_FAILED.getMessage());
        }

        boolean hasDraft = result != null && result.intValue() == IDREPO_DRAFT_FOUND;
        regProcLogger.info("idrepoHasDraft result for id " + id + " is " + hasDraft);
        return hasDraft;
    }

    public ResponseDTO idrepoGetDraft(String id) throws ApisResourceAccessException, IdrepoDraftException {
        return idrepoGetDraft(id, null);
    }

    public ResponseDTO idrepoGetDraft(String id, String type) throws ApisResourceAccessException, IdrepoDraftException {
        regProcLogger.debug("idrepoGetDraft entry " + id + " type=" + type);
        IdResponseDTO idResponseDTO;
        if (type != null) {
            idResponseDTO = (IdResponseDTO) registrationProcessorRestClientService.getApi(
                    ApiName.IDREPOGETDRAFT, Lists.newArrayList(id), "type", type, IdResponseDTO.class);
        } else {
            idResponseDTO = (IdResponseDTO) registrationProcessorRestClientService.getApi(
                    ApiName.IDREPOGETDRAFT, Lists.newArrayList(id), Lists.emptyList(), null, IdResponseDTO.class);
        }
        if (idResponseDTO.getErrors() != null && !idResponseDTO.getErrors().isEmpty()) {
            ErrorDTO error = idResponseDTO.getErrors().get(0);
            regProcLogger.error("Error occured while getting draft for id : " + id, error.toString());
            throw new IdrepoDraftException(error.getErrorCode(), error.getMessage());
        }
        regProcLogger.debug("idrepoGetDraft exit " + id);
        return idResponseDTO.getResponse();
    }


    public boolean idrepoCreateDraftV2(String id, String uin, boolean generateUin)
            throws ApisResourceAccessException, IdrepoDraftException {
        regProcLogger.debug("idrepoCreateDraftV2 entry " + id + " generateUin=" + generateUin);
        CreateDraftV2RequestDto requestBody = new CreateDraftV2RequestDto(uin, generateUin);
        ResponseWrapper response = (ResponseWrapper) registrationProcessorRestClientService.postApi(
                ApiName.IDREPOCREATEDRAFT, Lists.newArrayList(id), null, null, requestBody, ResponseWrapper.class);
        if (response == null) {
            regProcLogger.error("Null response from idrepoCreateDraftV2 for id " + id);
            throw new IdrepoDraftException(
                PlatformErrorMessages.RPR_CDS_DRAFT_CREATION_FAILED.getCode(),
                "Null response from ID Repository for draft create V2 for id: " + id);
        }
        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            List<ErrorDTO> error = response.getErrors();
            regProcLogger.error("Error while creating draft v2 for id " + id);
            throw new IdrepoDraftException(error.get(0).getErrorCode(), error.get(0).getMessage());
        }
        return true;
    }

    public boolean idrepoUpdateDraftUin(String id, String uin) throws ApisResourceAccessException, IdrepoDraftException, IdrepoDraftReprocessableException {
        regProcLogger.debug("idrepoUpdateDraftUin entry " + id);
        ObjectNode uinBody = mapper.createObjectNode();
        uinBody.put("uin", uin);
        int attempt = 0;
        while (true) {
            IdResponseDTO response = (IdResponseDTO) registrationProcessorRestClientService.patchApi(
                    ApiName.IDREPOUPDATEDRAFTUIN, Lists.newArrayList(id), null, null, uinBody, IdResponseDTO.class);
            if (response.getErrors() == null || response.getErrors().isEmpty()) {
                return true;
            }
            ErrorDTO error = response.getErrors().get(0);
            regProcLogger.error("Error while stamping UIN on draft for id " + id + " errorCode: " + error.getErrorCode());
            if (error.getErrorCode().equalsIgnoreCase(ID_REPO_KEY_MANAGER_ERROR)) {
                attempt++;
                if (attempt >= uinStampMaxRetry) {
                    regProcLogger.error("idrepoUpdateDraftUin: Key Manager error persists after " + uinStampMaxRetry + " attempt(s) for id " + id);
                    throw new IdrepoDraftReprocessableException(error.getErrorCode(), error.getMessage());
                }
                regProcLogger.warn("idrepoUpdateDraftUin: Key Manager error on attempt " + attempt + "/" + uinStampMaxRetry + " for id " + id + ", retrying after " + uinStampRetryDelayMs + "ms");
                try {
                    Thread.sleep(uinStampRetryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IdrepoDraftReprocessableException(error.getErrorCode(), error.getMessage());
                }
            } else {
                throw new IdrepoDraftException(error.getErrorCode(), error.getMessage());
            }
        }
    }

    public boolean idrepoCreateDraft(String id, String uin) throws ApisResourceAccessException, IdrepoDraftException {
        regProcLogger.debug("idrepoCreateDraft entry " + id);
        String queryParam = uin != null ? UIN : null;
        String queryParamValue = uin != null ? uin : null;

        ResponseWrapper response = (ResponseWrapper) registrationProcessorRestClientService.postApi(
                ApiName.IDREPOCREATEDRAFT, Lists.newArrayList(id), queryParam, queryParamValue, null, ResponseWrapper.class);
        if (response.getErrors() != null && !response.getErrors().isEmpty())
        {
            List<ErrorDTO> error=response.getErrors();
            regProcLogger.error("Error while creating draft for id " + id);
            throw new IdrepoDraftException(error.get(0).getErrorCode(), error.get(0).getMessage());
        }
        return (response.getErrors() == null || response.getErrors().isEmpty());
    }

    public IdResponseDTO idrepoUpdateDraft(String id, String uin, IdRequestDto idRequestDto)
            throws ApisResourceAccessException, IdrepoDraftException, IOException, IdrepoDraftReprocessableException {
        regProcLogger.debug("idrepoUpdateDraft entry " + id);
        if (!idrepoHasDraft(id)) {
            regProcLogger.info("Existing draft not found for id " + id + ". Creating new draft.");
            idrepoCreateDraft(id, uin);
        } else {
            regProcLogger.info("Existing draft found for id " + id + ". Updating uin in demographic identity.");
            ResponseDTO responseDTO = idrepoGetDraft(id);
            RequestDto requestDto = new RequestDto();
            requestDto.setAnonymousProfile(responseDTO.getAnonymousProfile());
            requestDto.setBiometricReferenceId(responseDTO.getBiometricReferenceId());
            // For LOST drafts, identity may be null before UIN is stamped — handle gracefully.
            JSONObject existingIdentity = responseDTO.getIdentity() != null
                    ? mapper.readValue(mapper.writeValueAsString(responseDTO.getIdentity()), JSONObject.class)
                    : new JSONObject();
            Object incomingIdentityRaw = idRequestDto.getRequest().getIdentity();
            JSONObject newIdentity = incomingIdentityRaw != null
                    ? mapper.readValue(mapper.writeValueAsString(incomingIdentityRaw), JSONObject.class)
                    : new JSONObject();
            Object existingUin = existingIdentity.get(UIN);
            if (existingUin != null) {
                newIdentity.put(UIN, existingUin);
            }
//          setting the identity to request while updating the draft.
            requestDto.setIdentity(newIdentity);
            // Documents (biometrics) must be carried over from the incoming request so the draft retains biometric data alongside updated demographics.
            requestDto.setDocuments(idRequestDto.getRequest().getDocuments());
            requestDto.setRegistrationId(responseDTO.getRegistrationId());
            requestDto.setStatus(responseDTO.getStatus());
            requestDto.setUin(responseDTO.getUin());
            idRequestDto.setRequest(requestDto);
        }
        IdResponseDTO response = (IdResponseDTO) registrationProcessorRestClientService.patchApi(
                    ApiName.IDREPOUPDATEDRAFT, Lists.newArrayList(id), null, null, idRequestDto, IdResponseDTO.class);
            if (response.getErrors() != null && !response.getErrors().isEmpty()) {
                ErrorDTO firstError = response.getErrors().get(0);
                if (firstError.getErrorCode().equalsIgnoreCase(RECORD_ALREADY_EXISTS_ERROR)) {
                    regProcLogger.info("Record already exists for id " + id + " — treating as success for idempotent reprocess.");
                    return response;
                }
                regProcLogger.info("Error while updating the drant " + id);
                regProcLogger.info(id+" Discarding the draft because of "+firstError.getMessage());
                idrepoDiscardDraft(id);
                regProcLogger.error("Error occured while updating draft for id : " + id, firstError.toString());
                if (firstError.getErrorCode().equalsIgnoreCase(ID_REPO_KEY_MANAGER_ERROR)) {
                    regProcLogger.error("Error occured Deleting the Draft : " + id, firstError.toString());
                    throw new IdrepoDraftReprocessableException(firstError.getErrorCode(), firstError.getMessage());
                } else {
                    throw new IdrepoDraftException(firstError.getErrorCode(), firstError.getMessage());
                }
        }
        regProcLogger.debug("idrepoUpdateDraft exit " + id);
        return response;
    }

    public IdResponseDTO idrepoPublishDraft(String id)
            throws ApisResourceAccessException, IdrepoDraftException, IdrepoDraftReprocessableException {
        regProcLogger.debug("idrepoPublishDraft entry " + id);
        List<String> pathsegments = new ArrayList<String>();
        pathsegments.add(id);
        IdResponseDTO response = (IdResponseDTO) registrationProcessorRestClientService.
                getApi(ApiName.IDREPOPUBLISHDRAFT, pathsegments, "", "", IdResponseDTO.class);

        if(response.getErrors()!=null && !response.getErrors().isEmpty())
        {
            ErrorDTO error=response.getErrors().get(0);
            regProcLogger.error("Error occured while publishing the Draft : " + id, error.toString());
            if (error.getErrorCode().equalsIgnoreCase(ID_REPO_KEY_MANAGER_ERROR)) {
                throw new IdrepoDraftReprocessableException(error.getErrorCode(), error.getMessage());
            } else if (error.getErrorCode().equalsIgnoreCase(DRAFT_UIN_DETAILS_NOT_FOUND)) {
                regProcLogger.error("UIN details not found in draft, cannot publish : " + id);
                throw new IdrepoDraftException(error.getErrorCode(), error.getMessage());
            } else {
                idrepoDiscardDraft(id);
                throw new IdrepoDraftException(error.getErrorCode(), error.getMessage());
            }
        }
        regProcLogger.debug("idrepoPublishDraft exit " + id);
        return response;
    }

    public Boolean idrepoDiscardDraft(String id) throws ApisResourceAccessException, IdrepoDraftReprocessableException, IdrepoDraftException {
        regProcLogger.debug("idrepoDiscardDraft entry " + id);
        List<String> pathsegments = new ArrayList<String>();
        pathsegments.add(id);
        IdResponseDTO response = (IdResponseDTO) registrationProcessorRestClientService.
                deleteApi(ApiName.IDREPODISCARDDRAFT, pathsegments, "", "", IdResponseDTO.class);
        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            ErrorDTO error = response.getErrors().get(0);
            regProcLogger.error("Error occured while discarding draft for id : " + id, error.toString());
            if (response.getErrors().get(0).getErrorCode().equalsIgnoreCase(ID_REPO_KEY_MANAGER_ERROR)) {
                throw new IdrepoDraftReprocessableException(error.getErrorCode(), error.getMessage());
            } else {
                throw new IdrepoDraftException(error.getErrorCode(), error.getMessage());
            }
        }
        return true;
    }
}
