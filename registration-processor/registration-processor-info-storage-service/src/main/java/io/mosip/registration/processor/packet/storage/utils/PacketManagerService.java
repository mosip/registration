package io.mosip.registration.processor.packet.storage.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import jakarta.annotation.PostConstruct;
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
import reactor.core.publisher.Mono;

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

    private final Map<String, CacheEntryWithTimestamp> infoCache = new ConcurrentHashMap<>();
    private static final long INFO_CACHE_TTL_MS = 5000; // 5 second TTL

    @Autowired
    @Qualifier("packetManagerWebClient")
    private WebClient webClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Environment env;

    /**
     * Inner class for cache with timestamp
     */
    private static class CacheEntryWithTimestamp {
        private final InfoResponseDto data;
        private final long timestamp;

        CacheEntryWithTimestamp(InfoResponseDto data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > INFO_CACHE_TTL_MS;
        }
    }

    @PostConstruct
    private void setObjectMapper() {
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * FIX: Removed .onStatus() handler - WebClient automatically handles 2xx as success
     * Only use .onStatus() if you need special handling for specific status codes
     */
    private <T> T postApi(ApiName apiName, Object requestObject, Class<T> responseClass)
            throws ApisResourceAccessException, IOException {
        String uri = null;
        try {
            uri = env.getProperty(apiName.name());

            if (uri == null) {
                String errorMsg = "API endpoint not configured for: " + apiName.toString();
                regProcLogger.error(SESSION_ID, APPLICATION_ID, APPLICATION_ID, errorMsg);
                throw new ApisResourceAccessException(errorMsg);
            }

            regProcLogger.info("Calling API: " + uri);

            // FIX: Remove ALL .onStatus() handlers for successful responses
            // WebClient automatically accepts 2xx without special handling
            return webClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(requestObject)
                    .retrieve()
                    // ONLY handle 4xx and 5xx errors
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        String errorMsg = "HTTP " + clientResponse.statusCode() +
                                                " - " + clientResponse.toString() + ": " + body;
                                        regProcLogger.error(SESSION_ID, APPLICATION_ID, APPLICATION_ID,
                                                "Error response: " + errorMsg);
                                        return Mono.error(new ApisResourceAccessException(errorMsg));
                                    }))
                    .bodyToMono(responseClass)
                    .timeout(Duration.ofMillis(REQUEST_TIMEOUT_MS))
                    .onErrorMap(ex -> {
                        // Don't wrap ApisResourceAccessException again
                        if (ex instanceof ApisResourceAccessException) {
                            return ex;
                        }
                        // Handle timeout
                        if (ex instanceof TimeoutException) {
                            return new ApisResourceAccessException("Request timeout after " + REQUEST_TIMEOUT_MS + "ms", ex);
                        }
                        // Handle WebClientResponseException (should not happen with above logic)
                        if (ex instanceof WebClientResponseException) {
                            WebClientResponseException wex = (WebClientResponseException) ex;
                            return new ApisResourceAccessException("WebClient error: " + wex.getMessage(), ex);
                        }
                        return new ApisResourceAccessException("Unexpected error: " + ex.getMessage(), ex);
                    })
                    .block();

           /* regProcLogger.info(SESSION_ID, APPLICATION_ID, APPLICATION_ID, uri);

            // FIX: Simplified - WebClient automatically treats 2xx as success
            // Only handle error status codes (4xx, 5xx)
            return webClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(requestObject)
                    .retrieve()
                    // Only handle error responses (4xx, 5xx)
                    .onStatus(status -> status.isError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(body -> new ApisResourceAccessException(
                                            "HTTP " + clientResponse.statusCode() +
                                                    " - " + clientResponse.logPrefix() +
                                                    ": " + body)))
                    .bodyToMono(responseClass)
                    .timeout(Duration.ofMillis(REQUEST_TIMEOUT_MS))
                    .block();*/

        } catch (ApisResourceAccessException e) {
            regProcLogger.error(SESSION_ID, APPLICATION_ID, APPLICATION_ID,
                    "API Error for " + apiName + ": " + e.getMessage() + ExceptionUtils.getStackTrace(e));
            throw e;
        } catch (WebClientResponseException e) {
            regProcLogger.error(SESSION_ID, APPLICATION_ID, APPLICATION_ID,
                    "WebClient error for " + apiName + ": " + e.getRawStatusCode() +
                            " - " + e.getStatusText() + " - " + e.getResponseBodyAsString());
            throw new ApisResourceAccessException(e.getMessage(), e);
        } catch (Exception e) {
            regProcLogger.error(SESSION_ID, APPLICATION_ID, APPLICATION_ID,
                    "Unexpected error for " + apiName + ": " + e.getMessage() + ExceptionUtils.getStackTrace(e));
            throw new ApisResourceAccessException("Error calling API: " + e.getMessage(), e);
        }
    }

    /**
     * Optimized error handling
     */
    private void handleErrorResponse(ResponseWrapper<?> response, String id)
            throws ApisResourceAccessException, PacketManagerException, ObjectDoesnotExistsException,
            PacketManagerNonRecoverableException, JsonProcessingException {

        if (response == null) {
            throw new ApisResourceAccessException("Response wrapper is null");
        }

        if (response.getErrors() != null && response.getErrors().size() > 0) {
            regProcLogger.error(SESSION_ID, REGISTRATION_ID, id, JsonUtils.javaObjectToJsonString(response));

            ErrorDTO errorDTO = response.getErrors().iterator().next();

            if (errorDTO == null) {
                throw new PacketManagerException("Unknown error occurred", "UNKNOWN_ERROR");
            }

            if (OBJECT_DOESNOT_EXISTS_ERROR_CODE.equalsIgnoreCase(errorDTO.getErrorCode()))
                throw new ObjectDoesnotExistsException(errorDTO.getErrorCode(), errorDTO.getMessage());
            if (PACKET_MANAGER_NON_RECOVERABLE_ERROR_CODES.contains(errorDTO.getErrorCode()))
                throw new PacketManagerNonRecoverableException(errorDTO.getErrorCode(), errorDTO.getMessage());
            throw new PacketManagerException(errorDTO.getErrorCode(), errorDTO.getMessage());
        }
    }

    public String getField(String id, String field, String source, String process)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        try {
            if (StringUtils.isBlank(id) || StringUtils.isBlank(field)) {
                throw new IllegalArgumentException("id and field cannot be blank");
            }

            FieldDto fieldDto = new FieldDto(id, field, source, process, false);

            RequestWrapper<FieldDto> request = new RequestWrapper<>();
            request.setId(ID);
            request.setVersion(VERSION);
            request.setRequesttime(DateUtils2.getUTCCurrentDateTime());
            request.setRequest(fieldDto);

            ResponseWrapper<FieldResponseDto> response = postApi(ApiName.PACKETMANAGER_SEARCH_FIELD, request, ResponseWrapper.class);

            handleErrorResponse(response, id);

            if (response.getResponse() == null) {
                regProcLogger.warn(SESSION_ID, REGISTRATION_ID, id, "No response data for field: " + field);
                return null;
            }

            FieldResponseDto fieldResponseDto = objectMapper.readValue(JsonUtils.javaObjectToJsonString(response.getResponse()), FieldResponseDto.class);

            String responseField = fieldResponseDto.getFields().get(field);
            if (StringUtils.isNotEmpty(responseField) && responseField.equalsIgnoreCase("null"))
                responseField = null;
            return responseField;
        } catch (ApisResourceAccessException | PacketManagerException | ObjectDoesnotExistsException |
                 JsonProcessingException | IOException e) {
            throw e;
        } catch (Exception e) {
            regProcLogger.error(SESSION_ID, REGISTRATION_ID, id, "Error in getField: " + ExceptionUtils.getStackTrace(e));
            throw new ApisResourceAccessException("Error retrieving field: " + e.getMessage(), e);
        }
    }

    public Map<String, String> getFields(String id, List<String> fields, String source, String process)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        try {
            if (StringUtils.isBlank(id) || CollectionUtils.isEmpty(fields)) {
                throw new IllegalArgumentException("id and fields cannot be blank or empty");
            }

            FieldDtos fieldDto = new FieldDtos(id, fields, source, process, false);

            RequestWrapper<FieldDtos> request = new RequestWrapper<>();
            request.setId(ID);
            request.setVersion(VERSION);
            request.setRequesttime(DateUtils2.getUTCCurrentDateTime());
            request.setRequest(fieldDto);

            ResponseWrapper<FieldResponseDto> response = postApi(ApiName.PACKETMANAGER_SEARCH_FIELDS, request, ResponseWrapper.class);

            handleErrorResponse(response, id);

            if (response.getResponse() == null) {
                regProcLogger.warn(SESSION_ID, REGISTRATION_ID, id, "No response data for fields");
                return new java.util.HashMap<>();
            }

            FieldResponseDto fieldResponseDto = objectMapper.readValue(JsonUtils.javaObjectToJsonString(response.getResponse()), FieldResponseDto.class);

            return fieldResponseDto.getFields() != null ? fieldResponseDto.getFields() : new java.util.HashMap<>();
        } catch (ApisResourceAccessException | PacketManagerException | ObjectDoesnotExistsException |
                 JsonProcessingException | IOException e) {
            throw e;
        } catch (Exception e) {
            regProcLogger.error(SESSION_ID, REGISTRATION_ID, id, "Error in getFields: " + ExceptionUtils.getStackTrace(e));
            throw new ApisResourceAccessException("Error retrieving fields: " + e.getMessage(), e);
        }
    }

    public Document getDocument(String id, String documentName, String process)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        return getDocument(id, documentName, null, process);
    }

    public Document getDocument(String id, String documentName, String source, String process)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        try {
            if (StringUtils.isBlank(id) || StringUtils.isBlank(documentName)) {
                throw new IllegalArgumentException("id and documentName cannot be blank");
            }

            DocumentDto fieldDto = new DocumentDto(id, documentName, source, process);

            RequestWrapper<DocumentDto> request = new RequestWrapper<>();
            request.setId(ID);
            request.setVersion(VERSION);
            request.setRequesttime(DateUtils2.getUTCCurrentDateTime());
            request.setRequest(fieldDto);

            ResponseWrapper<Document> response = postApi(ApiName.PACKETMANAGER_SEARCH_DOCUMENT, request, ResponseWrapper.class);

            handleErrorResponse(response, id);

            if (response.getResponse() == null) {
                regProcLogger.warn(SESSION_ID, REGISTRATION_ID, id, "No response data for document: " + documentName);
                return null;
            }

            Document document = objectMapper.readValue(JsonUtils.javaObjectToJsonString(response.getResponse()), Document.class);

            return document;
        } catch (ApisResourceAccessException | PacketManagerException | ObjectDoesnotExistsException |
                 JsonProcessingException | IOException e) {
            throw e;
        } catch (Exception e) {
            regProcLogger.error(SESSION_ID, REGISTRATION_ID, id, "Error in getDocument: " + ExceptionUtils.getStackTrace(e));
            throw new ApisResourceAccessException("Error retrieving document: " + e.getMessage(), e);
        }
    }

    public ValidatePacketResponse validate(String id, String source, String process)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        try {
            if (StringUtils.isBlank(id)) {
                throw new IllegalArgumentException("id cannot be blank");
            }

            InfoDto fieldDto = new InfoDto(id, source, process, false);

            RequestWrapper<InfoDto> request = new RequestWrapper<>();
            request.setId(ID);
            request.setVersion(VERSION);
            request.setRequesttime(DateUtils2.getUTCCurrentDateTime());
            request.setRequest(fieldDto);

            ResponseWrapper<ValidatePacketResponse> response = postApi(ApiName.PACKETMANAGER_VALIDATE, request, ResponseWrapper.class);

            handleErrorResponse(response, id);

            if (response.getResponse() == null) {
                regProcLogger.warn(SESSION_ID, REGISTRATION_ID, id, "No response data for validate");
                return null;
            }

            ValidatePacketResponse validatePacketResponse = objectMapper.readValue(JsonUtils.javaObjectToJsonString(response.getResponse()), ValidatePacketResponse.class);

            return validatePacketResponse;
        } catch (ApisResourceAccessException | PacketManagerException | ObjectDoesnotExistsException |
                 JsonProcessingException | IOException e) {
            throw e;
        } catch (Exception e) {
            regProcLogger.error(SESSION_ID, REGISTRATION_ID, id, "Error in validate: " + ExceptionUtils.getStackTrace(e));
            throw new ApisResourceAccessException("Error validating packet: " + e.getMessage(), e);
        }
    }

    public List<FieldResponseDto> getAudits(String id, String source, String process)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        try {
            if (StringUtils.isBlank(id)) {
                throw new IllegalArgumentException("id cannot be blank");
            }

            InfoDto fieldDto = new InfoDto(id, source, process, false);
            List<FieldResponseDto> response = new ArrayList<>();

            RequestWrapper<InfoDto> request = new RequestWrapper<>();
            request.setId(ID);
            request.setVersion(VERSION);
            request.setRequesttime(DateUtils2.getUTCCurrentDateTime());
            request.setRequest(fieldDto);

            ResponseWrapper<List<FieldResponseDto>> responseObj = postApi(ApiName.PACKETMANAGER_SEARCH_AUDITS, request, ResponseWrapper.class);

            handleErrorResponse(responseObj, id);

            if (responseObj.getResponse() == null || responseObj.getResponse().isEmpty()) {
                regProcLogger.warn(SESSION_ID, REGISTRATION_ID, id, "No audit data found");
                return response;
            }

            for (Object o : responseObj.getResponse()) {
                if (o != null) {
                    FieldResponseDto fieldResponseDto = objectMapper.readValue(JsonUtils.javaObjectToJsonString(o), FieldResponseDto.class);
                    response.add(fieldResponseDto);
                }
            }

            return response;
        } catch (ApisResourceAccessException | PacketManagerException | ObjectDoesnotExistsException |
                 JsonProcessingException | IOException e) {
            throw e;
        } catch (Exception e) {
            regProcLogger.error(SESSION_ID, REGISTRATION_ID, id, "Error in getAudits: " + ExceptionUtils.getStackTrace(e));
            throw new ApisResourceAccessException("Error retrieving audits: " + e.getMessage(), e);
        }
    }

    public BiometricRecord getBiometrics(String id, String person, List<String> modalities, String source,
                                         String process)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        try {
            if (StringUtils.isBlank(id) || StringUtils.isBlank(person)) {
                throw new IllegalArgumentException("id and person cannot be blank");
            }

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

            regProcLogger.warn(SESSION_ID, REGISTRATION_ID, id, "No biometric data found for person: " + person);
            return null;
        } catch (ApisResourceAccessException | PacketManagerException | ObjectDoesnotExistsException |
                 JsonProcessingException | IOException e) {
            throw e;
        } catch (Exception e) {
            regProcLogger.error(SESSION_ID, REGISTRATION_ID, id, "Error in getBiometrics: " + ExceptionUtils.getStackTrace(e));
            throw new ApisResourceAccessException("Error retrieving biometrics: " + e.getMessage(), e);
        }
    }

    public Map<String, String> getMetaInfo(String id, String source, String process)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        try {
            if (StringUtils.isBlank(id)) {
                throw new IllegalArgumentException("id cannot be blank");
            }

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

            if (response.getResponse() == null) {
                regProcLogger.warn(SESSION_ID, REGISTRATION_ID, id, "No meta info data found");
                return new java.util.HashMap<>();
            }

            FieldResponseDto fieldResponseDto = objectMapper.readValue(JsonUtils.javaObjectToJsonString(response.getResponse()), FieldResponseDto.class);

            return fieldResponseDto.getFields() != null ? fieldResponseDto.getFields() : new java.util.HashMap<>();
        } catch (ApisResourceAccessException | PacketManagerException | ObjectDoesnotExistsException |
                 JsonProcessingException | IOException e) {
            throw e;
        } catch (Exception e) {
            regProcLogger.error(SESSION_ID, REGISTRATION_ID, id, "Error in getMetaInfo: " + ExceptionUtils.getStackTrace(e));
            throw new ApisResourceAccessException("Error retrieving meta info: " + e.getMessage(), e);
        }
    }

    public InfoResponseDto info(String id)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        try {
            if (StringUtils.isBlank(id)) {
                throw new IllegalArgumentException("id cannot be blank");
            }

            if (infoCache.containsKey(id)) {
                CacheEntryWithTimestamp entry = infoCache.get(id);
                if (!entry.isExpired()) {
                    regProcLogger.info(SESSION_ID, REGISTRATION_ID, id, "Using cached info response");
                    return entry.data;
                } else {
                    infoCache.remove(id);
                }
            }

            InfoRequestDto infoRequestDto = new InfoRequestDto(id);

            RequestWrapper<InfoRequestDto> request = new RequestWrapper<>();
            request.setId(ID);
            request.setVersion(VERSION);
            request.setRequesttime(DateUtils2.getUTCCurrentDateTime());
            request.setRequest(infoRequestDto);

            ResponseWrapper<InfoResponseDto> response = postApi(ApiName.PACKETMANAGER_INFO, request, ResponseWrapper.class);

            handleErrorResponse(response, id);

            if (response.getResponse() == null) {
                regProcLogger.warn(SESSION_ID, REGISTRATION_ID, id, "No info data found");
                return null;
            }

            InfoResponseDto infoResponseDto = objectMapper.readValue(JsonUtils.javaObjectToJsonString(response.getResponse()), InfoResponseDto.class);

            infoCache.put(id, new CacheEntryWithTimestamp(infoResponseDto));

            return infoResponseDto;
        } catch (ApisResourceAccessException | PacketManagerException | ObjectDoesnotExistsException |
                 JsonProcessingException | IOException e) {
            throw e;
        } catch (Exception e) {
            regProcLogger.error(SESSION_ID, REGISTRATION_ID, id, "Error in info: " + ExceptionUtils.getStackTrace(e));
            throw new ApisResourceAccessException("Error retrieving packet info: " + e.getMessage(), e);
        }
    }

    public void addOrUpdateTags(String id, Map<String, String> tags)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        try {
            if (StringUtils.isBlank(id) || tags.isEmpty()) {
                throw new IllegalArgumentException("id and tags cannot be blank or empty");
            }

            UpdateTagRequestDto updateTagRequestDto = new UpdateTagRequestDto(id, tags);

            RequestWrapper<UpdateTagRequestDto> request = new RequestWrapper<>();
            request.setId(ID);
            request.setVersion(VERSION);
            request.setRequesttime(DateUtils2.getUTCCurrentDateTime());
            request.setRequest(updateTagRequestDto);

            ResponseWrapper<Void> response = postApi(ApiName.PACKETMANAGER_UPDATE_TAGS, request, ResponseWrapper.class);

            handleErrorResponse(response, id);
        } catch (ApisResourceAccessException | PacketManagerException | ObjectDoesnotExistsException |
                 JsonProcessingException | IOException e) {
            throw e;
        } catch (Exception e) {
            regProcLogger.error(SESSION_ID, REGISTRATION_ID, id, "Error in addOrUpdateTags: " + ExceptionUtils.getStackTrace(e));
            throw new ApisResourceAccessException("Error updating tags: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public void deleteTags(String id, List<String> tags)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException {
        try {
            if (StringUtils.isBlank(id) || CollectionUtils.isEmpty(tags)) {
                throw new IllegalArgumentException("id and tags list cannot be blank or empty");
            }

            DeleteTagRequestDTO deleteTagREquestDto = new DeleteTagRequestDTO(id, tags);
            RequestWrapper<DeleteTagRequestDTO> request = new RequestWrapper<>();
            request.setId(ID);
            request.setVersion(VERSION);
            request.setRequesttime(DateUtils2.getUTCCurrentDateTime());
            request.setRequest(deleteTagREquestDto);

            ResponseWrapper<DeleteTagResponseDTO> response = (ResponseWrapper<DeleteTagResponseDTO>) postApi(
                    ApiName.PACKETMANAGER_DELETE_TAGS, request, ResponseWrapper.class);

            handleErrorResponse(response, id);
        } catch (ApisResourceAccessException | PacketManagerException | ObjectDoesnotExistsException |
                 JsonProcessingException e) {
            throw e;
        } catch (Exception e) {
            regProcLogger.error(SESSION_ID, REGISTRATION_ID, id, "Error in deleteTags: " + ExceptionUtils.getStackTrace(e));
            throw new ApisResourceAccessException("Error deleting tags: " + e.getMessage(), e);
        }
    }

    public Map<String, String> getAllTags(String id)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        return getTags(id, null);
    }

    public Map<String, String> getTags(String id, List<String> tagNames)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        try {
            if (StringUtils.isBlank(id)) {
                throw new IllegalArgumentException("id cannot be blank");
            }

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

                if (error == null) {
                    throw new PacketManagerException("Unknown error occurred", "UNKNOWN_ERROR");
                }

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
        } catch (ApisResourceAccessException | PacketManagerException | ObjectDoesnotExistsException |
                 JsonProcessingException | IOException e) {
            throw e;
        } catch (Exception e) {
            regProcLogger.error(SESSION_ID, REGISTRATION_ID, id, "Error in getTags: " + ExceptionUtils.getStackTrace(e));
            throw new ApisResourceAccessException("Error retrieving tags: " + e.getMessage(), e);
        }
    }
}