package io.mosip.registration.processor.print.util;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
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
    private static final String VALUE_LABEL = "value";
    private static final String LANGUAGE = "language";

    @Autowired
    private Utilities utilities;

    @Value("#{T(java.util.Arrays).asList('${mosip.mandatory-languages:}')}")
    private List<String> mandatoryLanguages;

    @Value("${mosip.registration.processor.print.issuer.noMatch}")
    private String noMatchIssuer;

    @Value("#{${mosip.registration.processor.print.issuer.config-map:{:}}}")
    private Map<String, String> credentialPartnerExpression;

    /**
     * Configured Id object fields
     */
    private List<String> requiredIdObjectFieldNames;

    public List<String> getCredentialPartners(String regId, String registrationType, String uin) {

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
            Map<String, String> identityFieldValueMap = utilities.getPacketManagerService().getFields(regId,
                    requiredIdObjectFieldNames, registrationType, ProviderStageName.PRINTING);

            getFieldValueInIdRepo(uin, identityFieldValueMap);

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

    /**
     * In case of demographic update the packet manager doesn't have required field values for the given RID,
     * so for such cases fetching the registration data from ID-REPO using 'uin'.
     * The below logic can be modified for any better approach without impacting performance.
     * @param uin
     * @param identityFieldValueMap
     * @throws ApisResourceAccessException
     * @throws IOException
     */
    private void getFieldValueInIdRepo(String uin, Map<String, String> identityFieldValueMap) throws ApisResourceAccessException, IOException {

        if (identityFieldValueMap != null && !identityFieldValueMap.isEmpty()) {
            List<String> keys = identityFieldValueMap.entrySet().stream().filter(e -> ObjectUtils.isEmpty(e.getValue())).map(Map.Entry::getKey).collect(Collectors.toList());
            if (keys != null && !keys.isEmpty()) {
                JSONObject jsonObject = utilities.retrieveIdrepoJson(uin);
                keys.forEach(key -> {
                    try {
                        identityFieldValueMap.put(key, JsonUtils.javaObjectToJsonString(jsonObject.get(key)));
                    } catch (JsonProcessingException e) {
                        regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
                                ExceptionUtils.getStackTrace(e));
                    }
                });
            }
        }
    }

    private Map<String, Object> getContext(Map<String, String> identityFieldValueMap) {

        Map<String, Object> context = new HashMap<>();
        for (Map.Entry<String, String> identityAttribute: identityFieldValueMap.entrySet()) {
            JSONObject attributeObject = new JSONObject(identityFieldValueMap);
            try {
                JSONParser parser = new JSONParser();
                Object attributeValue = attributeObject.get(identityAttribute.getKey());
                if (attributeValue == null) {
                    context.put(identityAttribute.getKey(), attributeValue);
                    continue;
                }
                Object obj = parser.parse((String) attributeValue);
                if (obj instanceof org.json.simple.JSONArray) {
                    org.json.simple.JSONArray attributeArray = (org.json.simple.JSONArray) obj;
                    for (int i = 0; i < attributeArray.size(); i++) {
                        JSONObject jsonObject = (JSONObject) attributeArray.get(i);
                        if (mandatoryLanguages.get(0).equalsIgnoreCase((String) jsonObject.get(LANGUAGE))) {
                            context.put(identityAttribute.getKey(), jsonObject.get(VALUE_LABEL));
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
            Map<String, String> requiredIDObjectFieldNamesMap = new HashMap<>();
            for(Map.Entry<String, String> expressionEntry : credentialPartnerExpression.entrySet()) {
                ParserContext parserContext = ParserContext.create();
                MVEL.analysisCompile(expressionEntry.getValue(), parserContext);
                Map<String, Class> expressionVariablesMap = parserContext.getInputs();
                for(Map.Entry<String, Class> variableEntry: expressionVariablesMap.entrySet()) {
                    String actualFieldName = JsonUtil.getJSONValue(
                            JsonUtil.getJSONObject(identityMappingJson, variableEntry.getKey()),
                            VALUE_LABEL);
                    if(actualFieldName == null) {
                        throw new BaseCheckedException(
                                PlatformErrorMessages.RPR_PCM_FIELD_NAME_NOT_AVAILABLE_IN_MAPPING_JSON.getCode(),
                                PlatformErrorMessages.RPR_PCM_FIELD_NAME_NOT_AVAILABLE_IN_MAPPING_JSON.getMessage());
                    }
                    requiredIDObjectFieldNamesMap.put(actualFieldName, variableEntry.getKey());
                }
            }
            requiredIdObjectFieldNames = requiredIDObjectFieldNamesMap.keySet().stream().collect(Collectors.toList());
        } catch (IOException e) {
            throw new BaseCheckedException(
                    PlatformErrorMessages.RPR_PCM_ACCESSING_IDOBJECT_MAPPING_FILE_FAILED.getCode(),
                    PlatformErrorMessages.RPR_PCM_ACCESSING_IDOBJECT_MAPPING_FILE_FAILED.getMessage(), e);
        }
    }
}
