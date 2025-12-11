package io.mosip.registration.processor.packet.manager.service.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;

import javax.swing.text.Utilities;

import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.http.RequestWrapper;
import io.mosip.registration.processor.core.idrepo.dto.IdVidMetadataRequest;
import io.mosip.registration.processor.core.idrepo.dto.IdVidMetadataResponse;
import io.mosip.registration.processor.core.idrepo.dto.ResponseDTO;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.idrepo.dto.IdResponseDTO;
import io.mosip.registration.processor.core.packet.dto.demographicinfo.identify.IdentityJsonValues;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.manager.idreposervice.impl.IdRepoServiceImpl;

import java.io.IOException;
import java.util.List;

/**
 * The Class IdRepoServiceImplTest.
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ Utilities.class, IOUtils.class, JsonUtil.class })
public class IdRepoServiceImplTest {

	/** The rest client service. */
	@Mock
	private RegistrationProcessorRestClientService<Object> restClientService;

	/** The id repo service. */
	@InjectMocks
	private IdRepoServiceImpl idRepoService;

	@Spy
	private ObjectMapper mapper = new ObjectMapper();

	
	/**
	 * Sets the up.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Before
	public void setUp() throws Exception {
		IdentityJsonValues jv = new IdentityJsonValues();
		jv.setValue("1");
		IdResponseDTO dto = new IdResponseDTO();
		ResponseWrapper<IdResponseDTO> response = new ResponseWrapper();
		response.setId("1");
		response.setResponse(dto);
		Mockito.when(restClientService.getApi(any(), anyList(), anyString(), any(), any())).thenReturn(response);

	}

	/**
	 * Testget id json from ID repo.
	 *
	 * @throws Exception
	 *             the exception
	 */
	/*@Test
	public void testgetIdJsonFromIDRepo() throws Exception {
		Mockito.when(mapper.writeValueAsString(Mockito.any())).thenReturn("{}");
		JSONObject matchedDemographicIdentity = idRepoService.getIdJsonFromIDRepo("", "Identity");
		assertNull(matchedDemographicIdentity);

	}*/

	/**
	 * Testget UIN by RID.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testgetUINByRID() throws Exception {
		Mockito.when(mapper.writeValueAsString(Mockito.any())).thenReturn("");
		JSONObject demoJson = new JSONObject();
		demoJson.put("UIN", "1");
		PowerMockito.mockStatic(JsonUtil.class);
		PowerMockito.when(JsonUtil.class, "objectMapperReadValue", any(), any()).thenReturn(demoJson);
		PowerMockito.when(JsonUtil.class, "getJSONObject", any(), any()).thenReturn(demoJson);
		String matchedDemographicIdentity = idRepoService.getUinByRid("", "Identity");
		assertNull(matchedDemographicIdentity);
	}
	
	@Test
	public void testgetUINByRIDResponseNull() throws Exception {
		ResponseWrapper<IdResponseDTO> response = new ResponseWrapper();
		response.setId("1");
		response.setResponse(null);
		Mockito.when(restClientService.getApi(any(), anyList(), anyString(), any(), any())).thenReturn(response);
		String matchedDemographicIdentity = idRepoService.getUinByRid("", "Identity");
		assertNull(matchedDemographicIdentity);
	}

	/**
	 * Testfind uin from idrepo.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testfindUinFromIdrepo() throws Exception {
		Mockito.when(mapper.writeValueAsString(Mockito.any())).thenReturn("");
		JSONObject demoJson = new JSONObject();
		demoJson.put("UIN", "1");
		PowerMockito.mockStatic(JsonUtil.class);
		PowerMockito.when(JsonUtil.class, "objectMapperReadValue", any(), any()).thenReturn(demoJson);
		PowerMockito.when(JsonUtil.class, "getJSONObject", any(), any()).thenReturn(demoJson);
		String matchedDemographicIdentity = idRepoService.findUinFromIdrepo("", "Identity");
		assertNull(matchedDemographicIdentity);

	}

	@Test
	public void testGetIdJsonFromIDRepoWhenResponseNotNull() throws Exception {
		IdResponseDTO dto = new IdResponseDTO();
		dto.setId("123333");
		dto.setVersion("v1");
		dto.setTimestamp("2025-12-11T18:10:00Z");
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setEntity("entity");
		responseDTO.setStatus("Pass");
		dto.setResponse(responseDTO);
		ResponseWrapper<IdResponseDTO> response = new ResponseWrapper<>();
		response.setResponse(dto);

		Mockito.when(restClientService.getApi(
						Mockito.eq(ApiName.RETRIEVEIDENTITYFROMRID),
						anyList(),
						anyString(),
						anyString(),
						Mockito.eq(ResponseWrapper.class)))
				.thenReturn(response);

		Mockito.when(mapper.writeValueAsString(any())).thenReturn("{\"Identity\":{\"UIN\":\"1234\"}}");

		JSONObject identity = new JSONObject();
		JSONObject demo = new JSONObject();
		demo.put("UIN", "1234");
		identity.put("Identity", demo);

		PowerMockito.mockStatic(JsonUtil.class);
		PowerMockito.when(JsonUtil.objectMapperReadValue(anyString(), Mockito.eq(JSONObject.class)))
				.thenReturn(identity);

		PowerMockito.when(JsonUtil.getJSONObject(identity, "Identity"))
				.thenReturn(demo);
		JSONObject result = idRepoService.getIdJsonFromIDRepo("123", "Identity");
		assertEquals("1234", result.get("UIN"));
	}

	@Test
	public void testGetIdJsonFromIDRepoWhenResponseNull() throws Exception {

		ResponseWrapper<IdResponseDTO> response = new ResponseWrapper<>();
		response.setResponse(null);

		Mockito.when(restClientService.getApi(any(), anyList(), anyString(), anyString(), any()))
				.thenReturn(response);
		JSONObject result = idRepoService.getIdJsonFromIDRepo("123", "Identity");
		assertNull(result);
	}

	@Test
	public void testGetIdResponseFromIDRepoWhenResponseNull() throws Exception {
		ResponseWrapper<ResponseDTO> response = new ResponseWrapper<>();
		response.setResponse(null);

		Mockito.when(restClientService.getApi(any(), anyList(), anyString(), anyString(), any()))
				.thenReturn(response);
		ResponseDTO result = idRepoService.getIdResponseFromIDRepo("123");
		assertNull(result);
	}
}
