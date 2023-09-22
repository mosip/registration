package io.mosip.registration.processor.credentialrequestor.util;

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
import io.mosip.registration.processor.packet.storage.exception.ParsingException;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
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
    private Environment env;

    @Autowired
    private Utilities utilities;

    @Value("#{T(java.util.Arrays).asList('${mosip.mandatory-languages:}')}")
    private List<String> mandatoryLanguages;

    @Value("${mosip.registration.processor.print.issuer.noMatch}")
    private String noMatchIssuer;

    @Value("#{${mosip.registration.processor.print.issuer.config-map:{}}}")
    private Map<String, String> credentialPartnerExpression;

    /**
     * This map will hold the actual field names after resolving, using mapping JSON as keys and
     * configured field names as values
     */
    private Map<String, String> requiredIDObjectFieldNamesMap;
    /**
     * Configured Id object fields
     */
    private List<String> requiredIdObjectFieldNames;

    public List<String> getCredentialPartners(String regId, String registrationType, JSONObject identity) throws PacketManagerException, JSONException, ApisResourceAccessException, IOException, JsonProcessingException {

        regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                regId, "CredentialPartnerUtil::getCredentialPartners()::entry");

        List<String> filteredPartners = new ArrayList<>();
        if (credentialPartnerExpression == null || credentialPartnerExpression.isEmpty()) {
            regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
                    LoggerFileConstant.REGISTRATIONID.toString(), regId,
                    PlatformErrorMessages.RPR_PRT_ISSUER_NOT_FOUND_IN_PROPERTY.name());
            return filteredPartners;
        }

        Map<String, String> identityFieldValueMap = utilities.getPacketManagerService().getFields(regId,
                requiredIdObjectFieldNames, registrationType, ProviderStageName.EVENT_HANDLER);

        Map<String, Object> context = new HashMap<>();
        for (Map.Entry<String, String> identityAttribute: identityFieldValueMap.entrySet()) {
            JSONObject attributeObject = new JSONObject(identityFieldValueMap);
            try {
                JSONParser parser = new JSONParser();
                Object obj = parser.parse((String)attributeObject.get(identityAttribute.getKey()));
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
                throw new ParsingException(PlatformErrorMessages.RPR_BDD_JSON_PARSING_EXCEPTION.getCode(), e);
            }
        }

        regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                regId, "CredentialPartnerUtil::CredentialPartnerExpression::" + credentialPartnerExpression.toString());

        for(Map.Entry<String, String> entry : credentialPartnerExpression.entrySet()) {
            Boolean result = (Boolean) MVEL.eval(entry.getValue(), context);
            if (result) {
                filteredPartners.add(entry.getKey());
            }
        }
        if (StringUtils.hasText(noMatchIssuer) && filteredPartners.isEmpty()) {
            filteredPartners.add(noMatchIssuer);
        }
        regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                regId, "CredentialPartnerUtil::FilteredPartners::" + filteredPartners.toString());

        return filteredPartners;
    }

    @PostConstruct
    private void getIdObjectFieldNames() throws BaseCheckedException {
        regProcLogger.info( "CredentialPartnerUtil::getIdObjectFieldNames()::PostConstruct");
        try {
            org.json.simple.JSONObject identityMappingJson =
                    utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
            requiredIDObjectFieldNamesMap = new HashMap<>();
            for(Map.Entry<String, String> expressionEntry : credentialPartnerExpression.entrySet()) {
                ParserContext parserContext = ParserContext.create();
                MVEL.analysisCompile(expressionEntry.getValue(), parserContext);
                Map<String, Class> expressionVariablesMap = parserContext.getInputs();
                for(Map.Entry<String, Class> variableEntry: expressionVariablesMap.entrySet()) {
                    String actualFieldName = JsonUtil.getJSONValue(
                            JsonUtil.getJSONObject(identityMappingJson, variableEntry.getKey()),
                            VALUE_LABEL);
                    if(actualFieldName == null)
                        throw new BaseCheckedException(
                                PlatformErrorMessages.RPR_PCM_FIELD_NAME_NOT_AVAILABLE_IN_MAPPING_JSON.getCode(),
                                PlatformErrorMessages.RPR_PCM_FIELD_NAME_NOT_AVAILABLE_IN_MAPPING_JSON.getMessage());
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
