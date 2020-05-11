package io.mosip.registration.processor.packet.utility.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class PasswordRequest {
	public String appId;
	public String password;
	public String userName;
}
