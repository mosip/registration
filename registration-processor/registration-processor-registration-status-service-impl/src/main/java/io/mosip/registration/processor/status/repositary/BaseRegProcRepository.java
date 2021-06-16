package io.mosip.registration.processor.status.repositary;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;
import io.mosip.registration.processor.status.entity.BasePacketEntity;
import io.mosip.registration.processor.status.entity.SubWorkflowMappingEntity;

@Repository
public interface BaseRegProcRepository<T extends BasePacketEntity, E> extends BaseRepository<T, E> {

    @Query("SELECT subWorkflow FROM SubWorkflowMappingEntity subWorkflow WHERE subWorkflow.id.regId =:regId")
    public List<SubWorkflowMappingEntity> getSubWorkflowMapping(@Param("regId") String regId);

    @Query("SELECT subWorkflow FROM SubWorkflowMappingEntity subWorkflow WHERE subWorkflow.id.additionalInfoReqId =:additionalInfoReqId")
    public List<SubWorkflowMappingEntity> workflowMappingByReqId(@Param("additionalInfoReqId") String additionalInfoReqId);

    @Query("SELECT subWorkflow FROM SubWorkflowMappingEntity subWorkflow WHERE subWorkflow.id.regId =:regId AND subWorkflow.process =:process AND subWorkflow.iteration=:iteration")
    public List<SubWorkflowMappingEntity> workflowMappingByRegIdAndProcessAndIteration(@Param("regId") String regId, @Param("process") String process, @Param("iteration")  int iteration);

	@Query("SELECT subWorkflow FROM SubWorkflowMappingEntity subWorkflow WHERE subWorkflow.id.regId =:regId AND subWorkflow.process =:process order by subWorkflow.iteration desc")
	public List<SubWorkflowMappingEntity> workflowMappingByRegIdAndProcess(@Param("regId") String regId,
			@Param("process") String process);
}
