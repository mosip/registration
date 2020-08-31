package io.mosip.registration.processor.packet.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidatePacketResponse {

    private boolean isValid;
}
