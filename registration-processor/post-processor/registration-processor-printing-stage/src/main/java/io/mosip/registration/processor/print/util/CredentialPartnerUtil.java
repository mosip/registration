package io.mosip.registration.processor.print.util;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CredentialPartnerUtil {

    private static final Logger regProcLogger = RegProcessorLogger.getLogger(CredentialPartnerUtil.class);
    private static final String LANGUAGE = "language";
    public static final String META_INFO = "metaInfo_";

    @Autowired
    private Utilities utilities;

    @Value("#{T(java.util.Arrays).asList('${mosip.mandatory-languages:}')}")
    private List<String> mandatoryLanguages;

    @Value("${mosip.registration.processor.print.issuer.noMatch}")
    private String noMatchIssuer;

    @Value("#{${mosip.registration.processor.print.issuer.config-map:{:}}}")
    private Map<String, String> credentialPartnerExpression;

    /**
     * This map will hold the actual field names after resolving, using mapping JSON as keys and
     * configured field names as values
     */
    private Map<String, String> requiredIDObjectFieldNamesMap;

    public List<String> getCredentialPartners(String regId, String registrationType, JSONObject identityJson) {

        regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                regId, "CredentialPartnerUtil::getCredentialPartners()::entry");

        List<String> filteredPartners = new ArrayList<>();
        if (credentialPartnerExpression == null || credentialPartnerExpression.isEmpty()) {
            regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
                    LoggerFileConstant.REGISTRATIONID.toString(), regId,
                    PlatformErrorMessages.RPR_PRT_ISSUER_NOT_FOUND_IN_PROPERTY.name());
            return filteredPartners;
        }

        try {
            Map<String, String> identityFieldValueMap = new HashMap<>();
            if (!CollectionUtils.isEmpty(requiredIDObjectFieldNamesMap)) {
                requiredIDObjectFieldNamesMap.entrySet().forEach(entry -> identityFieldValueMap.put(entry.getValue(),
                        (identityJson.get(entry.getKey()) != null) ? identityJson.get(entry.getKey()).toString() : null));
            }

            getFieldValuesFromMeta(regId, registrationType, identityFieldValueMap);

            Map<String, Object> context = getContext(identityFieldValueMap);

            regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    regId, "CredentialPartnerUtil::CredentialPartnerExpression::" + credentialPartnerExpression.toString());

            for (Map.Entry<String, String> entry : credentialPartnerExpression.entrySet()) {
                Boolean result = MVEL.evalToBoolean(entry.getValue(), context);
                if (result) {
                    filteredPartners.add(entry.getKey());
                }
            }
            if (StringUtils.hasText(noMatchIssuer) && filteredPartners.isEmpty()) {
                filteredPartners.add(noMatchIssuer);
            }
        } catch (Exception e) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
                    ExceptionUtils.getStackTrace(e));
        }
        regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                regId, "CredentialPartnerUtil::FilteredPartners::" + filteredPartners.toString());

        return filteredPartners;
    }

    private void getFieldValuesFromMeta(String regId, String registrationType, Map<String, String> idenFieldValueMap) throws PacketManagerException, ApisResourceAccessException, IOException, JsonProcessingException, JSONException {

        List<Map.Entry<String, String>> metaFields = requiredIDObjectFieldNamesMap.entrySet().stream().filter(entry ->
                StringUtils.startsWithIgnoreCase( entry.getValue(), META_INFO)).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(metaFields)) {
            Map<String, String> metaInfo = utilities.getMetaInfo(regId, registrationType,
                    ProviderStageName.PRINTING);
            metaFields.stream().forEach( entry ->
                    idenFieldValueMap.put(entry.getValue(), metaInfo.get(entry.getKey()))
            );
        }
    }

    private Map<String, Object> getContext(Map<String, String> identityFieldValueMap) {

        Map<String, Object> context = new HashMap<>();
        JSONObject attributeObject = new JSONObject(identityFieldValueMap);
        for (Map.Entry<String, String> identityAttribute: identityFieldValueMap.entrySet()) {
            try {
                JSONParser parser = new JSONParser();
                Object attributeValue = attributeObject.get(identityAttribute.getKey());
                context.put(identityAttribute.getKey(), null);
                if (attributeValue == null) {
                    continue;
                }
                Object obj = parser.parse((String) attributeValue);
                if (obj instanceof org.json.simple.JSONArray) {
                    org.json.simple.JSONArray attributeArray = (org.json.simple.JSONArray) obj;
                    for (int i = 0; i < attributeArray.size(); i++) {
                        JSONObject jsonObject = (JSONObject) attributeArray.get(i);
                        if (!CollectionUtils.isEmpty(mandatoryLanguages)) {
                            if (mandatoryLanguages.get(0).equalsIgnoreCase((String) jsonObject.get(LANGUAGE))) {
                                context.put(identityAttribute.getKey(), jsonObject.get(MappingJsonConstants.VALUE));
                                break;
                            }
                        } else {
                            context.put(identityAttribute.getKey(), jsonObject.get(MappingJsonConstants.VALUE));
                            break;
                        }
                    }
                } else {
                    if (obj != null) {
                        context.put(identityAttribute.getKey(), obj.toString());
                    }
                }
            } catch (org.json.simple.parser.ParseException e) {
                regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
                        ExceptionUtils.getStackTrace(e));
            }
        }
        return context;
    }

    @PostConstruct
    private void getIdObjectFieldNames() throws BaseCheckedException {
        regProcLogger.info( "CredentialPartnerUtil::getIdObjectFieldNames()::PostConstruct");
        try {
            org.json.simple.JSONObject identityMappingJson =
                    utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
            org.json.simple.JSONObject metaInfoMappingJson =
                    utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.METAINFO);
            requiredIDObjectFieldNamesMap = new HashMap<>();
            for(Map.Entry<String, String> expressionEntry : credentialPartnerExpression.entrySet()) {
                ParserContext parserContext = ParserContext.create();
                MVEL.analysisCompile(expressionEntry.getValue(), parserContext);
                Map<String, Class> expressionVariablesMap = parserContext.getInputs();
                for(Map.Entry<String, Class> variableEntry: expressionVariablesMap.entrySet()) {
                    String actualFieldName = JsonUtil.getJSONValue(
                            JsonUtil.getJSONObject(identityMappingJson, variableEntry.getKey()),
                            MappingJsonConstants.VALUE);
                    if (actualFieldName == null && StringUtils.startsWithIgnoreCase( variableEntry.getKey(), META_INFO)) {
                        actualFieldName = JsonUtil.getJSONValue(
                                JsonUtil.getJSONObject(metaInfoMappingJson, variableEntry.getKey().substring(META_INFO.length())),
                                MappingJsonConstants.VALUE);
                    }
                    if(actualFieldName == null) {
                        throw new BaseCheckedException(
                                PlatformErrorMessages.RPR_PCM_FIELD_NAME_NOT_AVAILABLE_IN_MAPPING_JSON.getCode(),
                                PlatformErrorMessages.RPR_PCM_FIELD_NAME_NOT_AVAILABLE_IN_MAPPING_JSON.getMessage());
                    }
                    requiredIDObjectFieldNamesMap.put(actualFieldName, variableEntry.getKey());
                }
            }
        } catch (IOException e) {
            throw new BaseCheckedException(
                    PlatformErrorMessages.RPR_PCM_ACCESSING_IDOBJECT_MAPPING_FILE_FAILED.getCode(),
                    PlatformErrorMessages.RPR_PCM_ACCESSING_IDOBJECT_MAPPING_FILE_FAILED.getMessage(), e);
        }
    }
}
