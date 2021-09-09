package io.mosip.registration.processor.core.anonymous.dto;

import java.util.List;

import lombok.Data;

@Data
public class AnonymousProfileDTO {

	private String processName;
	private String processStage;
	private String date;
	private String startDateTime;
	private String endDateTime;
	private String yearOfBirth;
	private String gender;
	private List<String> location;
	private List<String> preferredLanguages;
	private String phone;
	private String email;
	private List<Object> exceptions;
	private List<String> verified;
	private List<Object> biometricInfo;
	private Object device;
	private List<String> documents;
	private List<String> assisted;
	private String enrollmentCenterId;
	private String status;
}
