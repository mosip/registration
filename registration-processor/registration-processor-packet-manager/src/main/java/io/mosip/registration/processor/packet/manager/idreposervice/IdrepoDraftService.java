package io.mosip.registration.processor.packet.manager.idreposervice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
    static final String NO_RECORD_FOUND_ERROR = "IDR-IDC-007";
    private static final String ID_REPO_KEY_MANAGER_ERROR = "IDR-IDS-003";
    /** Retryable ID Repo codes for draft v2 APIs only. HTTP 5xx/timeouts stay ApisResourceAccessException.
     *  IDR-IDC-001 and IDR-IDC-004 are retryable only on create/update draft v2 (not this list).
     *  IDR-IDC-007 is success only on discard. */
    static final String DEFAULT_REPROCESSABLE_ERROR_CODES =
            "IDR-IDS-004,IDR-IDS-009,IDR-IDC-019,IDR-IDC-020,IDR-IDC-021,IDR-IDS-003";
    private static final Set<String> ADDITIONAL_CREATE_OR_UPDATE_REPROCESSABLE_ERROR_CODES = Set.of("IDR-IDC-001", "IDR-IDC-004");

    @Autowired
    private ObjectMapper mapper;

    private Set<String> reprocessableErrorCodes = parseErrorCodes(DEFAULT_REPROCESSABLE_ERROR_CODES);

    @Value("${mosip.regproc.idrepo.draft.reprocessable-error-codes:" + DEFAULT_REPROCESSABLE_ERROR_CODES + "}")
    public void setReprocessableErrorCodes(String codes) {
        this.reprocessableErrorCodes = parseErrorCodes(codes);
    }

    public boolean isReprocessableError(String errorCode) {
        return errorCode != null && reprocessableErrorCodes.contains(errorCode.toUpperCase(Locale.ROOT));
    }

    /**
     * The registration processor rest client service.
     */
    @Autowired
    private RegistrationProcessorRestClientService<Object> registrationProcessorRestClientService;

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

    public ResponseDTO idrepoGetDraft(String id)
            throws ApisResourceAccessException, IdrepoDraftException, IdrepoDraftReprocessableException {
        return idrepoGetDraft(id, null);
    }

    public ResponseDTO idrepoGetDraft(String id, String type)
            throws ApisResourceAccessException, IdrepoDraftException, IdrepoDraftReprocessableException {
        regProcLogger.debug("idrepoGetDraft entry " + id + " type=" + type);
        IdResponseDTO idResponseDTO;
        if (type != null) {
            idResponseDTO = (IdResponseDTO) registrationProcessorRestClientService.getApi(
                    ApiName.IDREPOGETDRAFT, Lists.newArrayList(id), "type", type, IdResponseDTO.class);
        } else {
            idResponseDTO = (IdResponseDTO) registrationProcessorRestClientService.getApi(
                    ApiName.IDREPOGETDRAFT, Lists.newArrayList(id), Lists.emptyList(), null, IdResponseDTO.class);
        }
        throwIfIdRepoResponseIsNull(idResponseDTO, "get draft", id);
        ErrorDTO error = getFirstError(idResponseDTO.getErrors());
        if (error != null) {
            regProcLogger.error("Error occured while getting draft for id : " + id, error.toString());
            throwFailedOrReprocessable(error);
        }
        regProcLogger.debug("idrepoGetDraft exit " + id);
        return idResponseDTO.getResponse();
    }

    /**
     * @deprecated UIN Generator only — uses legacy ID Repo create (query-param UIN).
     *             Create Draft must use {@link #idrepoCreateDraftV2}.
     */
    @Deprecated
    public boolean idrepoCreateDraft(String id, String uin) throws ApisResourceAccessException, IdrepoDraftException {
        regProcLogger.debug("idrepoCreateDraft entry " + id);
        String queryParam = uin != null ? UIN : null;
        String queryParamValue = uin != null ? uin : null;

        ResponseWrapper response = (ResponseWrapper) registrationProcessorRestClientService.postApi(
                ApiName.IDREPOCREATEDRAFT, Lists.newArrayList(id), queryParam, queryParamValue, null, ResponseWrapper.class);
        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            List<ErrorDTO> error = response.getErrors();
            regProcLogger.error("Error while creating draft for id " + id);
            throw new IdrepoDraftException(error.get(0).getErrorCode(), error.get(0).getMessage());
        }
        return (response.getErrors() == null || response.getErrors().isEmpty());
    }

    /**
     * Creates an ID Repository draft using the V2 create API (JSON request body).
     * Called by {@link #idrepoUpdateDraftV2} when no draft exists for the registration id.
     *
     * @param id          registration id (RID)
     * @param uin         UIN to associate with the draft; {@code null} when UIN is not yet known (LOST)
     * @param generateUin {@code true} to request ID Repo to generate a new UIN (NEW); {@code false} when
     *                    UIN is supplied or will be stamped later (UPDATE, LOST)
     * @return {@code true} when the draft is created successfully
     * @throws ApisResourceAccessException when the ID Repo REST call fails or returns null
     * @throws IdrepoDraftException        when ID Repo returns a permanent error
     * @throws IdrepoDraftReprocessableException when ID Repo returns a retryable error
     */
    public boolean idrepoCreateDraftV2(String id, String uin, boolean generateUin)
            throws ApisResourceAccessException, IdrepoDraftException, IdrepoDraftReprocessableException {
        regProcLogger.debug("idrepoCreateDraftV2 entry " + id + " generateUin=" + generateUin);
        CreateDraftV2RequestDto requestBody = new CreateDraftV2RequestDto(uin, generateUin);
        ResponseWrapper response = (ResponseWrapper) registrationProcessorRestClientService.postApi(
                ApiName.IDREPOCREATEDRAFT, Lists.newArrayList(id), null, null, requestBody, ResponseWrapper.class);
        throwIfIdRepoResponseIsNull(response, "create draft", id);
        ErrorDTO error = getFirstError(response.getErrors());
        if (error != null) {
            regProcLogger.error("Error while creating draft v2 for id " + id);
            throwCreateOrUpdateDraftError(error);
        }
        return true;
    }

    /**
     * Stamps a resolved UIN on an existing bare LOST draft.
     * Called by Bio Dedupe after ABIS match; Create Draft creates the draft without UIN via
     * {@link #idrepoUpdateDraftV2} with {@code generateUin = false}.
     *
     * @param id  registration id (RID)
     * @param uin matched UIN to write onto the draft
     * @return {@code true} when the UIN is stamped successfully
     * @throws ApisResourceAccessException when the ID Repo REST call fails
     * @throws IdrepoDraftException        when ID Repo returns an error or a null response
     */
    public boolean idrepoUpdateDraftUin(String id, String uin)
            throws ApisResourceAccessException, IdrepoDraftException, IdrepoDraftReprocessableException {
        regProcLogger.debug("idrepoUpdateDraftUin entry " + id);
        ObjectNode uinBody = mapper.createObjectNode();
        uinBody.put("uin", uin);
        IdResponseDTO response = (IdResponseDTO) registrationProcessorRestClientService.patchApi(
                ApiName.IDREPOUPDATEDRAFTUIN, Lists.newArrayList(id), null, null, uinBody, IdResponseDTO.class);
        throwIfIdRepoResponseIsNull(response, "update draft UIN", id);
        ErrorDTO error = getFirstError(response.getErrors());
        if (error != null) {
            regProcLogger.error("Error while stamping UIN on draft for id " + id + " errorCode: " + error.getErrorCode());
            throwFailedOrReprocessable(error);
        }
        return true;
    }

    /**
     * @deprecated UIN Generator only — legacy create/update flow matching upstream develop.
     *             Create Draft must use {@link #idrepoUpdateDraftV2}.
     */
    @Deprecated
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
            JSONObject existingIdentity = mapper.readValue(mapper.writeValueAsString(responseDTO.getIdentity()), JSONObject.class);
            JSONObject newIdentity = mapper.readValue(mapper.writeValueAsString(idRequestDto.getRequest().getIdentity()), JSONObject.class);
            newIdentity.put(UIN, existingIdentity.get(UIN));
//          setting the identity to request while updating the draft.
            requestDto.setIdentity(newIdentity);
            requestDto.setRegistrationId(responseDTO.getRegistrationId());
            requestDto.setStatus(responseDTO.getStatus());
            requestDto.setUin(responseDTO.getUin());
            idRequestDto.setRequest(requestDto);
        }
        IdResponseDTO response = (IdResponseDTO) registrationProcessorRestClientService.patchApi(
                    ApiName.IDREPOUPDATEDRAFT, Lists.newArrayList(id), null, null, idRequestDto, IdResponseDTO.class);
            if (response.getErrors() != null && !response.getErrors().isEmpty()) {
                regProcLogger.info("Error while updating the drant " + id);
                regProcLogger.info(id+" Discarding the draft because of "+response.getErrors().get(0).getMessage());
                idrepoDiscardDraft(id);
                ErrorDTO error = response.getErrors().get(0);
                regProcLogger.error("Error occured while updating draft for id : " + id, error.toString());
                if (response.getErrors().get(0).getErrorCode().equalsIgnoreCase(ID_REPO_KEY_MANAGER_ERROR)) {
                    regProcLogger.error("Error occured Deleting the Draft : " + id, error.toString());
                    throw new IdrepoDraftReprocessableException(error.getErrorCode(), error.getMessage());
                } else {
                    throw new IdrepoDraftException(error.getErrorCode(), error.getMessage());
                }
        }
        regProcLogger.debug("idrepoUpdateDraft exit " + id);
        return response;
    }

    /**
     * Create Draft stage entry point — V2 create-if-absent / merge-if-present flow.
     * Creates a draft via {@link #idrepoCreateDraftV2} when none exists; otherwise merges incoming
     * identity into the existing draft before PATCHing ID Repo.
     * Pass {@code null} or {@code true} for {@code generateUin} on NEW, UPDATE, ACTIVATED, and DEACTIVATED.
     * Pass {@code false} only for LOST (bare draft; UIN is stamped later via {@link #idrepoUpdateDraftUin}).
     *
     * @param id            registration id (RID)
     * @param uin           UIN for draft creation when no draft exists; may be {@code null} for NEW/LOST
     * @param idRequestDto  identity and metadata to populate or merge into the draft
     * @param generateUin   {@code true} to generate UIN on create (NEW); {@code false} for LOST/UPDATE;
     *                      {@code null} defaults to {@code true}
     * @return ID Repo response from the draft update PATCH
     * @throws ApisResourceAccessException when an ID Repo REST call fails
     * @throws IdrepoDraftException        when ID Repo returns a permanent error
     * @throws IdrepoDraftReprocessableException when ID Repo returns a retryable error
     * @throws IOException                 when identity JSON merge fails
     */
    public IdResponseDTO idrepoUpdateDraftV2(String id, String uin, IdRequestDto idRequestDto, Boolean generateUin)
            throws ApisResourceAccessException, IdrepoDraftException, IOException, IdrepoDraftReprocessableException {
        regProcLogger.debug("idrepoUpdateDraftV2 entry " + id + " generateUin=" + generateUin);
        boolean effectiveGenerateUin = (generateUin == null) ? true : generateUin.booleanValue();
        if (!idrepoHasDraft(id)) {
            regProcLogger.info("Existing draft not found for id " + id + ". Creating new draft.");
            idrepoCreateDraftV2(id, uin, effectiveGenerateUin);
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
            requestDto.setRegistrationId(responseDTO.getRegistrationId());
            requestDto.setStatus(responseDTO.getStatus());
            requestDto.setUin(responseDTO.getUin());
            idRequestDto.setRequest(requestDto);
        }
        IdResponseDTO response = (IdResponseDTO) registrationProcessorRestClientService.patchApi(
                    ApiName.IDREPOUPDATEDRAFT, Lists.newArrayList(id), null, null, idRequestDto, IdResponseDTO.class);
        throwIfIdRepoResponseIsNull(response, "update draft", id);
        ErrorDTO error = getFirstError(response.getErrors());
        if (error != null) {
            regProcLogger.error("Error while updating draft v2 for id " + id + " errorCode: " + error.getErrorCode());
            throwCreateOrUpdateDraftError(error);
        }
        regProcLogger.debug("idrepoUpdateDraftV2 exit " + id);
        return response;
    }

    public IdResponseDTO idrepoPublishDraft(String id)
            throws ApisResourceAccessException, IdrepoDraftException, IdrepoDraftReprocessableException {
        regProcLogger.debug("idrepoPublishDraft entry " + id);
        List<String> pathsegments = new ArrayList<String>();
        pathsegments.add(id);
        IdResponseDTO response = (IdResponseDTO) registrationProcessorRestClientService.
                getApi(ApiName.IDREPOPUBLISHDRAFT, pathsegments, "", "", IdResponseDTO.class);
        throwIfIdRepoResponseIsNull(response, "publish draft", id);
        ErrorDTO error = getFirstError(response.getErrors());
        if (error != null) {
            regProcLogger.error("Error occured while publishing the Draft : " + id, error.toString());
            if (isReprocessableError(error.getErrorCode())) {
                throw new IdrepoDraftReprocessableException(error.getErrorCode(), error.getMessage());
            }
            idrepoDiscardDraft(id);
            throw new IdrepoDraftException(error.getErrorCode(), error.getMessage());
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
        throwIfIdRepoResponseIsNull(response, "discard draft", id);
        ErrorDTO error = getFirstError(response.getErrors());
        if (error != null) {
            if (NO_RECORD_FOUND_ERROR.equalsIgnoreCase(error.getErrorCode())) {
                // Discard only: nothing left to delete — already complete.
                regProcLogger.info("Draft already absent for id " + id + ". Treating discard as success.");
                return true;
            }
            regProcLogger.error("Error occured while discarding draft for id : " + id, error.toString());
            throwFailedOrReprocessable(error);
        }
        return true;
    }

    /**
     * Create/update draft v2 only: {@code IDR-IDC-001} and {@code IDR-IDC-004} are retryable.
     * Other v2 APIs treat them as FAILED via {@link #throwFailedOrReprocessable}.
     */
    private void throwCreateOrUpdateDraftError(ErrorDTO error)
            throws IdrepoDraftException, IdrepoDraftReprocessableException {
        if (error.getErrorCode() != null
                && ADDITIONAL_CREATE_OR_UPDATE_REPROCESSABLE_ERROR_CODES.contains(error.getErrorCode().toUpperCase(Locale.ROOT))) {
            throw new IdrepoDraftReprocessableException(error.getErrorCode(), error.getMessage());
        }
        throwFailedOrReprocessable(error);
    }

    private void throwFailedOrReprocessable(ErrorDTO error) throws IdrepoDraftException, IdrepoDraftReprocessableException {
        if (isReprocessableError(error.getErrorCode())) {
            throw new IdrepoDraftReprocessableException(error.getErrorCode(), error.getMessage());
        }
        throw new IdrepoDraftException(error.getErrorCode(), error.getMessage());
    }

    private void throwIfIdRepoResponseIsNull(Object response, String apiName, String registrationId)
            throws ApisResourceAccessException {
        if (response != null) {
            return;
        }
        String message = String.format(PlatformErrorMessages.RPR_CDS_IDREPO_NULL_RESPONSE.getMessage(), apiName);
        regProcLogger.error(message + " id=" + registrationId);
        throw new ApisResourceAccessException(PlatformErrorMessages.RPR_CDS_IDREPO_NULL_RESPONSE.getCode(), message);
    }

    private ErrorDTO getFirstError(List<ErrorDTO> errors) {
        if (errors == null || errors.isEmpty() || errors.get(0) == null) {
            return null;
        }
        return errors.get(0);
    }

    private Set<String> parseErrorCodes(String codes) {
        Set<String> parsed = new HashSet<>();
        if (codes == null || codes.isBlank()) {
            return parsed;
        }
        for (String code : codes.split(",")) {
            String trimmed = code.trim();
            if (!trimmed.isEmpty()) {
                parsed.add(trimmed.toUpperCase(Locale.ROOT));
            }
        }
        return parsed;
    }
}
