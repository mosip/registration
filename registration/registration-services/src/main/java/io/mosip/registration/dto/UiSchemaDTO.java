package io.mosip.registration.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class is a DTO which parses with UI Schema Json, which decides of how
 * the UI element should behave like whether it is a TextField / Password /
 * Button.
 * 
 * And also it contains the validators to validate the respective UI element,
 * label name,controlType, format,whether it is required or not.
 * 
 * @author YASWANTH S
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UiSchemaDTO {

	private String id;
	private boolean inputRequired;
	private String type;
	private int minimum;
	private int maximum;
	private String description;
	private HashMap<String, String> label;
	private String controlType;
	private String fieldType;
	private String format;
	private List<Validator> validators;
	private String fieldCategory;
	private String alignmentGroup;
	private RequiredOnExpr visible;
	@JsonProperty("required")
	private boolean isRequired;

	@JsonProperty("bioAttributes")
	private List<String> bioAttributes;

	@JsonProperty("requiredOn")
	private List<RequiredOnExpr> requiredOn;

	@JsonProperty("subType")
	private String subType;

	private String contactType;
	private String group;

}