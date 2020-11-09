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
public class PacketManagerService {

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

    public String getFieldByKey(String id, String key, String process) throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        JSONObject regProcessorIdentityJson = utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
        String field = JsonUtil.getJSONValue(
                JsonUtil.getJSONObject(regProcessorIdentityJson, key),
                MappingJsonConstants.VALUE);
        String source = utilities.getSource(MappingJsonConstants.IDENTITY, process, key);

        return getField(id, field, source, process);
    }


    public String getField(String id, String field, String source, String process) throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        FieldDto fieldDto = new FieldDto(id, field, source, process, false);

        RequestWrapper<FieldDto> request = new RequestWrapper<>();
        request.setId(ID);
        request.setVersion(VERSION);
        request.setRequesttime(DateUtils.getUTCCurrentDateTime());
        request.setRequest(fieldDto);
        ResponseWrapper<FieldResponseDto> response = (ResponseWrapper) restApi.postApi(ApiName.PACKETMANAGER_SEARCH_FIELD, "", "", request, ResponseWrapper.class);

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

    public Map<String, String> getFields(String id, List<String> idjsonKeys, String process) throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        Map<String, String> response = new HashMap<>();
        if (idjsonKeys != null && !idjsonKeys.isEmpty()) {
            Map<String, String> sourceFieldMap = new HashMap<>();
            for (String field : idjsonKeys) {
                String source = utilities.getSourceFromIdField(MappingJsonConstants.IDENTITY, process, field);
                String val = sourceFieldMap.get(source) != null ? sourceFieldMap.get(source) + "," + field : field;
                sourceFieldMap.put(source, val);
            }

            for (Map.Entry<String, String> entry : sourceFieldMap.entrySet()) {
                List<String> items = Arrays.asList(entry.getValue().split(","));
                response.putAll(getFields(id, items, entry.getKey(), process));
            }
        }
        return response;
    }

    public Map<String, String> getFields(String id, List<String> fields, String source, String process) throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        FieldDtos fieldDto = new FieldDtos(id, fields, source, process, false);

        RequestWrapper<FieldDtos> request = new RequestWrapper<>();
        request.setId(ID);
        request.setVersion(VERSION);
        request.setRequesttime(DateUtils.getUTCCurrentDateTime());
        request.setRequest(fieldDto);
        ResponseWrapper<FieldResponseDto> response = (ResponseWrapper) restApi.postApi(ApiName.PACKETMANAGER_SEARCH_FIELDS, "", "", request, ResponseWrapper.class);

        if (response.getErrors() != null && response.getErrors().size() > 0) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id, JsonUtils.javaObjectToJsonString(response));
            throw new PacketManagerException(response.getErrors().get(0).getErrorCode(), response.getErrors().get(0).getMessage());
        }

        FieldResponseDto fieldResponseDto = objectMapper.readValue(JsonUtils.javaObjectToJsonString(response.getResponse()), FieldResponseDto.class);

        return fieldResponseDto.getFields();
    }

    public Document getDocument(String id, String documentName, String process) throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        JSONObject regProcessorIdentityJson = utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT);
        String docKey = JsonUtil.getJSONValue(
                JsonUtil.getJSONObject(regProcessorIdentityJson, documentName),
                MappingJsonConstants.VALUE);
        String source = utilities.getSource(MappingJsonConstants.DOCUMENT, process, documentName);
        DocumentDto fieldDto = new DocumentDto(id, docKey, source, process);

        RequestWrapper<DocumentDto> request = new RequestWrapper<>();
        request.setId(ID);
        request.setVersion(VERSION);
        request.setRequesttime(DateUtils.getUTCCurrentDateTime());
        request.setRequest(fieldDto);
        ResponseWrapper<Document> response = (ResponseWrapper) restApi.postApi(ApiName.PACKETMANAGER_SEARCH_DOCUMENT, "", "", request, ResponseWrapper.class);

        if (response.getErrors() != null && response.getErrors().size() > 0) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id, JsonUtils.javaObjectToJsonString(response));
            throw new PacketManagerException(response.getErrors().get(0).getErrorCode(), response.getErrors().get(0).getMessage());
        }

        Document document = objectMapper.readValue(JsonUtils.javaObjectToJsonString(response.getResponse()), Document.class);

        return document;
    }

    public ValidatePacketResponse validate(String id, String process) throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        InfoDto fieldDto = new InfoDto(id, utilities.getDefaultSource(process, ConfigEnum.READER), process, false);

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

    public List<FieldResponseDto> getAudits(String id, String process) throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        String source = utilities.getSource(MappingJsonConstants.AUDITS, process, null);

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

    public BiometricRecord getBiometrics(String id, String person, List<String> modalities, String process) throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {

        JSONObject regProcessorIdentityJson = utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
        String personField = JsonUtil.getJSONValue(
                JsonUtil.getJSONObject(regProcessorIdentityJson, person),
                MappingJsonConstants.VALUE);
        String source = utilities.getSource(MappingJsonConstants.IDENTITY, process, person);


        BiometricRequestDto fieldDto = new BiometricRequestDto(id, personField, modalities, source, process, false);

        RequestWrapper<BiometricRequestDto> request = new RequestWrapper<>();
        request.setId(ID);
        request.setVersion(VERSION);
        request.setRequesttime(DateUtils.getUTCCurrentDateTime());
        request.setRequest(fieldDto);
        ResponseWrapper<BiometricRecord> response = (ResponseWrapper) restApi.postApi(ApiName.PACKETMANAGER_SEARCH_BIOMETRICS, "", "", request, ResponseWrapper.class);

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

    public Map<String, String> getMetaInfo(String id, String process) throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        String source = utilities.getSource(MappingJsonConstants.METAINFO, process, null);
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

}
