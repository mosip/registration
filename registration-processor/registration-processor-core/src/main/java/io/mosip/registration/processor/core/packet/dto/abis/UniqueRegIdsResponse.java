package io.mosip.registration.processor.core.packet.dto.abis;


import lombok.Data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Data
public class UniqueRegIdsResponse implements Serializable {
    private Set<String> response=new HashSet<>();
    private Boolean isResponceNull=false;

    @Override
    public String toString() {
        return "UniqueRegIdsMethodResponse{" +
                "responce=" + response +
                ", isResponceNull=" + isResponceNull +
                '}';
    }
}