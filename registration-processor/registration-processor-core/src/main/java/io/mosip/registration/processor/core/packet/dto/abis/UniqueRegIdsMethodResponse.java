package io.mosip.registration.processor.core.packet.dto.abis;

import lombok.Data;

import java.io.Serializable;
import java.util.Set;

@Data
public class UniqueRegIdsMethodResponse implements Serializable {
    private Set<String> response;
    private Boolean isResponceNull;

    @Override
    public String toString() {
        return "UniqueRegIdsMethodResponse{" +
                "responce=" + response +
                ", isResponceNull=" + isResponceNull +
                '}';
    }
}
