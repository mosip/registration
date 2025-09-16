package io.mosip.registration.processor.core.packet.dto.abis;


import lombok.Data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Data
public class UniqueRegistrationIds implements Serializable {
    private Set<String> registrationIds = new HashSet<>();
    private Boolean isPacketUINMatched;

    @Override
    public String toString() {
        return "UniqueRegistrationIds : isPacketUINMatched : " + isPacketUINMatched
                + ":: registrationIds :" + registrationIds;
    }
}