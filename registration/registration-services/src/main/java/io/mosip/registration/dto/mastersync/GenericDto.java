package io.mosip.registration.dto.mastersync;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenericDto {

	private String code;
	private String name;
	private String langCode;
}
