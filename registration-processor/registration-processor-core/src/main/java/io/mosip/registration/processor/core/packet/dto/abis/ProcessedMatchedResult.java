package io.mosip.registration.processor.core.packet.dto.abis;


import lombok.Data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Data
public class ProcessedMatchedResult implements Serializable {
    private Set<String> matchedResults = new HashSet<>();
    private boolean biometricMatchedForPacketUIN;

    @Override
    public String toString() {
        return "ProcessedMatchedResult : biometricMatchedForPacketUIN : " + biometricMatchedForPacketUIN
                + ":: matchedResults :" + matchedResults;
    }
}