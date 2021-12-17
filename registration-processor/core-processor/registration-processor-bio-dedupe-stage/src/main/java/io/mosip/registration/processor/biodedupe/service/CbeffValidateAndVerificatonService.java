package io.mosip.registration.processor.biodedupe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.biodedupe.stage.exception.CbeffNotFoundException;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.PolicyConstant;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.datashare.Filter;
import io.mosip.registration.processor.core.packet.dto.datashare.ShareableAttributes;
import io.mosip.registration.processor.core.packet.dto.datashare.Source;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CbeffValidateAndVerificatonService {

    private Map<String, List<String>> typeAndSubTypeMap = new HashMap<>();
    private static Logger regProcLogger = RegProcessorLogger.getLogger(CbeffValidateAndVerificatonService.class);

    @Value("${registration.processor.policy.id}")
    private String policyId;

    @Value("${registration.processor.subscriber.id}")
    private String subscriberId;

    /** The utilities. */
    @Autowired
    Utilities utilities;

    @Autowired
    private PacketManagerService packetManagerService;

    @Autowired
    private RegistrationProcessorRestClientService registrationProcessorRestClientService;

    @Autowired
    private PriorityBasedPacketManagerService priorityBasedPacketManagerService;


    public void validateBiometrics(String id, String process)
            throws ApisResourceAccessException, IOException, PacketManagerException, JsonProcessingException {

        Map<String, List<String>> typeAndSubtypMap = createTypeSubtypeMapping(id);
        List<String> modalities = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : typeAndSubtypMap.entrySet()) {
            if (entry.getValue() == null)
                modalities.add(entry.getKey());
            else
                modalities.addAll(entry.getValue());
        }

        JSONObject regProcessorIdentityJson = utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
        String individualBiometricsLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(
                regProcessorIdentityJson, MappingJsonConstants.INDIVIDUAL_BIOMETRICS), MappingJsonConstants.VALUE);

        BiometricRecord biometricRecord = priorityBasedPacketManagerService.getBiometrics(id, individualBiometricsLabel,
                modalities, process, ProviderStageName.BIO_DEDUPE);

        Set<String> availableTypes = biometricRecord != null && !CollectionUtils.isEmpty(biometricRecord.getSegments()) ?
                biometricRecord.getSegments().stream().map(b -> b.getBdbInfo().getType() != null ?
                b.getBdbInfo().getType().iterator().next().value() : null).collect(Collectors.toSet()) : null;

        boolean isBiometricsNotPresent = availableTypes != null ? typeAndSubtypMap.keySet().stream().noneMatch(
                type -> availableTypes.contains(type)) : true;

        if (isBiometricsNotPresent)
            throw new CbeffNotFoundException("Biometrics not found for : " + id);
    }

    public Map<String, List<String>> createTypeSubtypeMapping(String id) throws ApisResourceAccessException, CbeffNotFoundException,
            IOException, JsonProcessingException {

        // Call only once and use cache
        if (!CollectionUtils.isEmpty(typeAndSubTypeMap))
            return typeAndSubTypeMap;

        ResponseWrapper<?> policyResponse = (ResponseWrapper<?>) registrationProcessorRestClientService.getApi(
                ApiName.PMS, Lists.newArrayList(policyId, PolicyConstant.PARTNER_ID, subscriberId), "", "",
                ResponseWrapper.class);
        if (policyResponse == null || (policyResponse.getErrors() != null && policyResponse.getErrors().size() > 0)) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    id, JsonUtils.javaObjectToJsonString(policyResponse));
            throw new ApisResourceAccessException(policyResponse.getErrors().get(0).getMessage());

        } else {
            LinkedHashMap<String, Object> responseMap = (LinkedHashMap<String, Object>) policyResponse.getResponse();
            LinkedHashMap<String, Object> policies = (LinkedHashMap<String, Object>) responseMap
                    .get(PolicyConstant.POLICIES);
            List<?> attributes = (List<?>) policies.get(PolicyConstant.SHAREABLE_ATTRIBUTES);
            ObjectMapper mapper = new ObjectMapper();
            ShareableAttributes shareableAttributes = mapper.readValue(mapper.writeValueAsString(attributes.get(0)),
                    ShareableAttributes.class);
            for (Source source : shareableAttributes.getSource()) {
                List<Filter> filterList = source.getFilter();
                if (filterList != null && !filterList.isEmpty()) {

                    filterList.forEach(filter -> {
                        if (filter.getSubType() != null && !filter.getSubType().isEmpty())
                            typeAndSubTypeMap.put(filter.getType(), filter.getSubType());
                        else
                            typeAndSubTypeMap.put(filter.getType(), null);
                    });
                }
            }
        }
        return typeAndSubTypeMap;

    }
}
