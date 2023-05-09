package io.mosip.registration.processor.print.stage;

import org.json.simple.JSONObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

import java.util.List;

@ConditionalOnClass(name = "PrintPartnerServiceImpl")
public interface PrintPartnerService {
    List<String> getPrintPartners(String regId, JSONObject identity);
}
