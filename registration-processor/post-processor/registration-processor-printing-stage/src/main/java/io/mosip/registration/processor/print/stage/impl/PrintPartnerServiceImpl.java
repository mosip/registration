package io.mosip.registration.processor.print.stage.impl;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.demographicinfo.JsonValue;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.print.stage.PrintPartnerService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PrintPartnerServiceImpl implements PrintPartnerService {

    private static final String PRINT_ISSUER = "mosip.registration.processor.print.issuer.map";
    private static final String PRINT_ISSUER_ATTRIBUTE = "mosip.registration.processor.print.issuer.identification.attribute";
    private static final String SEPARATOR = ":";
    private static final String DELIMITER = ",";

    private static final Logger regProcLogger = RegProcessorLogger.getLogger(PrintPartnerServiceImpl.class);

    @Autowired
    private Environment env;

    @Override
    public List<String> getPrintPartners(String regId, JSONObject identity) {
        regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                regId, "PrintPartnerServiceImpl::getPrintPartners()::entry");
        Map<String, String> printPartnerMap = new HashMap();
        List<String> filteredPartners = new ArrayList();
        String configuredPrintIssuers = env.getProperty(PRINT_ISSUER);
        if(configuredPrintIssuers == null) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                    LoggerFileConstant.REGISTRATIONID.toString(), regId,
                    PlatformErrorMessages.RPR_PRT_ISSUER_NOT_FOUND_IN_PROPERTY.name());
            return filteredPartners;
        }
        regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                regId, "PrintPartnerServiceImpl::getPrintPartners()::" + configuredPrintIssuers);

        for (String issuer: configuredPrintIssuers.split(DELIMITER)) {
            printPartnerMap.put(issuer.split(SEPARATOR)[0], issuer.split(SEPARATOR)[1]);
        }
        regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                regId, "PrintPartnerServiceImpl::getPrintPartners()::Configured print partners are fetched and mapped");

        var identityValues = getIdJsonByAttribute(identity, env.getProperty(PRINT_ISSUER_ATTRIBUTE));
        for( String printPartner : printPartnerMap.keySet()) {
            if(printPartnerMap.get(printPartner).equals("ALL")) {
                filteredPartners.add(printPartner);
            } else {
                for (String value : identityValues) {
                    if(value.equals(printPartnerMap.get(printPartner))){
                        filteredPartners.add(printPartner);
                    }
                }
            }
        }
        return filteredPartners;
    }

    private List<String> getIdJsonByAttribute(JSONObject demographicJsonIdentity, String attribute) {
        regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                "", "PrintPartnerServiceImpl::getIdJsonByAttribute()::entry");
        List<String> identityValues = new ArrayList<>();

        Object jsonObject = JsonUtil.getJSONValue(demographicJsonIdentity, attribute);
            if (jsonObject instanceof ArrayList) {
                JSONArray node = JsonUtil.getJSONArray(demographicJsonIdentity, attribute);
                JsonValue[] jsonValues = JsonUtil.mapJsonNodeToJavaObject(JsonValue.class, node);
                if (jsonValues != null)
                    for (int count = 0; count < jsonValues.length; count++) {
                        identityValues.add(jsonValues[count].getValue());
                    }
            } else if (jsonObject instanceof LinkedHashMap) {
                JSONObject json = JsonUtil.getJSONObject(demographicJsonIdentity, attribute);
                if (json != null)
                    identityValues.add(json.get("value").toString());
            } else {
                if (jsonObject != null)
                    identityValues.add(jsonObject.toString());
            }
        return identityValues;
    }

}
