package io.mosip.registration.processor.anonymous.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.QualityType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.entities.Entry;
import io.mosip.registration.processor.core.anonymous.service.AnonymousProfileService;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.packet.dto.Document;
import io.mosip.registration.processor.core.packet.dto.FieldValue;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "javax.net.ssl.*" })
public class AnonymousProfileServiceTest {

	@InjectMocks
	AnonymousProfileService anonymousProfileService;

	@Mock
	ObjectMapper mapper;

	Map<String, String> fieldMap = new HashedMap();
	Map<String, String> metaInfoMap = new HashedMap();
	BiometricRecord biometricRecord = new BiometricRecord();

	@Before
	public void setUp() throws Exception {
		ReflectionTestUtils.setField(anonymousProfileService, "mandatoryLanguages", Arrays.asList("eng"));

		fieldMap.put("postalCode", "14022");
		fieldMap.put("dateOfBirth", "1998/01/01");
		fieldMap.put("preferredLang", "English");
		fieldMap.put("phone", "6666666666");
		fieldMap.put("gender",
				"[ {\"language\" : \"eng\",\"value\" : \"Female\"}, {\"language\" : \"ara\",\"value\" : \"أنثى\"} ]");
		fieldMap.put("zone",
				"[ {\"language\" : \"eng\",\"value\" : \"Ben Mansour\"}, {\"language\" : \"ara\",\"value\" : \"بن منصور\"} ]");

		metaInfoMap.put("documents", "[{\"documentType\" : \"CIN\"},{\"documentType\" : \"RNC\"}]");
		metaInfoMap.put("metaData",
				"[{\"label\" : \"registrationType\",\"value\" : \"NEW\"},{\"label\" : \"centerId\",\"value\" : \"1003\"}]");
		metaInfoMap.put("operationsData",
				"[{\"label\" : \"officerId\",\"value\" : \"110024\"},{\"label\" : \"officerBiometricFileName\",\"value\" : \"null\"}]");
		metaInfoMap.put("creationDate", "2021-09-01T03:48:49.193Z");

		BIR bir = new BIR();
		BDBInfo bdbInfo = new BDBInfo();
		bdbInfo.setType(Arrays.asList(BiometricType.FINGER));
		bdbInfo.setSubtype(Arrays.asList("Left", "RingFinger"));
		QualityType quality = new QualityType();
		quality.setScore((long) 80);
		bdbInfo.setQuality(quality);
		bir.setBdbInfo(bdbInfo);
		bir.setSb("eyJ4NWMiOlsiTUlJRGtEQ0NBbmlnQXdJQkFnSUVwNzo".getBytes());
		bir.setBdb("SUlSADAyMAAAACc6AAEAAQAAJyoH5AoJECYh//8Bc18wBgAAAQIDCgABlwExCA".getBytes());

		Entry entry = new Entry();
		entry.setKey("PAYLOAD");
		entry.setValue(
				"{\"digitalId\":\"eyJ4NWMiOlsiTUlJRjZqQ0NBOUtnQXdJQkFnSUJCVEFOQmdrcWhraUc5dzBC\",\"bioValue\":\"<bioValue>\",\"qualityScore\":\"80\",\"bioType\":\"Iris\"}");

		Entry entry1 = new Entry();
		entry1.setKey("EXCEPTION");
		entry1.setValue("false");

		Entry entry2 = new Entry();
		entry2.setKey("RETRIES");
		entry2.setValue("1");

		bir.setOthers(Arrays.asList(entry, entry1, entry2));
		biometricRecord.setSegments(Arrays.asList(bir));
	}

	@Test
	public void buildJsonStringFromPacketInfoTest()
			throws ApisResourceAccessException, PacketManagerException, JSONException, IOException {

		String json = "{\"processName\":\"NEW\",\"processStage\":\"packetValidatorStage\",\"date\":\"2021-09-01\",\"startDateTime\":null,\"endDateTime\":null,\"yearOfBirth\":\"1998\",\"gender\":\"Female\",\"location\":[\"Ben Mansour\",\"14022\"],\"preferredLanguages\":[\"English\"],\"channel\":[\"phone\"],\"exceptions\":[],\"verified\":null,\"biometricInfo\":[{\"type\":\"FINGER\",\"subType\":\"Left RingFinger\",\"qualityScore\":80,\"attempts\":\"1\",\"digitalId\":\"eyJ4NWMiOlsiTUlJRjZqQ0NBOUtnQXdJQkFnSUJCVEFOQmdrcWhraUc5dzBC\"}],\"device\":null,\"documents\":[\"CIN\",\"RNC\"],\"assisted\":[\"110024\"],\"enrollmentCenterId\":\"1003\",\"status\":\"PROCESSED\"}";
		Document doc1 = new Document();
		doc1.setDocumentType("CIN");
		Document doc2 = new Document();
		doc2.setDocumentType("RNC");

		Mockito.when(mapper.readValue(anyString(), any(Class.class)))
				.thenReturn(new FieldValue("registrationType", "NEW")).thenReturn(doc1).thenReturn(doc2)
				.thenReturn(new FieldValue("centerId", "1003")).thenReturn(new FieldValue("officerId", "110024"));
		assertEquals(json, anonymousProfileService.buildJsonStringFromPacketInfo(biometricRecord, fieldMap, metaInfoMap,
				"PROCESSED", "packetValidatorStage"));
	}

}
