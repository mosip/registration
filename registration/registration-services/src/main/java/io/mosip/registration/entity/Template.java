package io.mosip.registration.entity;

import java.sql.Date;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

import io.mosip.registration.entity.id.TemplateId;
import lombok.Getter;
import lombok.Setter;

/**
 * The Entity Class for Template details
 * 
 * @author Himaja Dhanyamraju
 * @since 1.0.0
 */
@Entity
@IdClass(TemplateId.class)
@Table(schema = "reg", name = "TEMPLATE")
@Getter
@Setter
public class Template extends RegistrationCommonFields {

	@Id
	@AttributeOverrides({ @AttributeOverride(name = "id", column = @Column(name = "id")),
			@AttributeOverride(name = "langCode", column = @Column(name = "lang_code")) })

	private String id;
	private String langCode;

	@Column(name = "name")
	private String name;
	@Column(name = "file_format_code")
	private String fileFormatCode;
	private String model;
	@Column(name = "file_txt", length = 4000)
	private String fileText;
	@Column(name = "module_id")
	private String moduleId;
	@Column(name = "module_name")
	private String moduleName;
	@Column(name = "template_typ_code")
	private String templateTypeCode;
	
	@Column(name="descr")
	private String description;
	@Column(name="is_deleted")
	private Boolean isDeleted;
	@Column(name="del_dtimes")
	private Date delDtimes;



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fileFormatCode == null) ? 0 : fileFormatCode.hashCode());
		result = prime * result + ((fileText == null) ? 0 : fileText.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((langCode == null) ? 0 : langCode.hashCode());
		result = prime * result + ((model == null) ? 0 : model.hashCode());
		result = prime * result + ((moduleId == null) ? 0 : moduleId.hashCode());
		result = prime * result + ((moduleName == null) ? 0 : moduleName.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((templateTypeCode == null) ? 0 : templateTypeCode.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Template other = (Template) obj;
		if (fileFormatCode == null) {
			if (other.fileFormatCode != null)
				return false;
		} else if (!fileFormatCode.equals(other.fileFormatCode))
			return false;
		if (fileText == null) {
			if (other.fileText != null)
				return false;
		} else if (!fileText.equals(other.fileText))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (langCode == null) {
			if (other.langCode != null)
				return false;
		} else if (!langCode.equals(other.langCode))
			return false;
		if (model == null) {
			if (other.model != null)
				return false;
		} else if (!model.equals(other.model))
			return false;
		if (moduleId == null) {
			if (other.moduleId != null)
				return false;
		} else if (!moduleId.equals(other.moduleId))
			return false;
		if (moduleName == null) {
			if (other.moduleName != null)
				return false;
		} else if (!moduleName.equals(other.moduleName))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (templateTypeCode == null) {
			if (other.templateTypeCode != null)
				return false;
		} else if (!templateTypeCode.equals(other.templateTypeCode))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Template [id=" + id + ", name=" + name + ", descr=" + description + ", file_format_code=" + fileFormatCode
				+ ", model=" + model + ", file_txt=" + fileText + ", module_id=" + moduleId + ", module_name="
				+ moduleName + ", template_typ_code=" + templateTypeCode + ", lang_code=" + langCode + ", is_active="
				+ getIsActive() + ", cr_by=" + crBy + ", cr_dtimes=" + getCrDtime() + ", upd_by=" + updBy + ", upd_dtimes="
				+ updDtimes + ", is_deleted=" + isDeleted + ", del_dtimes=" + delDtimes + "]";
	}

}
