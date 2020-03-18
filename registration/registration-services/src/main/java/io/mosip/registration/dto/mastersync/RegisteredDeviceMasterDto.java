package io.mosip.registration.dto.mastersync;

import java.sql.Timestamp;

import io.mosip.registration.constants.RegistrationConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class RegisteredDeviceMasterDto extends MasterSyncBaseDto {

	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getDtypeCode() {
		return dtypeCode;
	}
	public void setDtypeCode(String dtypeCode) {
		this.dtypeCode = dtypeCode;
	}
	public String getDsTypeCode() {
		return dsTypeCode;
	}
	public void setDsTypeCode(String dsTypeCode) {
		this.dsTypeCode = dsTypeCode;
	}
	public String getStatusCode() {
		return statusCode;
	}
	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}
	public String getDeviceId() {
		return deviceId;
	}
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	public String getDeviceSubId() {
		return deviceSubId;
	}
	public void setDeviceSubId(String deviceSubId) {
		this.deviceSubId = deviceSubId;
	}
	public String getDigitalId() {
		return digitalId;
	}
	public void setDigitalId(String digitalId) {
		this.digitalId = digitalId;
	}
	public String getSerialNumber() {
		return serialNumber;
	}
	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}
	public String getProviderId() {
		return providerId;
	}
	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}
	public String getProviderName() {
		return providerName;
	}
	public void setProviderName(String providerName) {
		this.providerName = providerName;
	}
	public String getPurpose() {
		return purpose;
	}
	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}
	public String getFirmware() {
		return firmware;
	}
	public void setFirmware(String firmware) {
		this.firmware = firmware;
	}
	public String getMake() {
		return make;
	}
	public void setMake(String make) {
		this.make = make;
	}
	public String getModel() {
		return model;
	}
	public void setModel(String model) {
		this.model = model;
	}
	public Timestamp getExpiryDate() {
		return expiryDate;
	}
	public void setExpiryDate(Timestamp expiryDate) {
		this.expiryDate = expiryDate;
	}
	public String getCertificationLevel() {
		return certificationLevel;
	}
	public void setCertificationLevel(String certificationLevel) {
		this.certificationLevel = certificationLevel;
	}
	public String getFoundationalTrustSignature() {
		return foundationalTrustSignature;
	}
	public void setFoundationalTrustSignature(String foundationalTrustSignature) {
		this.foundationalTrustSignature = foundationalTrustSignature;
	}
	public String getFoundationalTPId() {
		return foundationalTPId;
	}
	public void setFoundationalTPId(String foundationalTPId) {
		this.foundationalTPId = foundationalTPId;
	}
	public byte[] getFoundationalTrustCertificate() {
		if(foundationalTrustCertificate!=null)
			return foundationalTrustCertificate.getBytes();
		return RegistrationConstants.NULL_VECTOR;
	}
	public void setFoundationalTrustCertificate(String foundationalTrustCertificate) {
		this.foundationalTrustCertificate = foundationalTrustCertificate;
	}
	public String getDproviderSignature() {
		return dproviderSignature;
	}
	public void setDproviderSignature(String dproviderSignature) {
		this.dproviderSignature = dproviderSignature;
	}
	public Boolean getIsActive() {
		return isActive;
	}
	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}
	public String getCrBy() {
		return crBy;
	}
	public void setCrBy(String crBy) {
		this.crBy = crBy;
	}
	public Timestamp getCrDtime() {
		return crDtime;
	}
	public void setCrDtime(Timestamp crDtime) {
		this.crDtime = crDtime;
	}
	public String getUpdatedBy() {
		return updatedBy;
	}
	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}
	public Timestamp getUpdatedDateTimes() {
		return updatedDateTimes;
	}
	public void setUpdatedDateTimes(Timestamp updatedDateTimes) {
		this.updatedDateTimes = updatedDateTimes;
	}
	public Timestamp getDelDTimes() {
		return delDTimes;
	}
	public void setDelDTimes(Timestamp delDTimes) {
		this.delDTimes = delDTimes;
	}
	private String code;
	private String dtypeCode;
	private String dsTypeCode;
	private String statusCode;
	private String deviceId;
	private String deviceSubId;
	private String digitalId;
	private String serialNumber;
	private String providerId;
	private String providerName;
	private String purpose;
	private String firmware;
	private String make;
	private String model;
	private Timestamp expiryDate;
	private String certificationLevel;
	private String foundationalTrustSignature;
	private String foundationalTPId;
	private String foundationalTrustCertificate;
	private String dproviderSignature;
	private Boolean isActive;
	private String crBy;
	private Timestamp crDtime;
	private String updatedBy;
	private Timestamp updatedDateTimes;
	private Timestamp delDTimes;
}
