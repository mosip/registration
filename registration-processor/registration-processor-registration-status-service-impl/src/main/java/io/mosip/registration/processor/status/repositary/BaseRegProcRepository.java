package io.mosip.registration.processor.status.repositary;

import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;
import io.mosip.registration.processor.status.entity.BasePacketEntity;
import io.mosip.registration.processor.status.entity.BaseRegistrationEntity;
import io.mosip.registration.processor.status.entity.SubWorkflowMappingEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BaseRegProcRepository<T extends BasePacketEntity, E> extends BaseRepository<T, E> {

    @Query("SELECT subWorkflow FROM SubWorkflowMappingEntity subWorkflow WHERE subWorkflow.id.regId =:regId")
    public List<SubWorkflowMappingEntity> getSubWorkflowMapping(@Param("regId") String regId);

    @Query("SELECT subWorkflow FROM SubWorkflowMappingEntity subWorkflow WHERE subWorkflow.id.additionalInfoReqId =:additionalInfoReqId")
    public List<SubWorkflowMappingEntity> workflowMappingByReqId(@Param("additionalInfoReqId") String additionalInfoReqId);

    @Query("SELECT subWorkflow FROM SubWorkflowMappingEntity subWorkflow WHERE subWorkflow.id.regId =:regId and subWorkflow.process=:process and subWorkflow.iteration=:iteration")
	public List<SubWorkflowMappingEntity> workflowMappingByRIdAndProcessAndIteration(@Param("regId")String regId,
			@Param("process")String process, @Param("iteration")int iteration);
}
