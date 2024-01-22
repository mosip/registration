package io.mosip.registration.processor.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.LandingZoneTypeConstant;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.spi.filesystem.manager.FileManager;
import io.mosip.registration.processor.packet.manager.dto.DirectoryPathDto;

@Component
class LandingZone {
	private static Logger regProcLogger = RegProcessorLogger.getLogger(LandingZone.class);
	private static final int MAX_NUMBER_OF_PACKETS = 100;
	@Value("${mosip.regproc.landing.zone.account.name}")
	private String landingZoneAccount;

	@Value("${mosip.regproc.landing.zone.type:ObjectStore}")
	private String landingZoneType;

	@Value("${registration.processor.packet.ext}")
	private String extention;

	@Autowired
	private FileManager<DirectoryPathDto, InputStream> fileManager;

	@Autowired
	private ObjectStoreAdapter objectStoreAdapter;

	@Autowired
	Environment env;

	/**
	 * 
	 * moves the packets from dmz server to objectstore if the landing zone has been
	 * changed to
	 */
	@Scheduled(fixedDelayString = "${mosip.regproc.landing.zone.fixed.delay.millisecs:43200000}", initialDelayString = "${mosip.regproc.landing.zone.inital.delay.millisecs:300000}")
	public void movePacketsToObjectStore() {

		if (landingZoneType.equalsIgnoreCase(LandingZoneTypeConstant.OBJECT_STORE)) {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
					"PacketUploaderServiceImpl::movePacketsToObjectStore()::entry");

			try {

				List<File> packetList = new ArrayList<>();
				try(Stream<Path> PathStream=Files.list(Paths.get(env.getProperty(DirectoryPathDto.LANDING_ZONE.toString())))){
					PathStream.map(Path::toFile)
					.filter(File::isFile).filter(file -> file.getName().endsWith(extention)).forEach(file -> {
						packetList.add(file);
						if (packetList.size() >= MAX_NUMBER_OF_PACKETS) {
							packetList.forEach(packet -> handlePacket(packet));
							packetList.clear();
						}
					});
				}

				if (packetList.size() < MAX_NUMBER_OF_PACKETS) {
					packetList.forEach(packet -> handlePacket(packet));
					packetList.clear();
				}

			} catch (IOException e) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), "", e.getMessage());
			} catch (Exception e) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), "", e.getMessage());
			}
		}

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"PacketUploaderServiceImpl::movePacketsToObjectStore()::exit");
	}

	private void handlePacket(File packet) {

		String regId = packet.getName().split("-")[0];
		String packetId = packet.getName().split("\\.")[0];
		if (packet.exists()) {
			boolean result;
			try {
				InputStream stream = new FileInputStream(packet);
				result = objectStoreAdapter.putObject(landingZoneAccount, regId, null, null, packetId, stream);
				stream.close();
				if (!result) {
					regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
							regId, packetId + ":Packet store not accesible");
				} else {
					fileManager.deletePacket(DirectoryPathDto.LANDING_ZONE, packetId);
					regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
							regId, packetId + ":Packet has been moved to landing zone object store ");
				}
			} catch (Exception e) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
						regId, e.getMessage());
			}
		} else {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), regId,
					packetId + ":Packet not present in dmz server");
		}
	}
}
