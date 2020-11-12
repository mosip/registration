package io.mosip.registration.processor.core.idrepo.dto;

import java.util.List;

import io.mosip.kernel.core.http.ResponseWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
public class VidsInfosDTO extends ResponseWrapper<List<VidInfoDTO>>{

	private List<VidInfoDTO> response;
}
