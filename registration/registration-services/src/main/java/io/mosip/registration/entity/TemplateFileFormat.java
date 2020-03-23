package io.mosip.registration.entity;

import java.sql.Date;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * The Entity Class for Template FileFormat details
 * 
 * @author Himaja Dhanyamraju
 * @since 1.0.0
 */
@Entity
@Table(schema="reg", name = "TEMPLATE_FILE_FORMAT")
@Getter
@Setter
public class TemplateFileFormat extends RegistrationCommonFields {
	@EmbeddedId
	@Column(name="pk_tfft_code")
	private TemplateEmbeddedKeyCommonFields pkTfftCode;
	
	@Column(name="descr")
	private String description;
	@Column(name="is_deleted")
	private Boolean isDeleted;
	@Column(name="del_dtimes")
	private Date delDtimes;

}
