package io.mosip.registration.processor.core.packet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RegisteredDevice {

    private String deviceCode;
    private String deviceServiceVersion;
    private DigitalId digitalId;
}
