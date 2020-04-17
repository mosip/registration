package io.mosip.registration.entity;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This Entity Class contains list of dynamic fields  
 * which will be displayed in UI with respect to language code.
 * The data for this table will come through sync from server master table 
 * 
 * @author Taleev.Aalam
 * @version 1.0.9
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(schema = "reg", name = "dynamic_field")
public class DynamicField {
	
	@Id
	@Column(name="id")
	private String id;

	@Column(name="name")
	private String name;

	@Column(name="description")
	private String description;
	
	@Column(name="lang_code")
	private String langCode;
	
	@Column(name="data_type")
	private String dataType;
	
	@Column(name="value_json")
	private String valueJson;
	
	@Column(name="is_active")
	private boolean isActive;
}

