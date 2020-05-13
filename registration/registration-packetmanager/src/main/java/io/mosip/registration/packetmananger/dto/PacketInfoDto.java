package io.mosip.registration.packetmananger.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.mosip.registration.packetmananger.datatype.SimpleType;
import io.mosip.registration.packetmananger.dto.metadata.DeviceMetaInfo;
import io.mosip.registration.packetmananger.dto.metadata.FieldValue;
import io.mosip.registration.packetmananger.dto.metadata.BiometricsException;
import lombok.Data;


@Data
public class PacketInfoDto {	
	
	private String registrationId;
	private double idSchemaVersion;
	private String creationDate;
	private Map<String, Object> demographics;
	private Map<String, DocumentDto> documents;
	private Map<String, List<BiometricsDto>> biometrics;
	private Map<String, List<BiometricsException>> exceptionBiometrics;
	private List<FieldValue> metaData;
	private List<FieldValue> operationsData;
	private List<DeviceMetaInfo> capturedRegisteredDevices;
	private List<FieldValue> capturedNonRegisteredDevices;
	private List<FieldValue> checkSum;
	private List<SimpleDto> printingName;	
	private List<BiometricsDto> officerBiometrics;
	private List<BiometricsDto> supervisorBiometrics;	
	private List<AuditDto> audits;	
	private byte[] acknowledgeReceipt;
	private String acknowledgeReceiptName;
		
	public PacketInfoDto() {
		this.demographics = new HashMap<String, Object>();
		this.documents = new HashMap<String, DocumentDto>();
		this.biometrics = new HashMap<String, List<BiometricsDto>>();
		this.exceptionBiometrics = new HashMap<String, List<BiometricsException>>();
		this.metaData = new ArrayList<FieldValue>();
		this.operationsData = new ArrayList<FieldValue>();
		this.checkSum = new ArrayList<FieldValue>();
	}
	
	public void setField(String fieldName, Object value) {
		this.demographics.put(fieldName, value);
	}
		
	public void setField(String fieldName, List<SimpleDto> value) {
		List<SimpleType> list = new ArrayList<SimpleType>();
		for(SimpleDto dto : value) {
			list.add(new SimpleType(dto.getLanguage(), dto.getValue()));
		}
		this.demographics.put(fieldName, list);
	}	
	
	public void setBiometricField(String fieldName, List<BiometricsDto> list) {		
		this.biometrics.put(fieldName, list);	
	}	
	
	public void setDocumentField(String fieldName, DocumentDto dto) {		
		this.documents.put(fieldName, dto);		
	}
	
	public void setExceptionBiometrics(String fieldName, List<BiometricsException> exceptionList) {
		this.exceptionBiometrics.put(fieldName, exceptionList);
	}
	
	public void setMetaData(String label, String value) {
		if(!this.metaData.contains(new FieldValue(label, value)))
			this.metaData.add(new FieldValue(label, value));
	}
	
	public void setMetaData(FieldValue fieldValue) {
		if(!this.metaData.contains(fieldValue))
			this.metaData.add(fieldValue);
	}
	
	public void setOperationsData(String key, String value) {
		if(!this.operationsData.contains(new FieldValue(key, value)))
			this.operationsData.add(new FieldValue(key, value));
	}
	
	public void addCheckSum(FieldValue fieldValue) {
		if(!this.checkSum.contains(fieldValue))
			this.checkSum.add(fieldValue);	
	}
	
}
