package io.mosip.registration.processor.status.service;

import java.util.List;

import org.springframework.stereotype.Service;

import io.mosip.registration.processor.status.dto.PacketExternalStatusDTO;


/**
 * The Interface PacketExternalStatusService.
 */
@Service
public interface PacketExternalStatusService {

	/**
	 * Gets the by packet ids.
	 *
	 * @param packetIdList the packet id list
	 * @return the by packet ids
	 */
	public List<PacketExternalStatusDTO> getByPacketIds(List<String> packetIdList);
}
