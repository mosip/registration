package io.mosip.registration.processor.status.dto;

import io.mosip.kernel.core.http.ResponseWrapper;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 
 * @author Girish Yarru
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "Model representing a Crypto-Manager-Service Response")
public class CryptomanagerResponseDto extends ResponseWrapper<DecryptResponseDto> {

}
