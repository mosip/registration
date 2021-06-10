package io.mosip.registration.processor.packet.storage.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.http.RequestWrapper;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.dto.BiometricRequestDto;
import io.mosip.registration.processor.packet.storage.dto.BiometricType;
import io.mosip.registration.processor.packet.storage.dto.ConfigEnum;
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
import io.mosip.registration.processor.core.exception.PacketManagerException;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PacketManagerService extends PriorityBasedPacketManagerService {

    private static Logger regProcLogger = RegProcessorLogger.getLogger(PacketManagerService.class);
    private static final String ID = "mosip.commmons.packetmanager";
    private static final String VERSION = "v1";

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
        regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                id, "Calling PACKETMANAGER_SEARCH_FIELD API to get value for : " + field);
        ResponseWrapper<FieldResponseDto> response = (ResponseWrapper) restApi.postApi(ApiName.PACKETMANAGER_SEARCH_FIELD, "", "", request, ResponseWrapper.class);
        regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                id, "Received response from PACKETMANAGER_SEARCH_FIELD API for : " + field);

        if (response.getErrors() != null && response.getErrors().size() > 0) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id, JsonUtils.javaObjectToJsonString(response));
            throw new PacketManagerException(response.getErrors().get(0).getErrorCode(), response.getErrors().get(0).getMessage());
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

        regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                id, "Calling PACKETMANAGER_SEARCH_FIELDS API to get values for : " + fields);

        ResponseWrapper<FieldResponseDto> response = (ResponseWrapper) restApi.postApi(ApiName.PACKETMANAGER_SEARCH_FIELDS, "", "", request, ResponseWrapper.class);

        regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                id, "Received response from PACKETMANAGER_SEARCH_FIELDS API for : " + fields);

        if (response.getErrors() != null && response.getErrors().size() > 0) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id, JsonUtils.javaObjectToJsonString(response));
            throw new PacketManagerException(response.getErrors().get(0).getErrorCode(), response.getErrors().get(0).getMessage());
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
        regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                id, "Calling PACKETMANAGER_SEARCH_DOCUMENT API for : " + documentName);

        ResponseWrapper<Document> response = (ResponseWrapper) restApi.postApi(ApiName.PACKETMANAGER_SEARCH_DOCUMENT, "", "", request, ResponseWrapper.class);

        regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                id, "Received response from PACKETMANAGER_SEARCH_DOCUMENT API for : " + documentName);

        if (response.getErrors() != null && response.getErrors().size() > 0) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id, JsonUtils.javaObjectToJsonString(response));
            throw new PacketManagerException(response.getErrors().get(0).getErrorCode(), response.getErrors().get(0).getMessage());
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
            throw new PacketManagerException(response.getErrors().get(0).getErrorCode(), response.getErrors().get(0).getMessage());
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
            throw new PacketManagerException(responseObj.getErrors().get(0).getErrorCode(), responseObj.getErrors().get(0).getMessage());
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
        regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                id, "Calling PACKETMANAGER_SEARCH_BIOMETRICS API for : " + person);

        ResponseWrapper<BiometricRecord> response = (ResponseWrapper) restApi.postApi(ApiName.PACKETMANAGER_SEARCH_BIOMETRICS, "", "", request, ResponseWrapper.class);

        regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                id, "Received response from PACKETMANAGER_SEARCH_BIOMETRICS API for : " + person);
        if (response.getErrors() != null && response.getErrors().size() > 0) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id, JsonUtils.javaObjectToJsonString(response));
            throw new PacketManagerException(response.getErrors().get(0).getErrorCode(), response.getErrors().get(0).getMessage());
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

        if (response.getErrors() != null && response.getErrors().size() > 0) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id, JsonUtils.javaObjectToJsonString(response));
            throw new PacketManagerException(response.getErrors().get(0).getErrorCode(), response.getErrors().get(0).getMessage());
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
            throw new PacketManagerException(response.getErrors().get(0).getErrorCode(), response.getErrors().get(0).getMessage());
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
            throw new PacketManagerException(response.getErrors().get(0).getErrorCode(), response.getErrors().get(0).getMessage());
        }
    }

}
