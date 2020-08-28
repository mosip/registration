package io.mosip.registration.processor.status.dto;

import java.io.Serializable;

public class RegistrationAdditionalInfoDTO implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5493632810187324004L;
	
	private String name;
	private String phone;
	private String email;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	
}