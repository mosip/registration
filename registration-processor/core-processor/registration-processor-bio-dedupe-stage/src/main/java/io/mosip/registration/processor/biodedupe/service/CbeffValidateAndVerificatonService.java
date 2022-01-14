package io.mosip.registration.processor.biodedupe.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.entities.Entry;
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

@Service
public class CbeffValidateAndVerificatonService {

    private Map<String, List<String>> typeAndSubTypeMap = new HashMap<>();
    private static Logger regProcLogger = RegProcessorLogger.getLogger(CbeffValidateAndVerificatonService.class);

    @Value("${registration.processor.policy.id}")
    private String policyId;

    @Value("${registration.processor.subscriber.id}")
    private String subscriberId;
    
    @Value("#{'${mosip.regproc.cbeff-validation.mandatory.modalities:Right,Left,Left RingFinger,Left LittleFinger,"
    		+ "Right RingFinger,Left Thumb,Left IndexFinger,Right IndexFinger,Right LittleFinger,Right MiddleFinger,"
    		+ "Left MiddleFinger,Right Thumb,Face}'.split(',')}")
	private List<String> mandatoryModalities ;

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
        

        JSONObject regProcessorIdentityJson = utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
        String individualBiometricsLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(
                regProcessorIdentityJson, MappingJsonConstants.INDIVIDUAL_BIOMETRICS), MappingJsonConstants.VALUE);

        BiometricRecord biometricRecord = priorityBasedPacketManagerService.getBiometrics(id, individualBiometricsLabel,
        		mandatoryModalities, process, ProviderStageName.BIO_DEDUPE);

        Set<String> availableModalities = biometricRecord != null && !CollectionUtils.isEmpty(biometricRecord.getSegments()) ?
                biometricRecord.getSegments().stream().map(b -> {
                	if(b.getBdbInfo().getType()!=null || !b.getBdbInfo().getType().isEmpty()) {
                		for(Entry entry:b.getOthers()) {
                			if(entry.getKey().equals("EXCEPTION") &&!entry.getValue().equals("true")) {
                				return b.getBdbInfo().getSubtype()!=null ||!b.getBdbInfo().getSubtype().isEmpty()?
                						String.join(" ", b.getBdbInfo().getSubtype())
                						:b.getBdbInfo().getType().iterator().next().value();
                			}
                		}
                	}
                	return null;
                }).collect(Collectors.toSet()) : null;

        boolean isBiometricsNotPresent = availableModalities != null ? mandatoryModalities.stream().noneMatch(
                modalities -> availableModalities.contains(modalities)) : true;

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
