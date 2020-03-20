package io.mosip.registration.entity;

import java.sql.Date;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * The Entity Class for TemplateType details
 * 
 * @author Himaja Dhanyamraju
 * @since 1.0.0
 */
@Entity
@Table(schema="reg", name = "TEMPLATE_TYPE")
public class TemplateType extends RegistrationCommonFields {
	@EmbeddedId
	@Column(name="pk_tmplt_code")
	private TemplateEmbeddedKeyCommonFields pkTmpltCode;
	
	@Column(name="descr")
	private String descr;
	@Column(name="is_deleted")
	private Boolean isDeleted;
	@Column(name="del_dtimes")
	private Date delDtimes;

	/**
	 * @return the pkTmpltCode
	 */
	public TemplateEmbeddedKeyCommonFields getPkTmpltCode() {
		return pkTmpltCode;
	}

	/**
	 * @param pkTmpltCode the pkTmpltCode to set
	 */
	public void setPkTmpltCode(TemplateEmbeddedKeyCommonFields pkTmpltCode) {
		this.pkTmpltCode = pkTmpltCode;
	}

	public String getDescr() {
		return descr;
	}

	public void setDescr(String descr) {
		this.descr = descr;
	}

	public Boolean getIsDeleted() {
		return isDeleted;
	}

	public void setIsDeleted(Boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	public Date getDelDtimes() {
		return delDtimes;
	}

	public void setDelDtimes(Date delDtimes) {
		this.delDtimes = delDtimes;
	}

	
}
