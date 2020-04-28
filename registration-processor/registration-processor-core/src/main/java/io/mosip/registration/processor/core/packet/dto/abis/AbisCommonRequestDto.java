package io.mosip.registration.processor.core.packet.dto.abis;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import lombok.Data;

@Data
public class AbisCommonRequestDto implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -7080424253600088998L;

	/** The id. */
	private String id;
	
	/** The ver. */
	private String ver;
	
	/** The request id. */
	private String requestId;
	
	
	private String requesttime;
	
	/** The reference id. */
	private String referenceId;
}
