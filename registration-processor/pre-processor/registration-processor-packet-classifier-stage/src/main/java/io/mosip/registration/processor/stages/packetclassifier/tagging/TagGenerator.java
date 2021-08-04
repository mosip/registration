package io.mosip.registration.processor.stages.packetclassifier.tagging;

import java.util.List;
import java.util.Map;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.registration.processor.stages.packetclassifier.dto.FieldDTO;

/**
 * This interface will outline all the methods that needs to be implemented by every tag generator
 * so they integrate loosly with the PacketCalassificationProcessor class
 *
 * @author Vishwanath V
 */
public interface TagGenerator {

    /**
     * Gives back the list of Id Object field names, whos value is required by the tag generator 
     * implementation to generate tags
     * @return list of ID object field names
     * @throws BaseCheckedException All exception cases will the handled in tag generator implemetations and will be
     * wrapped in BaseCheckedException and thrown
     */
    List<String> getRequiredIdObjectFieldNames() throws BaseCheckedException;
    
    /**
     * Generates the tags for a packet
     * @param registrationId of the packet
     * @param process Current process of the packet
     * @param idObjectFieldDTOMap Map containing the values and types for the required identity fields
     * @param metaInfoMap Map containing all the metaInfo of a packet
     * @return Generated tags as a map
     * @throws BaseCheckedException All exception cases will the handled in tag generator implemetations and will be
     * wrapped in BaseCheckedException and thrown
     */
    Map<String,String> generateTags(String workflowInstanceId, String registrationId, String process, 
        Map<String, FieldDTO> idObjectFieldDTOMap, Map<String, String> metaInfoMap, int iteration) 
            throws BaseCheckedException;
    
}
