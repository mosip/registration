package io.mosip.registration.processor.packet.manager.idreposervice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.util.Lists;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.packet.manager.dto.IdRequestDto;
import io.mosip.registration.processor.packet.manager.dto.IdResponseDTO;
import io.mosip.registration.processor.packet.manager.dto.RequestDto;
import io.mosip.registration.processor.packet.manager.dto.ResponseDTO;
import io.mosip.registration.processor.packet.manager.exception.IdrepoDraftException;

@Service
public class IdrepoDraftService {

    private static final String UIN = "UIN";
    private static final Integer IDREPO_DRAFT_FOUND = 200;
    private static final Integer IDREPO_DRAFT_NOT_FOUND = 204;
    private static Logger regProcLogger = RegProcessorLogger.getLogger(IdrepoDraftService.class);

    @Autowired
    private ObjectMapper mapper;

    /** The registration processor rest client service. */
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

    public ResponseDTO idrepoGetDraft(String id) throws ApisResourceAccessException {
        regProcLogger.debug("idrepoGetDraft entry " + id);
        IdResponseDTO idResponseDTO = (IdResponseDTO) registrationProcessorRestClientService.getApi(
                ApiName.IDREPOGETDRAFT, Lists.newArrayList(id), Lists.emptyList(), null, IdResponseDTO.class);

        regProcLogger.debug("idrepoGetDraft exit " + id);
        return idResponseDTO.getResponse();
    }

    public boolean idrepoCreateDraft(String id, String uin) throws ApisResourceAccessException {
        regProcLogger.debug("idrepoCreateDraft entry " + id);
        String queryParam = uin != null ? UIN : null;
        String queryParamValue = uin != null ? uin : null;

        ResponseWrapper response = (ResponseWrapper) registrationProcessorRestClientService.postApi(
                ApiName.IDREPOCREATEDRAFT, Lists.newArrayList(id), queryParam, queryParamValue, null, ResponseWrapper.class);

        return  (response.getErrors() == null || response.getErrors().isEmpty());
    }

    public IdResponseDTO idrepoUpdateDraft(String id, String uin, IdRequestDto idRequestDto) throws ApisResourceAccessException, IdrepoDraftException, IOException {
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
            idRequestDto.getRequest().setIdentity(newIdentity);
            requestDto.setRegistrationId(responseDTO.getRegistrationId());
            requestDto.setStatus(responseDTO.getStatus());
            requestDto.setUin(responseDTO.getUin());
            idRequestDto.setRequest(requestDto);

        }

        IdResponseDTO response = (IdResponseDTO) registrationProcessorRestClientService.patchApi(
                ApiName.IDREPOUPDATEDRAFT, Lists.newArrayList(id), null, null, idRequestDto, IdResponseDTO.class);
        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            regProcLogger.error("Error occured while updating draft for id : " + id, response.getErrors().iterator().next().toString());
            throw new IdrepoDraftException(response.getErrors().iterator().next().getErrorCode(),
                    response.getErrors().iterator().next().getMessage());
        }

        regProcLogger.debug("idrepoUpdateDraft exit " + id);
        return response;
    }
    
    public IdResponseDTO idrepoPublishDraft(String id) throws ApisResourceAccessException, IdrepoDraftException {
    	regProcLogger.debug("idrepoPublishDraft entry " + id);
    	List<String> pathsegments=new ArrayList<String>();
		pathsegments.add(id);
		IdResponseDTO response =  (IdResponseDTO) registrationProcessorRestClientService.
				getApi(ApiName.IDREPOPUBLISHDRAFT, pathsegments, "", "", IdResponseDTO.class);	
		if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            regProcLogger.error("Error occured while updating draft for id : " + id, response.getErrors().iterator().next().toString());
            throw new IdrepoDraftException(response.getErrors().iterator().next().getErrorCode(),
                    response.getErrors().iterator().next().getMessage());
        }

        regProcLogger.debug("idrepoPublishDraft exit " + id);
        return response;
	}
}
