package io.mosip.registration.packetmanager.spi;

import java.util.List;
import java.util.Map;

import io.mosip.registration.packetmananger.dto.AuditDto;
import io.mosip.registration.packetmananger.dto.BiometricsDto;
import io.mosip.registration.packetmananger.dto.DocumentDto;
import io.mosip.registration.packetmananger.dto.SimpleDto;
import io.mosip.registration.packetmananger.dto.metadata.ModalityException;
import io.mosip.registration.packetmananger.exception.PacketCreatorException;


public interface PacketCreator {	
			
	public void initialize();
	
	public void setField(String fieldName, Object value);
	
	public void setField(String fieldName, List<SimpleDto> value);		
	
	public void setBiometric(String fieldName, List<BiometricsDto> value);	
	
	public void setDocument(String fieldName, DocumentDto value);
	
	public void setMetaInfo(String key, String value);	
	
	public void setBiometricException(String fieldName, List<ModalityException> modalityExceptions);
	
	public void setAudits(List<AuditDto> auditList);
	
	public void setAcknowledgement(String acknowledgeReceiptName, byte[] acknowledgeReceipt);
	
	public boolean isPacketCreatorInitialized();
	
	public byte[] createPacket(String registrationId, double version,  String schemaJson, 
			Map<String,String> categoryPacketMapping, byte[] publicKey, PacketSigner signer) throws PacketCreatorException;

}
