package io.mosip.registration.processor.packet.storage.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import io.mosip.registration.processor.core.exception.PacketManagerNonRecoverableException;
import io.mosip.registration.processor.core.packet.dto.packetmanager.TagRequestDto;
import io.mosip.registration.processor.core.packet.dto.packetmanager.TagResponseDto;
import io.mosip.registration.processor.packet.storage.exception.ObjectDoesnotExistsException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.http.RequestWrapper;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.packet.storage.dto.BiometricRequestDto;
import io.mosip.registration.processor.packet.storage.dto.DeleteTagRequestDTO;
import io.mosip.registration.processor.packet.storage.dto.DeleteTagResponseDTO;
import io.mosip.registration.processor.packet.storage.dto.Document;
import io.mosip.registration.processor.packet.storage.dto.DocumentDto;
import io.mosip.registration.processor.packet.storage.dto.FieldDto;
import io.mosip.registration.processor.packet.storage.dto.FieldDtos;
import io.mosip.registration.processor.packet.storage.dto.FieldResponseDto;
import io.mosip.registration.processor.packet.storage.dto.InfoDto;
import io.mosip.registration.processor.packet.storage.dto.InfoRequestDto;
import io.mosip.registration.processor.packet.storage.dto.InfoResponseDto;
import io.mosip.registration.processor.packet.storage.dto.UpdateTagRequestDto;
import io.mosip.registration.processor.packet.storage.dto.ValidatePacketResponse;

@Component
public class PacketManagerService extends PriorityBasedPacketManagerService {

    private static Logger regProcLogger = RegProcessorLogger.getLogger(PacketManagerService.class);
    private static final String ID = "mosip.commmons.packetmanager";
    private static final String VERSION = "v1";
    private static final String OBJECT_DOESNOT_EXISTS_ERROR_CODE = "KER-PUT-027";
    private static final List<String> PACKET_MANAGER_NON_RECOVERABLE_ERROR_CODES = Arrays.asList("KER-PUT-019");

    @Autowired
    private RegistrationProcessorRestClientService<Object> restApi;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Utilities utilities;

    @PostConstruct
    private void setObjectMapper() {
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    protected String getField(String id, String field, String source, String process) throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        FieldDto fieldDto = new FieldDto(id, field, source, process, false);

        RequestWrapper<FieldDto> request = new RequestWrapper<>();
        request.setId(ID);
        request.setVersion(VERSION);
        request.setRequesttime(DateUtils.getUTCCurrentDateTime());
        request.setRequest(fieldDto);
        ResponseWrapper<FieldResponseDto> response = (ResponseWrapper) restApi.postApi(ApiName.PACKETMANAGER_SEARCH_FIELD, "", "", request, ResponseWrapper.class);

        if (response.getErrors() != null && response.getErrors().size() > 0) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id, JsonUtils.javaObjectToJsonString(response));
            ErrorDTO errorDTO = response.getErrors().iterator().next();
            if (OBJECT_DOESNOT_EXISTS_ERROR_CODE.equalsIgnoreCase(errorDTO.getErrorCode()))
                throw new ObjectDoesnotExistsException(errorDTO.getErrorCode(), errorDTO.getMessage());
            if(PACKET_MANAGER_NON_RECOVERABLE_ERROR_CODES.contains(errorDTO.getErrorCode()))
                throw new PacketManagerNonRecoverableException(errorDTO.getErrorCode(), errorDTO.getMessage());
            throw new PacketManagerException(errorDTO.getErrorCode(), errorDTO.getMessage());
        }

        FieldResponseDto fieldResponseDto = objectMapper.readValue(JsonUtils.javaObjectToJsonString(response.getResponse()), FieldResponseDto.class);

        String responseField = fieldResponseDto.getFields().get(field);
        if (StringUtils.isNotEmpty(responseField) && responseField.equalsIgnoreCase("null"))
            responseField = null;
        return responseField;
    }

    protected Map<String, String> getFields(String id, List<String> fields, String source, String process) throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        FieldDtos fieldDto = new FieldDtos(id, fields, source, process, false);

        RequestWrapper<FieldDtos> request = new RequestWrapper<>();
        request.setId(ID);
        request.setVersion(VERSION);
        request.setRequesttime(DateUtils.getUTCCurrentDateTime());
        request.setRequest(fieldDto);
        ResponseWrapper<FieldResponseDto> response = (ResponseWrapper) restApi.postApi(ApiName.PACKETMANAGER_SEARCH_FIELDS, "", "", request, ResponseWrapper.class);

        if (response.getErrors() != null && response.getErrors().size() > 0) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id, JsonUtils.javaObjectToJsonString(response));
            ErrorDTO errorDTO = response.getErrors().iterator().next();
            if (OBJECT_DOESNOT_EXISTS_ERROR_CODE.equalsIgnoreCase(errorDTO.getErrorCode()))
                throw new ObjectDoesnotExistsException(errorDTO.getErrorCode(), errorDTO.getMessage());
            if(PACKET_MANAGER_NON_RECOVERABLE_ERROR_CODES.contains(errorDTO.getErrorCode()))
                throw new PacketManagerNonRecoverableException(errorDTO.getErrorCode(), errorDTO.getMessage());
            throw new PacketManagerException(errorDTO.getErrorCode(), errorDTO.getMessage());
        }

        FieldResponseDto fieldResponseDto = objectMapper.readValue(JsonUtils.javaObjectToJsonString(response.getResponse()), FieldResponseDto.class);

        return fieldResponseDto.getFields();
    }

    protected Document getDocument(String id, String documentName, String process) throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        return getDocument(id, documentName, null, process);
    }

    protected Document getDocument(String id, String documentName, String source, String process) throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        DocumentDto fieldDto = new DocumentDto(id, documentName, source, process);

        RequestWrapper<DocumentDto> request = new RequestWrapper<>();
        request.setId(ID);
        request.setVersion(VERSION);
        request.setRequesttime(DateUtils.getUTCCurrentDateTime());
        request.setRequest(fieldDto);
        ResponseWrapper<Document> response = (ResponseWrapper) restApi.postApi(ApiName.PACKETMANAGER_SEARCH_DOCUMENT, "", "", request, ResponseWrapper.class);

        if (response.getErrors() != null && response.getErrors().size() > 0) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id, JsonUtils.javaObjectToJsonString(response));
            ErrorDTO errorDTO = response.getErrors().iterator().next();
            if (OBJECT_DOESNOT_EXISTS_ERROR_CODE.equalsIgnoreCase(errorDTO.getErrorCode()))
                throw new ObjectDoesnotExistsException(errorDTO.getErrorCode(), errorDTO.getMessage());
            if(PACKET_MANAGER_NON_RECOVERABLE_ERROR_CODES.contains(errorDTO.getErrorCode()))
                throw new PacketManagerNonRecoverableException(errorDTO.getErrorCode(), errorDTO.getMessage());
            throw new PacketManagerException(errorDTO.getErrorCode(), errorDTO.getMessage());
        }

        Document document = objectMapper.readValue(JsonUtils.javaObjectToJsonString(response.getResponse()), Document.class);

        return document;
    }

    protected ValidatePacketResponse validate(String id, String source, String process) throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        InfoDto fieldDto = new InfoDto(id, source, process, false);

        RequestWrapper<InfoDto> request = new RequestWrapper<>();
        request.setId(ID);
        request.setVersion(VERSION);
        request.setRequesttime(DateUtils.getUTCCurrentDateTime());
        request.setRequest(fieldDto);
        ResponseWrapper<ValidatePacketResponse> response = (ResponseWrapper) restApi.postApi(ApiName.PACKETMANAGER_VALIDATE, "", "", request, ResponseWrapper.class);

        if (response.getErrors() != null && response.getErrors().size() > 0) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id, JsonUtils.javaObjectToJsonString(response));
            ErrorDTO errorDTO = response.getErrors().iterator().next();
            if (OBJECT_DOESNOT_EXISTS_ERROR_CODE.equalsIgnoreCase(errorDTO.getErrorCode()))
                throw new ObjectDoesnotExistsException(errorDTO.getErrorCode(), errorDTO.getMessage());
            if(PACKET_MANAGER_NON_RECOVERABLE_ERROR_CODES.contains(errorDTO.getErrorCode()))
                throw new PacketManagerNonRecoverableException(errorDTO.getErrorCode(), errorDTO.getMessage());
            throw new PacketManagerException(errorDTO.getErrorCode(), errorDTO.getMessage());
        }
        ValidatePacketResponse validatePacketResponse = objectMapper.readValue(JsonUtils.javaObjectToJsonString(response.getResponse()), ValidatePacketResponse.class);

        return validatePacketResponse;
    }

    protected List<FieldResponseDto> getAudits(String id, String source, String process) throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {

        InfoDto fieldDto = new InfoDto(id, source, process, false);
        List<FieldResponseDto> response = new ArrayList<>();

        RequestWrapper<InfoDto> request = new RequestWrapper<>();
        request.setId(ID);
        request.setVersion(VERSION);
        request.setRequesttime(DateUtils.getUTCCurrentDateTime());
        request.setRequest(fieldDto);
        ResponseWrapper<List<FieldResponseDto>> responseObj = (ResponseWrapper) restApi.postApi(ApiName.PACKETMANAGER_SEARCH_AUDITS, "", "", request, ResponseWrapper.class);

        if (responseObj.getErrors() != null && responseObj.getErrors().size() > 0) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id, JsonUtils.javaObjectToJsonString(responseObj));
            ErrorDTO errorDTO = responseObj.getErrors().iterator().next();
            if (OBJECT_DOESNOT_EXISTS_ERROR_CODE.equalsIgnoreCase(errorDTO.getErrorCode()))
                throw new ObjectDoesnotExistsException(errorDTO.getErrorCode(), errorDTO.getMessage());
            if(PACKET_MANAGER_NON_RECOVERABLE_ERROR_CODES.contains(errorDTO.getErrorCode()))
                throw new PacketManagerNonRecoverableException(errorDTO.getErrorCode(), errorDTO.getMessage());
            throw new PacketManagerException(errorDTO.getErrorCode(), errorDTO.getMessage());
        }

        for (Object o : responseObj.getResponse()) {
            FieldResponseDto fieldResponseDto = objectMapper.readValue(JsonUtils.javaObjectToJsonString(o), FieldResponseDto.class);
            response.add(fieldResponseDto);
        }

        return response;
    }

    protected BiometricRecord getBiometrics(String id, String person, List<String> modalities, String source, String process) throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {

        BiometricRequestDto fieldDto = new BiometricRequestDto(id, person, modalities, source, process, false);

        RequestWrapper<BiometricRequestDto> request = new RequestWrapper<>();
        request.setId(ID);
        request.setVersion(VERSION);
        request.setRequesttime(DateUtils.getUTCCurrentDateTime());
        request.setRequest(fieldDto);
        ResponseWrapper<BiometricRecord> response = (ResponseWrapper) restApi.postApi(ApiName.PACKETMANAGER_SEARCH_BIOMETRICS, "", "", request, ResponseWrapper.class);

        if (response.getErrors() != null && response.getErrors().size() > 0) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id, JsonUtils.javaObjectToJsonString(response));
            ErrorDTO errorDTO = response.getErrors().iterator().next();
            if (OBJECT_DOESNOT_EXISTS_ERROR_CODE.equalsIgnoreCase(errorDTO.getErrorCode()))
                throw new ObjectDoesnotExistsException(errorDTO.getErrorCode(), errorDTO.getMessage());
            if(PACKET_MANAGER_NON_RECOVERABLE_ERROR_CODES.contains(errorDTO.getErrorCode()))
                throw new PacketManagerNonRecoverableException(errorDTO.getErrorCode(), errorDTO.getMessage());
            throw new PacketManagerException(errorDTO.getErrorCode(), errorDTO.getMessage());
        }
        if (response.getResponse() != null) {
            BiometricRecord biometricRecord = objectMapper.readValue(JsonUtils.javaObjectToJsonString(response.getResponse()), BiometricRecord.class);
            return biometricRecord;
        }
        return null;

    }

    protected Map<String, String> getMetaInfo(String id, String source, String process) throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        InfoDto fieldDto = new InfoDto(id, source, process, false);

        RequestWrapper<InfoDto> request = new RequestWrapper<>();
        request.setId(ID);
        request.setVersion(VERSION);
        request.setRequesttime(DateUtils.getUTCCurrentDateTime());
        request.setRequest(fieldDto);
        ResponseWrapper<FieldResponseDto> response = (ResponseWrapper) restApi.postApi(ApiName.PACKETMANAGER_SEARCH_METAINFO, "", "", request, ResponseWrapper.class);

        if (CollectionUtils.isNotEmpty(response.getErrors())) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                    LoggerFileConstant.REGISTRATIONID.toString(), id, JsonUtils.javaObjectToJsonString(response));
            ErrorDTO errorDTO = response.getErrors().iterator().next();
            if (OBJECT_DOESNOT_EXISTS_ERROR_CODE.equalsIgnoreCase(errorDTO.getErrorCode()))
                throw new ObjectDoesnotExistsException(errorDTO.getErrorCode(), errorDTO.getMessage());
            if(PACKET_MANAGER_NON_RECOVERABLE_ERROR_CODES.contains(errorDTO.getErrorCode()))
                throw new PacketManagerNonRecoverableException(errorDTO.getErrorCode(), errorDTO.getMessage());
            throw new PacketManagerException(errorDTO.getErrorCode(), errorDTO.getMessage());
        }

        FieldResponseDto fieldResponseDto = objectMapper.readValue(JsonUtils.javaObjectToJsonString(response.getResponse()), FieldResponseDto.class);

        return fieldResponseDto.getFields();
    }

    protected InfoResponseDto info(String id) throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        InfoRequestDto infoRequestDto = new InfoRequestDto(id);

        RequestWrapper<InfoRequestDto> request = new RequestWrapper<>();
        request.setId(ID);
        request.setVersion(VERSION);
        request.setRequesttime(DateUtils.getUTCCurrentDateTime());
        request.setRequest(infoRequestDto);
        ResponseWrapper<InfoResponseDto> response = (ResponseWrapper) restApi.postApi(ApiName.PACKETMANAGER_INFO, "", "", request, ResponseWrapper.class);

        if (response.getErrors() != null && response.getErrors().size() > 0) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id, JsonUtils.javaObjectToJsonString(response));
            ErrorDTO errorDTO = response.getErrors().iterator().next();
            if (OBJECT_DOESNOT_EXISTS_ERROR_CODE.equalsIgnoreCase(errorDTO.getErrorCode()))
                throw new ObjectDoesnotExistsException(errorDTO.getErrorCode(), errorDTO.getMessage());
            if(PACKET_MANAGER_NON_RECOVERABLE_ERROR_CODES.contains(errorDTO.getErrorCode()))
                throw new PacketManagerNonRecoverableException(errorDTO.getErrorCode(), errorDTO.getMessage());
            throw new PacketManagerException(errorDTO.getErrorCode(), errorDTO.getMessage());
        }

        InfoResponseDto infoResponseDto = objectMapper.readValue(JsonUtils.javaObjectToJsonString(response.getResponse()), InfoResponseDto.class);

        return infoResponseDto;
    }

    public void addOrUpdateTags(String id, Map<String, String> tags) throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        UpdateTagRequestDto updateTagRequestDto = new UpdateTagRequestDto(id, tags);

        RequestWrapper<UpdateTagRequestDto> request = new RequestWrapper<>();
        request.setId(ID);
        request.setVersion(VERSION);
        request.setRequesttime(DateUtils.getUTCCurrentDateTime());
        request.setRequest(updateTagRequestDto);
        ResponseWrapper<Void> response = (ResponseWrapper) restApi.postApi(ApiName.PACKETMANAGER_UPDATE_TAGS, "", "", request, ResponseWrapper.class);

        if (response.getErrors() != null && response.getErrors().size() > 0) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id, JsonUtils.javaObjectToJsonString(response));
            ErrorDTO errorDTO = response.getErrors().iterator().next();
            if (OBJECT_DOESNOT_EXISTS_ERROR_CODE.equalsIgnoreCase(errorDTO.getErrorCode()))
                throw new ObjectDoesnotExistsException(errorDTO.getErrorCode(), errorDTO.getMessage());
            if(PACKET_MANAGER_NON_RECOVERABLE_ERROR_CODES.contains(errorDTO.getErrorCode()))
                throw new PacketManagerNonRecoverableException(errorDTO.getErrorCode(), errorDTO.getMessage());
            throw new PacketManagerException(errorDTO.getErrorCode(), errorDTO.getMessage());
        }
    }

	@SuppressWarnings("unchecked")
	public void deleteTags(String id, List<String> tags)
			throws ApisResourceAccessException, PacketManagerException, JsonProcessingException {
		DeleteTagRequestDTO deleteTagREquestDto = new DeleteTagRequestDTO(id, tags);
		RequestWrapper<DeleteTagRequestDTO> request = new RequestWrapper<>();
		request.setId(ID);
		request.setVersion(VERSION);
		request.setRequesttime(DateUtils.getUTCCurrentDateTime());
		request.setRequest(deleteTagREquestDto);
		ResponseWrapper<DeleteTagResponseDTO> response = (ResponseWrapper<DeleteTagResponseDTO>) restApi
				.postApi(ApiName.PACKETMANAGER_DELETE_TAGS, "", "",
				request, ResponseWrapper.class);

		if (response.getErrors() != null && response.getErrors().size() > 0) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					id, JsonUtils.javaObjectToJsonString(response));
            ErrorDTO errorDTO = response.getErrors().iterator().next();
            if (OBJECT_DOESNOT_EXISTS_ERROR_CODE.equalsIgnoreCase(errorDTO.getErrorCode()))
                throw new ObjectDoesnotExistsException(errorDTO.getErrorCode(), errorDTO.getMessage());
            if(PACKET_MANAGER_NON_RECOVERABLE_ERROR_CODES.contains(errorDTO.getErrorCode()))
                throw new PacketManagerNonRecoverableException(errorDTO.getErrorCode(), errorDTO.getMessage());
            throw new PacketManagerException(errorDTO.getErrorCode(), errorDTO.getMessage());
		}


	}

    public Map<String, String> getAllTags(String id) throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        return getTags(id, null);
    }

    public Map<String, String> getTags(String id, List<String> tagNames) throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        TagRequestDto tagRequestDto = new TagRequestDto(id, tagNames);
        RequestWrapper<TagRequestDto> request = new RequestWrapper<>();
        request.setId(ID);
        request.setVersion(VERSION);
        request.setRequesttime(DateUtils.getUTCCurrentDateTime());
        request.setRequest(tagRequestDto);
        ResponseWrapper<TagResponseDto> response = (ResponseWrapper<TagResponseDto>) restApi
                .postApi(ApiName.PACKETMANAGER_GET_TAGS, "", "",
                        request, ResponseWrapper.class);

        if (response.getErrors() != null && response.getErrors().size() > 0) {
        	ErrorDTO error=response.getErrors().get(0);
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    id, JsonUtils.javaObjectToJsonString(response));
            //This error code will return if requested tag is not present ,so returning null for that
            if(error.getErrorCode().equalsIgnoreCase("KER-PUT-024")) 
        		return null;
            else {
                ErrorDTO errorDTO = response.getErrors().iterator().next();
                if (OBJECT_DOESNOT_EXISTS_ERROR_CODE.equalsIgnoreCase(errorDTO.getErrorCode()))
                    throw new ObjectDoesnotExistsException(errorDTO.getErrorCode(), errorDTO.getMessage());
                if(PACKET_MANAGER_NON_RECOVERABLE_ERROR_CODES.contains(errorDTO.getErrorCode()))
                    throw new PacketManagerNonRecoverableException(errorDTO.getErrorCode(), errorDTO.getMessage());
                throw new PacketManagerException(errorDTO.getErrorCode(), errorDTO.getMessage());
            }
        }

        TagResponseDto tagResponseDto = null;
        if (response.getResponse() != null) {
            tagResponseDto = objectMapper.readValue(JsonUtils.javaObjectToJsonString(response.getResponse()), TagResponseDto.class);

        }

        return tagResponseDto != null ? tagResponseDto.getTags() : null;
    }
}
