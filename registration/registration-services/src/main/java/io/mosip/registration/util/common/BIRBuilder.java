package io.mosip.registration.util.common;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.commons.packet.constants.Biometric;
import io.mosip.commons.packet.constants.PacketManagerConstants;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.ProcessedLevelType;
import io.mosip.kernel.biometrics.constant.PurposeType;
import io.mosip.kernel.biometrics.constant.QualityType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BIRInfo;
import io.mosip.kernel.biometrics.entities.RegistryIDType;
import io.mosip.kernel.biometrics.entities.VersionType;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleAnySubtypeType;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.config.AppConfig;

@Component
public class BIRBuilder {

	@Value("${mosip.kernel.packetmanager.cbeff_only_unique_tags:Y}")
	private String uniqueTagsEnabled;

	//private static SecureRandom random = new SecureRandom(String.valueOf(5000).getBytes());

	private static final Logger LOGGER = AppConfig.getLogger(BIRBuilder.class);

	public BIR buildBIR(byte[] bdb, double qualityScore, SingleType singleType, String bioAttribute) {

		LOGGER.debug("BIRBUILDER", APPLICATION_NAME, APPLICATION_ID,
				"started building BIR for for bioAttribute : " + bioAttribute);

		LOGGER.debug("BIRBUILDER", APPLICATION_NAME, APPLICATION_ID,
				"started building BIR format for for bioAttribute : " + bioAttribute);
		// Format
		RegistryIDType birFormat = new RegistryIDType();
		birFormat.setOrganization(PacketManagerConstants.CBEFF_DEFAULT_FORMAT_ORG);
		birFormat.setType(String.valueOf(Biometric.getFormatType(singleType)));
		
		BiometricType biometricType = BiometricType.fromValue(singleType.name());

		LOGGER.debug("BIRBUILDER", APPLICATION_NAME, APPLICATION_ID,
				"started building BIR algorithm for for bioAttribute : " + bioAttribute);

		// Algorithm
		RegistryIDType birAlgorithm = new RegistryIDType();
		birAlgorithm.setOrganization(PacketManagerConstants.CBEFF_DEFAULT_ALG_ORG);
		birAlgorithm.setType(PacketManagerConstants.CBEFF_DEFAULT_ALG_TYPE);

		LOGGER.debug("BIRBUILDER", APPLICATION_NAME, APPLICATION_ID,
				"started building Quality type for for bioAttribute : " + bioAttribute);

		// Quality Type
		QualityType qualityType = new QualityType();
		qualityType.setAlgorithm(birAlgorithm);
		qualityType.setScore((long) qualityScore);
		
		VersionType versionType = new VersionType(1, 1);

		return new BIR.BIRBuilder().withBdb(bdb)
				.withVersion(versionType)
				.withCbeffversion(versionType)
				.withBirInfo(new BIRInfo.BIRInfoBuilder().withIntegrity(false).build())
				.withBdbInfo(new BDBInfo.BDBInfoBuilder().withFormat(birFormat).withQuality(qualityType)
						.withType(Arrays.asList(biometricType)).withSubtype(getSubTypes(biometricType, bioAttribute))
						.withPurpose(PurposeType.ENROLL).withLevel(ProcessedLevelType.RAW)
						.withCreationDate(LocalDateTime.now(ZoneId.of("UTC"))).withIndex(UUID.randomUUID().toString())
						.build())
				.build();
	}

//	private JAXBElement<String> getCBEFFTestTag(SingleType biometricType) {
//		String testTagElementName = null;
//		String testTagType = "y".equalsIgnoreCase(uniqueTagsEnabled) ? "Unique"
//				: (random.nextInt() % 2 == 0 ? "Duplicate" : "Unique");
//
//		switch (biometricType) {
//		case FINGER:
//			testTagElementName = "TestFinger";
//			break;
//		case IRIS:
//			testTagElementName = "TestIris";
//			break;
//		case FACE:
//			testTagElementName = "TestFace";
//			break;
//		default:
//			break;
//		}
//
//		return new JAXBElement<>(new QName("testschema", testTagElementName), String.class, testTagType);
//	}

	@SuppressWarnings("incomplete-switch")
	private List<String> getSubTypes(BiometricType biometricType, String bioAttribute) {
		List<String> subtypes = new LinkedList<>();
		switch (biometricType) {
		case FINGER:
			subtypes.add(bioAttribute.contains("left") ? SingleAnySubtypeType.LEFT.value()
					: SingleAnySubtypeType.RIGHT.value());
			if (bioAttribute.toLowerCase().contains("thumb"))
				subtypes.add(SingleAnySubtypeType.THUMB.value());
			else {
				String val = bioAttribute.toLowerCase().replace("left", "").replace("right", "");
				subtypes.add(SingleAnySubtypeType.fromValue(StringUtils.capitalizeFirstLetter(val).concat("Finger"))
						.value());
			}
			break;
		case IRIS:
			subtypes.add(bioAttribute.contains("left") ? SingleAnySubtypeType.LEFT.value()
					: SingleAnySubtypeType.RIGHT.value());
			break;
		case FACE:
			break;
		}
		return subtypes;
	}

}
