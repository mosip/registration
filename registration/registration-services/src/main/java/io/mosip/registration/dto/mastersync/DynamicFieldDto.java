package io.mosip.registration.dto.mastersync;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DynamicFieldDto {

	private String id;
	private String name;
	private String dataType;
	private List<DynamicFieldValueDto> fieldVal;
	private String langCode;
	private boolean isActive;
	private boolean isDeleted;

	@JsonProperty("isActive")
	public void setIsActiveFlag(boolean active) {
		this.setActive(active);
	}
}
