package io.mosip.registration.processor.credentialrequestor.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.constant.*;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.FieldValue;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.credentialrequestor.dto.CredentialPartner;
import io.mosip.registration.processor.credentialrequestor.dto.CredentialPartnersList;
import io.mosip.registration.processor.packet.storage.exception.ParsingException;
import io.mosip.registration.processor.packet.storage.utils.IdSchemaUtil;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import org.apache.commons.collections.MapUtils;
import org.assertj.core.util.Lists;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class CredentialPartnerUtil {

    private static final Logger regProcLogger = RegProcessorLogger.getLogger(CredentialPartnerUtil.class);
    private static final String VALUE_LABEL = "value";
    private static final String LANGUAGE = "language";

    private CredentialPartnersList credentialPartners;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Environment env;

    @Autowired
    private Utilities utilities;

    @Autowired
    IdSchemaUtil idSchemaUtil;

    @Value("${mosip.registration.processor.credential.partner-profiles}")
    private String partnerProfileFileName;

    @Value("#{T(java.util.Arrays).asList('${mosip.mandatory-languages:}')}")
    private List<String> mandatoryLanguages;

    @Value("${mosip.registration.processor.credential.conditional.no-match-partner-ids}")
    private String noMatchIssuer;

    @Value("#{${mosip.registration.processor.credential.conditional.partner-id-map:{}}}")
    private Map<String, String> credentialPartnerExpression;

    @Value("${config.server.file.storage.uri}")
    private String configServerFileStorageURL;

    /**
     * This map will hold the actual field names after resolving, using mapping JSON as keys and
     * configured field names as values
     */
    private Map<String, String> requiredIDObjectFieldNamesMap;
    /**
     * Configured Id object fields
     */
    private List<String> requiredIdObjectFieldNames;

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

    public List<CredentialPartner> getCredentialPartners(String regId, String registrationType, JSONObject identity) throws PacketManagerException, JSONException, ApisResourceAccessException, IOException, JsonProcessingException {

        regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                regId, "CredentialPartnerUtil::getCredentialPartners()::entry");

        List<String> filteredPartners = new ArrayList<>();
        if (credentialPartnerExpression == null || credentialPartnerExpression.isEmpty()) {
            regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
                    LoggerFileConstant.REGISTRATIONID.toString(), regId,
                    PlatformErrorMessages.RPR_PRT_ISSUER_NOT_FOUND_IN_PROPERTY.name());
            return Lists.emptyList();
        }

        Map<String, Object> identityFieldValueMap = new HashMap<>();
                requiredIdObjectFieldNames.forEach(field -> identityFieldValueMap.put(field, JsonUtil.getJSONValue(identity, field)));

        Map<String, Object> context = new HashMap<>();
        for (Map.Entry<String, Object> identityAttribute: identityFieldValueMap.entrySet()) {
            JSONObject attributeObject = new JSONObject(identityFieldValueMap);
            try {
                if (identityAttribute.getKey() != null && identityAttribute.getValue() != null) {
                    Object obj = attributeObject.get(identityAttribute.getKey());
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
                } else
                    context.put(identityAttribute.getKey(), identityAttribute.getValue());
            } catch (Exception e) {
                regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
                        ExceptionUtils.getStackTrace(e));
                throw new ParsingException(PlatformErrorMessages.RPR_PRT_DATA_VALIDATION_FAILED.getCode(), e);
            }
        }

        // adding additional metadata so that it can be used for MVEL expression
        Map<String, String> metaInfo = utilities.getPacketManagerService().getMetaInfo(regId, registrationType, ProviderStageName.CREDENTIAL_REQUESTOR);
        if (MapUtils.isNotEmpty(metaInfo)) {
            String metadata = metaInfo.get(JsonConstant.METADATA);
            if (!StringUtils.isEmpty(metadata)) {
                JSONArray jsonArray = new JSONArray(metadata);
                addToMap(jsonArray, context);
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

        List<CredentialPartner> finalList = new ArrayList<>();
        if (!filteredPartners.isEmpty()) {
            filteredPartners.forEach(
                    p -> {
                        Optional<CredentialPartner> partner = credentialPartners.getPartners()
                                .stream().filter(pr -> pr.getId().equalsIgnoreCase(p)).findAny();
                        if (partner.isPresent())
                            finalList.add(partner.get());
                    });
        }
        return finalList;
    }

    private void addToMap(JSONArray jsonArray, Map<String, Object> allMap) throws JSONException, IOException {
        for (int i =0; i < jsonArray.length(); i++) {
            org.json.JSONObject jsonObject = (org.json.JSONObject) jsonArray.get(i);
            FieldValue fieldValue = objectMapper.readValue(jsonObject.toString(), FieldValue.class);
            allMap.put(fieldValue.getLabel(), fieldValue.getValue());
        }
    }

    @PostConstruct
    public void loadPartnerDetails() throws RegistrationProcessorCheckedException {
        try {
            String partners = Utilities.getJson(configServerFileStorageURL, partnerProfileFileName);
            credentialPartners = JsonUtil.readValueWithUnknownProperties(partners, CredentialPartnersList.class);
        } catch (Exception e) {
            regProcLogger.error("Error loading credential Partners", e);
            throw new RegistrationProcessorCheckedException(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode(),
                    PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage(), e);
        }
    }

    public CredentialPartnersList getAllCredentialPartners() throws RegistrationProcessorCheckedException {
        if (credentialPartners == null)
            loadPartnerDetails();
        return credentialPartners;
    }
}
