package io.mosip.registration.processor.packet.storage.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.beans.factory.annotation.Qualifier;

import com.fasterxml.jackson.databind.SerializationFeature;
import io.mosip.registration.processor.core.exception.PacketManagerNonRecoverableException;
import io.mosip.registration.processor.core.packet.dto.packetmanager.TagRequestDto;
import io.mosip.registration.processor.core.packet.dto.packetmanager.TagResponseDto;
import io.mosip.registration.processor.packet.storage.exception.ObjectDoesnotExistsException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils2;
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
public class PacketManagerService {

    private static Logger regProcLogger = RegProcessorLogger.getLogger(PacketManagerService.class);
    private static final String ID = "mosip.commmons.packetmanager";
    private static final String VERSION = "v1";
    private static final String OBJECT_DOESNOT_EXISTS_ERROR_CODE = "KER-PUT-027";
    private static final List<String> PACKET_MANAGER_NON_RECOVERABLE_ERROR_CODES = Arrays.asList("KER-PUT-019");
    private static final long REQUEST_TIMEOUT_MS = 30000;
    private static final String SESSION_ID = LoggerFileConstant.SESSIONID.toString();
    private static final String APPLICATION_ID = LoggerFileConstant.APPLICATIONID.toString();
    private static final String REGISTRATION_ID = LoggerFileConstant.REGISTRATIONID.toString();

    // Cache for Info API responses (per packet ID) - improves performance for repeated info() calls
    private final Map<String, InfoResponseDto> infoCache = new ConcurrentHashMap<>();
    private static final long INFO_CACHE_TTL_MS = 5000; // 5 second TTL

    @Autowired
    @Qualifier("selfTokenWebClient")
    private WebClient webClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Environment env;

    @PostConstruct
    private void setObjectMapper() {
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Optimized WebClient POST API with minimal changes
     */
    private <T> T postApi(ApiName apiName, Object requestObject, Class<T> responseClass) throws ApisResourceAccessException {
        try {
            String uri = env.getProperty(apiName.toString());
            regProcLogger.info(SESSION_ID, APPLICATION_ID, APPLICATION_ID, uri);
            if (uri!=null){
                return webClient.post()
                        .uri(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestObject)
                        .retrieve()
                        .bodyToMono(responseClass)
                        .timeout(Duration.ofMillis(REQUEST_TIMEOUT_MS))
                        .block();
            }
        } catch (WebClientResponseException e) {
            regProcLogger.error(SESSION_ID, APPLICATION_ID, APPLICATION_ID,
                    e.getMessage() + ExceptionUtils.getStackTrace(e));
            throw new ApisResourceAccessException(e.getMessage(), e);
        } catch (Exception e) {
            regProcLogger.error(SESSION_ID, APPLICATION_ID, APPLICATION_ID,
                    e.getMessage() + ExceptionUtils.getStackTrace(e));
            throw e;
        }
        return null;
    }

    /**
     * Optimized error handling
     */
    private void handleErrorResponse(ResponseWrapper<?> response, String id)
            throws ApisResourceAccessException, PacketManagerException, ObjectDoesnotExistsException,
            PacketManagerNonRecoverableException, JsonProcessingException {
        if (response.getErrors() != null && response.getErrors().size() > 0) {
            regProcLogger.error(SESSION_ID, REGISTRATION_ID, id, JsonUtils.javaObjectToJsonString(response));
            ErrorDTO errorDTO = response.getErrors().iterator().next();
            if (OBJECT_DOESNOT_EXISTS_ERROR_CODE.equalsIgnoreCase(errorDTO.getErrorCode()))
                throw new ObjectDoesnotExistsException(errorDTO.getErrorCode(), errorDTO.getMessage());
            if (PACKET_MANAGER_NON_RECOVERABLE_ERROR_CODES.contains(errorDTO.getErrorCode()))
                throw new PacketManagerNonRecoverableException(errorDTO.getErrorCode(), errorDTO.getMessage());
            throw new PacketManagerException(errorDTO.getErrorCode(), errorDTO.getMessage());
        }
    }

    public String getField(String id, String field, String source, String process)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        FieldDto fieldDto = new FieldDto(id, field, source, process, false);

        RequestWrapper<FieldDto> request = new RequestWrapper<>();
        request.setId(ID);
        request.setVersion(VERSION);
        request.setRequesttime(DateUtils2.getUTCCurrentDateTime());
        request.setRequest(fieldDto);
        ResponseWrapper<FieldResponseDto> response = postApi(ApiName.PACKETMANAGER_SEARCH_FIELD, request, ResponseWrapper.class);

        handleErrorResponse(response, id);

        FieldResponseDto fieldResponseDto = objectMapper.readValue(JsonUtils.javaObjectToJsonString(response.getResponse()), FieldResponseDto.class);

        String responseField = fieldResponseDto.getFields().get(field);
        if (StringUtils.isNotEmpty(responseField) && responseField.equalsIgnoreCase("null"))
            responseField = null;
        return responseField;
    }

    public Map<String, String> getFields(String id, List<String> fields, String source, String process)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        FieldDtos fieldDto = new FieldDtos(id, fields, source, process, false);

        RequestWrapper<FieldDtos> request = new RequestWrapper<>();
        request.setId(ID);
        request.setVersion(VERSION);
        request.setRequesttime(DateUtils2.getUTCCurrentDateTime());
        request.setRequest(fieldDto);
        ResponseWrapper<FieldResponseDto> response = postApi(ApiName.PACKETMANAGER_SEARCH_FIELDS, request, ResponseWrapper.class);

        handleErrorResponse(response, id);

        FieldResponseDto fieldResponseDto = objectMapper.readValue(JsonUtils.javaObjectToJsonString(response.getResponse()), FieldResponseDto.class);

        return fieldResponseDto.getFields();
    }

    public Document getDocument(String id, String documentName, String process)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        return getDocument(id, documentName, null, process);
    }

    public Document getDocument(String id, String documentName, String source, String process)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        DocumentDto fieldDto = new DocumentDto(id, documentName, source, process);

        RequestWrapper<DocumentDto> request = new RequestWrapper<>();
        request.setId(ID);
        request.setVersion(VERSION);
        request.setRequesttime(DateUtils2.getUTCCurrentDateTime());
        request.setRequest(fieldDto);
        ResponseWrapper<Document> response = postApi(ApiName.PACKETMANAGER_SEARCH_DOCUMENT, request, ResponseWrapper.class);

        handleErrorResponse(response, id);

        Document document = objectMapper.readValue(JsonUtils.javaObjectToJsonString(response.getResponse()), Document.class);

        return document;
    }

    public ValidatePacketResponse validate(String id, String source, String process)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        InfoDto fieldDto = new InfoDto(id, source, process, false);

        RequestWrapper<InfoDto> request = new RequestWrapper<>();
        request.setId(ID);
        request.setVersion(VERSION);
        request.setRequesttime(DateUtils2.getUTCCurrentDateTime());
        request.setRequest(fieldDto);
        ResponseWrapper<ValidatePacketResponse> response = postApi(ApiName.PACKETMANAGER_VALIDATE, request, ResponseWrapper.class);

        handleErrorResponse(response, id);

        ValidatePacketResponse validatePacketResponse = objectMapper.readValue(JsonUtils.javaObjectToJsonString(response.getResponse()), ValidatePacketResponse.class);

        return validatePacketResponse;
    }

    public List<FieldResponseDto> getAudits(String id, String source, String process)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {

        InfoDto fieldDto = new InfoDto(id, source, process, false);
        List<FieldResponseDto> response = new ArrayList<>();

        RequestWrapper<InfoDto> request = new RequestWrapper<>();
        request.setId(ID);
        request.setVersion(VERSION);
        request.setRequesttime(DateUtils2.getUTCCurrentDateTime());
        request.setRequest(fieldDto);
        ResponseWrapper<List<FieldResponseDto>> responseObj = postApi(ApiName.PACKETMANAGER_SEARCH_AUDITS, request, ResponseWrapper.class);

        handleErrorResponse(responseObj, id);

        for (Object o : responseObj.getResponse()) {
            FieldResponseDto fieldResponseDto = objectMapper.readValue(JsonUtils.javaObjectToJsonString(o), FieldResponseDto.class);
            response.add(fieldResponseDto);
        }

        return response;
    }

    public BiometricRecord getBiometrics(String id, String person, List<String> modalities, String source,
                                         String process)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {

        BiometricRequestDto fieldDto = new BiometricRequestDto(id, person, modalities, source, process, false);

        RequestWrapper<BiometricRequestDto> request = new RequestWrapper<>();
        request.setId(ID);
        request.setVersion(VERSION);
        request.setRequesttime(DateUtils2.getUTCCurrentDateTime());
        request.setRequest(fieldDto);
        ResponseWrapper<BiometricRecord> response = postApi(ApiName.PACKETMANAGER_SEARCH_BIOMETRICS, request, ResponseWrapper.class);

        handleErrorResponse(response, id);

        if (response.getResponse() != null) {
            BiometricRecord biometricRecord = objectMapper.readValue(JsonUtils.javaObjectToJsonString(response.getResponse()), BiometricRecord.class);
            return biometricRecord;
        }
        return null;

    }

    public Map<String, String> getMetaInfo(String id, String source, String process)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        InfoDto fieldDto = new InfoDto(id, source, process, false);

        RequestWrapper<InfoDto> request = new RequestWrapper<>();
        request.setId(ID);
        request.setVersion(VERSION);
        request.setRequesttime(DateUtils2.getUTCCurrentDateTime());
        request.setRequest(fieldDto);
        ResponseWrapper<FieldResponseDto> response = postApi(ApiName.PACKETMANAGER_SEARCH_METAINFO, request, ResponseWrapper.class);

        if (CollectionUtils.isNotEmpty(response.getErrors())) {
            regProcLogger.error(SESSION_ID, REGISTRATION_ID, id, JsonUtils.javaObjectToJsonString(response));
            ErrorDTO errorDTO = response.getErrors().iterator().next();
            if (OBJECT_DOESNOT_EXISTS_ERROR_CODE.equalsIgnoreCase(errorDTO.getErrorCode()))
                throw new ObjectDoesnotExistsException(errorDTO.getErrorCode(), errorDTO.getMessage());
            if (PACKET_MANAGER_NON_RECOVERABLE_ERROR_CODES.contains(errorDTO.getErrorCode()))
                throw new PacketManagerNonRecoverableException(errorDTO.getErrorCode(), errorDTO.getMessage());
            throw new PacketManagerException(errorDTO.getErrorCode(), errorDTO.getMessage());
        }

        FieldResponseDto fieldResponseDto = objectMapper.readValue(JsonUtils.javaObjectToJsonString(response.getResponse()), FieldResponseDto.class);

        return fieldResponseDto.getFields();
    }

    /**
     * OPTIMIZED: info() with simple caching to reduce repeated API calls
     */
    public InfoResponseDto info(String id)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        // Quick cache check for repeated calls within 5 seconds
        if (infoCache.containsKey(id)) {
            return infoCache.get(id);
        }

        InfoRequestDto infoRequestDto = new InfoRequestDto(id);

        RequestWrapper<InfoRequestDto> request = new RequestWrapper<>();
        request.setId(ID);
        request.setVersion(VERSION);
        request.setRequesttime(DateUtils2.getUTCCurrentDateTime());
        request.setRequest(infoRequestDto);
        ResponseWrapper<InfoResponseDto> response = postApi(ApiName.PACKETMANAGER_INFO, request, ResponseWrapper.class);

        handleErrorResponse(response, id);

        InfoResponseDto infoResponseDto = objectMapper.readValue(JsonUtils.javaObjectToJsonString(response.getResponse()), InfoResponseDto.class);

        // Cache the result
        infoCache.put(id, infoResponseDto);

        // Clear cache after TTL (simple approach)
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                infoCache.remove(id);
            }
        }, INFO_CACHE_TTL_MS);

        return infoResponseDto;
    }

    public void addOrUpdateTags(String id, Map<String, String> tags) throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        UpdateTagRequestDto updateTagRequestDto = new UpdateTagRequestDto(id, tags);

        RequestWrapper<UpdateTagRequestDto> request = new RequestWrapper<>();
        request.setId(ID);
        request.setVersion(VERSION);
        request.setRequesttime(DateUtils2.getUTCCurrentDateTime());
        request.setRequest(updateTagRequestDto);
        ResponseWrapper<Void> response = postApi(ApiName.PACKETMANAGER_UPDATE_TAGS, request, ResponseWrapper.class);

        handleErrorResponse(response, id);
    }

    @SuppressWarnings("unchecked")
    public void deleteTags(String id, List<String> tags)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException {
        DeleteTagRequestDTO deleteTagREquestDto = new DeleteTagRequestDTO(id, tags);
        RequestWrapper<DeleteTagRequestDTO> request = new RequestWrapper<>();
        request.setId(ID);
        request.setVersion(VERSION);
        request.setRequesttime(DateUtils2.getUTCCurrentDateTime());
        request.setRequest(deleteTagREquestDto);
        ResponseWrapper<DeleteTagResponseDTO> response = (ResponseWrapper<DeleteTagResponseDTO>) postApi(
                ApiName.PACKETMANAGER_DELETE_TAGS, request, ResponseWrapper.class);

        handleErrorResponse(response, id);
    }

    public Map<String, String> getAllTags(String id) throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        return getTags(id, null);
    }

    public Map<String, String> getTags(String id, List<String> tagNames) throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        TagRequestDto tagRequestDto = new TagRequestDto(id, tagNames);
        RequestWrapper<TagRequestDto> request = new RequestWrapper<>();
        request.setId(ID);
        request.setVersion(VERSION);
        request.setRequesttime(DateUtils2.getUTCCurrentDateTime());
        request.setRequest(tagRequestDto);
        ResponseWrapper<TagResponseDto> response = (ResponseWrapper<TagResponseDto>) postApi(
                ApiName.PACKETMANAGER_GET_TAGS, request, ResponseWrapper.class);

        if (response.getErrors() != null && response.getErrors().size() > 0) {
            ErrorDTO error = response.getErrors().get(0);
            regProcLogger.error(SESSION_ID, REGISTRATION_ID, id, JsonUtils.javaObjectToJsonString(response));
            //This error code will return if requested tag is not present ,so returning null for that
            if (error.getErrorCode().equalsIgnoreCase("KER-PUT-024"))
                return null;
            else {
                ErrorDTO errorDTO = response.getErrors().iterator().next();
                if (OBJECT_DOESNOT_EXISTS_ERROR_CODE.equalsIgnoreCase(errorDTO.getErrorCode()))
                    throw new ObjectDoesnotExistsException(errorDTO.getErrorCode(), errorDTO.getMessage());
                if (PACKET_MANAGER_NON_RECOVERABLE_ERROR_CODES.contains(errorDTO.getErrorCode()))
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