package io.mosip.registration.processor.print.stage;

import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import org.json.simple.JSONObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@ConditionalOnClass(name = "PrintPartnerServiceImpl")
public interface PrintPartnerService {
    List<String> getPrintPartners(String regId, JSONObject identity);
}
