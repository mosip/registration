package io.mosip.registration.processor.util;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.SyncRegistrationService; 
@Component
class LandingZoneUtility{
	private static Logger regProcLogger = RegProcessorLogger.getLogger(LandingZoneUtility.class);
	@Value("${landing.zone.account.name}")
	private String landingZoneAccount;
		
	@Value("${landing.zone.type:ObjectStore}")
	private String landingZoneType;
	
	@Value("${registration.processor.packet.ext}")
    private String extention;
	
	@Value("${landing.zone.move.schedule:0 0 0 * * *}")
    private static  String schedule;
	
	 @Autowired
	 private ObjectStoreAdapter objectStoreAdapter;

	    /**
	     * The sync registration service.
	     */
	 @Autowired
	 private SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationService;

	    /**
	     * The registration status service.
	     */
	 @Autowired
	 private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	 @Autowired
	 private RegistrationProcessorRestClientService restClient;
	 
	 @Scheduled(fixedDelayString = "${mosip.regproc.landing.zone.fixed.delay.millisecs:43200000}",
	            initialDelayString = "${mosip.regproc.landing.zone.inital.delay.millisecs:300000}")
	   public void movePacketsToObjectStore() {
		System.out.println("in utility");
		if(landingZoneType.equalsIgnoreCase("ObjectStore")) {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
					"", "PacketUploaderServiceImpl::movePacketsToObjectStore()::entry");
			List<String> regIdList=registrationStatusService.getAllRegistrationIds();
			for(String regId:regIdList) {
				List<String> packetIdList=syncRegistrationService.getAllPacketIds(regId);
				for(String packetId:packetIdList) {
					 List<String> pathSegment = new ArrayList<>();
				     pathSegment.add(packetId + extention);
					
						try {
							byte[] packet = (byte[]) restClient.getApi(ApiName.NGINXDMZURL, pathSegment, "", null, byte[].class);
							if(packet!=null) {
							 boolean result =objectStoreAdapter.putObject(landingZoneAccount, regId, null, null, packetId, new ByteArrayInputStream(packet));
							 if(!result) {
								 regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
										 regId, packetId+":Packet store not accesible");
							 }
							 else {
								 regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
										 regId, packetId+":Packet has been moved to landing zoneobject store ");
							 }
							}
							else {
								regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
										 regId, packetId+":Packet not present in dmz server");
							}
						}catch (ApisResourceAccessException e) {    
					           regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					        		   regId, e.getMessage());
					       }
						}
				}
			}
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
					"", "PacketUploaderServiceImpl::movePacketsToObjectStore()::exit");
			}
		
	   
}
