package io.mosip.registration.processor.biodedupe.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.biodedupe.stage.exception.CbeffNotFoundException;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;

@Service
public class CbeffValidateAndVerificatonService {
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
    private PriorityBasedPacketManagerService priorityBasedPacketManagerService;


    public void validateBiometrics(String id, String process)
            throws ApisResourceAccessException, IOException, PacketManagerException, JsonProcessingException {
    	regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				id, "CbeffValidateAndVerificatonService::validateBiometrics()::entry");
        JSONObject regProcessorIdentityJson = utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
        String individualBiometricsLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(
                regProcessorIdentityJson, MappingJsonConstants.INDIVIDUAL_BIOMETRICS), MappingJsonConstants.VALUE);

        BiometricRecord biometricRecord = priorityBasedPacketManagerService.getBiometrics(id, individualBiometricsLabel,
        		mandatoryModalities, process, ProviderStageName.BIO_DEDUPE);

        Set<String> availableModalities = biometricRecord != null && !CollectionUtils.isEmpty(biometricRecord.getSegments()) ?
                biometricRecord.getSegments().stream().map(b -> {
                	if(!CollectionUtils.isEmpty(b.getBdbInfo().getType())) {
                		if (b.getOthers() != null) {
                            for(Map.Entry entry : b.getOthers().entrySet()) {
                                if(entry.getKey().equals("EXCEPTION") &&!entry.getValue().equals("true")) {
                                    return b.getBdbInfo().getSubtype()!=null && !b.getBdbInfo().getSubtype().isEmpty()?
                                            String.join(" ", b.getBdbInfo().getSubtype())
                                            :b.getBdbInfo().getType().get(0).value();
                                }
                            }
                        } else {
                            return b.getBdbInfo().getSubtype()!=null && !b.getBdbInfo().getSubtype().isEmpty()?
                                    String.join(" ", b.getBdbInfo().getSubtype())
                                    :b.getBdbInfo().getType().get(0).value();
                        }
                	}
                	return null;
                }).collect(Collectors.toSet()) : null;

        boolean isBiometricsNotPresent = availableModalities != null ? mandatoryModalities.stream().noneMatch(
                modalities -> availableModalities.contains(modalities)) : true;

        if (isBiometricsNotPresent)
            throw new CbeffNotFoundException("Biometrics not found for : " + id);
        
        regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
    			id, "CbeffValidateAndVerificatonService::validateBiometrics()::exit" );
    }
    
}
