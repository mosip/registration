package io.mosip.registration.processor.packet.utility.dto;

import io.mosip.kernel.core.http.ResponseWrapper;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 
 * @author Sowmya
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "Model representing a Crypto-Manager-Service Response")
public class CryptomanagerResponseDto extends ResponseWrapper<DecryptResponseDto> {

}
