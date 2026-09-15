package io.mosip.registration.processor.packet.manager.service.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.mosip.registration.processor.packet.manager.dto.CreateDraftV2RequestDto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Lists;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.packet.manager.dto.IdRequestDto;
import io.mosip.registration.processor.packet.manager.dto.IdResponseDTO;
import io.mosip.registration.processor.packet.manager.dto.RequestDto;
import io.mosip.registration.processor.packet.manager.dto.ResponseDTO;
import io.mosip.registration.processor.packet.manager.exception.IdrepoDraftException;
import io.mosip.registration.processor.packet.manager.exception.IdrepoDraftReprocessableException;
import io.mosip.registration.processor.packet.manager.idreposervice.IdrepoDraftService;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ IOUtils.class, HMACUtils2.class })
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "javax.net.ssl.*" })
public class IdrepoDraftServiceTest {

    @InjectMocks
    private IdrepoDraftService idrepoDraftService = new IdrepoDraftService();

    @Mock
    private RegistrationProcessorRestClientService<Object> registrationProcessorRestClientService;

    @Mock
    private ObjectMapper mapper;

    private IdResponseDTO idResponseDTO;

    private String ID = "12345689";

    @Before
    public void setup() throws IOException {
        ResponseDTO responseDTO = new ResponseDTO();
        responseDTO.setStatus("ACTIVATED");
        responseDTO.setRegistrationId(ID);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("UIN", "1234");
        responseDTO.setIdentity(jsonObject);

        idResponseDTO = new IdResponseDTO();
        idResponseDTO.setErrors(null);
        idResponseDTO.setId("id.uin.update");
        idResponseDTO.setResponse(responseDTO);

        when(mapper.writeValueAsString(any())).thenReturn("string");
        when(mapper.readValue("string", JSONObject.class)).thenReturn(jsonObject);
    }

    @Test
    public void idrepoDraftPresentTest() throws Exception {

        when(registrationProcessorRestClientService.headApi
                (ApiName.IDREPOHASDRAFT, Lists.newArrayList(ID), null, null)).thenReturn(200);

        boolean result = idrepoDraftService.idrepoHasDraft(ID);

        assertTrue(result);
    }

    @Test
    public void idrepoDraftNotPresentTest() throws Exception {

        when(registrationProcessorRestClientService.headApi
                (ApiName.IDREPOHASDRAFT, Lists.newArrayList(ID), null, null)).thenReturn(204);

        boolean result = idrepoDraftService.idrepoHasDraft(ID);

        assertFalse(result);
    }

    @Test
    public void idrepoDraftCheckExceptionTest() throws Exception {
        when(registrationProcessorRestClientService.headApi
                (ApiName.IDREPOHASDRAFT, Lists.newArrayList(ID), null, null)).thenReturn(500);

        try {
            idrepoDraftService.idrepoHasDraft(ID);
            fail("Expected IdrepoDraftException to be thrown");
        } catch (IdrepoDraftException e) {
            assertEquals(PlatformErrorMessages.DRAFT_CHECK_FAILED.getCode(), e.getErrorCode());
        }
    }

    @Test
    public void idrepoGetDraftSuccessTest() throws Exception {

        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOGETDRAFT, Lists.newArrayList(ID), Lists.emptyList(), null, IdResponseDTO.class)).thenReturn(idResponseDTO);

        ResponseDTO result = idrepoDraftService.idrepoGetDraft(ID);

        assertTrue(result.getRegistrationId().equals(ID));
    }
    
    @Test
    public void idrepoPublishDraftSuccessTest() throws ApisResourceAccessException, IdrepoDraftException, IdrepoDraftReprocessableException {

        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOPUBLISHDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class)).thenReturn(idResponseDTO);

        IdResponseDTO result = idrepoDraftService.idrepoPublishDraft(ID);

        assertTrue(result.getResponse().getRegistrationId().equals(ID));
    }
    
    @Test
	public void idrepoPublishDraftExceptionTest()
			throws ApisResourceAccessException, IdrepoDraftReprocessableException {
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage("ERROR");
        errorDTO.setErrorCode("ERROR");
        IdResponseDTO idResponseDTO1 = new IdResponseDTO();
        idResponseDTO1.setErrors(Lists.newArrayList(errorDTO));
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOPUBLISHDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class)).thenReturn(idResponseDTO1);

        IdResponseDTO discardIdresponseDto = new IdResponseDTO();
        discardIdresponseDto.setErrors(null);
        discardIdresponseDto.setId("id.uin.update");
        when(registrationProcessorRestClientService
                .deleteApi(ApiName.IDREPODISCARDDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class))
                .thenReturn(discardIdresponseDto);

        try {
            idrepoDraftService.idrepoPublishDraft(ID);
            fail("Expected IdrepoDraftException to be thrown");
        } catch (IdrepoDraftException e) {
            // expected
        }
        verify(registrationProcessorRestClientService)
                .deleteApi(ApiName.IDREPODISCARDDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class);
    }

    @Test
	public void idrepoUpdateDraftSuccessTest()
			throws ApisResourceAccessException, IdrepoDraftException, IOException, IdrepoDraftReprocessableException {
        RequestDto requestDto = new RequestDto();
        requestDto.setIdentity(idResponseDTO.getResponse().getIdentity());
        IdRequestDto idRequestDto = new IdRequestDto();
        idRequestDto.setRequest(requestDto);

        when(registrationProcessorRestClientService.headApi
                (ApiName.IDREPOHASDRAFT, Lists.newArrayList(ID), null, null)).thenReturn(200);
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOGETDRAFT, Lists.newArrayList(ID), Lists.emptyList(), null, IdResponseDTO.class)).thenReturn(idResponseDTO);
        when(registrationProcessorRestClientService.patchApi(
                any(), any(), any(), any(), any(), any())).thenReturn(idResponseDTO);

        IdResponseDTO result = idrepoDraftService.idrepoUpdateDraft(ID, null, idRequestDto);

        assertTrue(result.getResponse().getRegistrationId().equals(ID));
    }

    @Test
	public void idrepoUpdateDraftExceptionTest()
			throws ApisResourceAccessException, IOException, IdrepoDraftReprocessableException {
        RequestDto requestDto = new RequestDto();
        requestDto.setIdentity(idResponseDTO.getResponse().getIdentity());
        IdRequestDto idRequestDto = new IdRequestDto();
        idRequestDto.setRequest(requestDto);

        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage("ERROR");
        errorDTO.setErrorCode("ERROR");
        IdResponseDTO idResponseDTO1 = new IdResponseDTO();
        idResponseDTO1.setErrors(Lists.newArrayList(errorDTO));

        when(registrationProcessorRestClientService.headApi(
                ApiName.IDREPOHASDRAFT, Lists.newArrayList(ID), null, null)).thenReturn(200);
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOGETDRAFT, Lists.newArrayList(ID), Lists.emptyList(), null, IdResponseDTO.class)).thenReturn(idResponseDTO);
        when(registrationProcessorRestClientService.patchApi(
                any(), any(), any(), any(), any(), any())).thenReturn(idResponseDTO1);

        IdResponseDTO discardIdresponseDto = new IdResponseDTO();
        discardIdresponseDto.setErrors(null);
        discardIdresponseDto.setId("id.uin.update");
        when(registrationProcessorRestClientService
                .deleteApi(ApiName.IDREPODISCARDDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class))
                .thenReturn(discardIdresponseDto);

        try {
            idrepoDraftService.idrepoUpdateDraft(ID, null, idRequestDto);
            fail("Expected IdrepoDraftException to be thrown");
        } catch (IdrepoDraftException e) {
            // expected
        }
        verify(registrationProcessorRestClientService)
                .deleteApi(ApiName.IDREPODISCARDDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class);
    }

	@Test
	public void idrepoDraftReprocessableExceptionTest()
			throws Exception {
		RequestDto requestDto = new RequestDto();
		requestDto.setIdentity(idResponseDTO.getResponse().getIdentity());
		IdRequestDto idRequestDto = new IdRequestDto();
		idRequestDto.setRequest(requestDto);

		ErrorDTO errorDTO = new ErrorDTO();
		errorDTO.setMessage("Failed to either encrypt/decrypt message using Kernel Crypto Manager");
		errorDTO.setErrorCode("IDR-IDS-003");
		IdResponseDTO idResponseDTO1 = new IdResponseDTO();
		idResponseDTO1.setErrors(Lists.newArrayList(errorDTO));

		when(registrationProcessorRestClientService.headApi(ApiName.IDREPOHASDRAFT, Lists.newArrayList(ID), null, null))
				.thenReturn(200);
		when(registrationProcessorRestClientService.getApi(ApiName.IDREPOGETDRAFT, Lists.newArrayList(ID),
				Lists.emptyList(), null, IdResponseDTO.class)).thenReturn(idResponseDTO);
		when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
				.thenReturn(idResponseDTO1);

		IdResponseDTO discardIdresponseDto = new IdResponseDTO();
		discardIdresponseDto.setErrors(null);
		discardIdresponseDto.setId("id.uin.update");
		when(registrationProcessorRestClientService
				.deleteApi(ApiName.IDREPODISCARDDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class))
				.thenReturn(discardIdresponseDto);

		try {
			idrepoDraftService.idrepoUpdateDraft(ID, null, idRequestDto);
			fail("Expected IdrepoDraftReprocessableException to be thrown");
		} catch (IdrepoDraftReprocessableException e) {
            assertEquals("IDR-IDS-003", e.getErrorCode());
		}
		verify(registrationProcessorRestClientService)
				.deleteApi(ApiName.IDREPODISCARDDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class);
	}

    @Test
    public void idrepoGetDraftWithTypeSuccessTest() throws Exception {
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOGETDRAFT, Lists.newArrayList(ID), "type", "demographics", IdResponseDTO.class)).thenReturn(idResponseDTO);

        ResponseDTO result = idrepoDraftService.idrepoGetDraft(ID, "demographics");

        assertTrue(result.getRegistrationId().equals(ID));
    }

    @Test
    public void idrepoGetDraftWithBiometricsTypeTest() throws Exception {
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOGETDRAFT, Lists.newArrayList(ID), "type", "biometrics", IdResponseDTO.class)).thenReturn(idResponseDTO);

        ResponseDTO result = idrepoDraftService.idrepoGetDraft(ID, "biometrics");

        assertTrue(result.getRegistrationId().equals(ID));
    }

    @Test
    public void idrepoGetDraftWithSupportingDocumentsTypeTest() throws Exception {
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOGETDRAFT, Lists.newArrayList(ID), "type", "supportingDocuments", IdResponseDTO.class)).thenReturn(idResponseDTO);

        ResponseDTO result = idrepoDraftService.idrepoGetDraft(ID, "supportingDocuments");

        assertTrue(result.getRegistrationId().equals(ID));
    }

    @Test(expected = IdrepoDraftException.class)
    public void idrepoGetDraftWithTypeErrorTest() throws Exception {
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage("ERROR");
        errorDTO.setErrorCode("ERROR");
        IdResponseDTO errorResponse = new IdResponseDTO();
        errorResponse.setErrors(Lists.newArrayList(errorDTO));

        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOGETDRAFT, Lists.newArrayList(ID), "type", "demographics", IdResponseDTO.class)).thenReturn(errorResponse);

        idrepoDraftService.idrepoGetDraft(ID, "demographics");
    }

    @Test
    public void idrepoCreateDraftSuccessTest() throws Exception {
        ResponseWrapper responseWrapper = new ResponseWrapper();

        when(registrationProcessorRestClientService.postApi(
                eq(ApiName.IDREPOCREATEDRAFT), any(), any(), any(), isNull(), eq(ResponseWrapper.class))).thenReturn(responseWrapper);

        boolean result = idrepoDraftService.idrepoCreateDraft(ID, null);

        assertTrue(result);
        verify(registrationProcessorRestClientService).postApi(
                eq(ApiName.IDREPOCREATEDRAFT), eq(Lists.newArrayList(ID)), isNull(), isNull(), isNull(), eq(ResponseWrapper.class));
    }

    @Test
    public void idrepoCreateDraftV2SuccessTest() throws Exception {
        ResponseWrapper responseWrapper = new ResponseWrapper();
        ArgumentCaptor<CreateDraftV2RequestDto> bodyCaptor = ArgumentCaptor.forClass(CreateDraftV2RequestDto.class);

        when(registrationProcessorRestClientService.postApi(
                eq(ApiName.IDREPOCREATEDRAFT), any(), any(), any(), any(), eq(ResponseWrapper.class))).thenReturn(responseWrapper);

        boolean result = idrepoDraftService.idrepoCreateDraftV2(ID, null, true);

        assertTrue(result);
        verify(registrationProcessorRestClientService).postApi(
                eq(ApiName.IDREPOCREATEDRAFT), any(), any(), any(), bodyCaptor.capture(), eq(ResponseWrapper.class));
        CreateDraftV2RequestDto captured = bodyCaptor.getValue();
        assertNull("UIN must be null for a NEW packet", captured.getUin());
        assertTrue("generateUin must be true for a NEW packet", captured.isGenerateUin());
    }

    @Test(expected = IdrepoDraftException.class)
    public void idrepoCreateDraftV2ExceptionTest() throws Exception {
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage("ERROR");
        errorDTO.setErrorCode("ERROR");
        ResponseWrapper errorResponse = new ResponseWrapper();
        errorResponse.setErrors(Lists.newArrayList(errorDTO));

        when(registrationProcessorRestClientService.postApi(
                eq(ApiName.IDREPOCREATEDRAFT), any(), any(), any(), any(), eq(ResponseWrapper.class))).thenReturn(errorResponse);

        idrepoDraftService.idrepoCreateDraftV2(ID, null, false);
    }

    @Test
    public void idrepoUpdateDraftUinSuccessTest() throws Exception {
        when(mapper.createObjectNode()).thenReturn(new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode());
        ArgumentCaptor<ObjectNode> bodyCaptor = ArgumentCaptor.forClass(ObjectNode.class);
        when(registrationProcessorRestClientService.patchApi(
                eq(ApiName.IDREPOUPDATEDRAFTUIN), any(), any(), any(), any(), any())).thenReturn(idResponseDTO);

        boolean result = idrepoDraftService.idrepoUpdateDraftUin(ID, "1234567890");

        assertTrue(result);
        verify(registrationProcessorRestClientService).patchApi(
                eq(ApiName.IDREPOUPDATEDRAFTUIN), any(), any(), any(), bodyCaptor.capture(), any());
        assertEquals("1234567890", bodyCaptor.getValue().get("uin").asText());
    }

    @Test(expected = IdrepoDraftException.class)
    public void idrepoUpdateDraftUinExceptionTest() throws Exception {
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage("ERROR");
        errorDTO.setErrorCode("ERROR");
        IdResponseDTO errorResponse = new IdResponseDTO();
        errorResponse.setErrors(Lists.newArrayList(errorDTO));

        when(mapper.createObjectNode()).thenReturn(new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode());
        when(registrationProcessorRestClientService.patchApi(
                eq(ApiName.IDREPOUPDATEDRAFTUIN), any(), any(), any(), any(), any())).thenReturn(errorResponse);

        idrepoDraftService.idrepoUpdateDraftUin(ID, "1234567890");
    }

    @Test
    public void idrepoUpdateDraftUinNullResponseTest() throws Exception {
        when(mapper.createObjectNode()).thenReturn(new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode());
        when(registrationProcessorRestClientService.patchApi(
                eq(ApiName.IDREPOUPDATEDRAFTUIN), any(), any(), any(), any(), any())).thenReturn(null);

        try {
            idrepoDraftService.idrepoUpdateDraftUin(ID, "1234567890");
            fail("Expected ApisResourceAccessException to be thrown");
        } catch (ApisResourceAccessException e) {
            assertNullIdRepoResponse("update draft UIN", e);
        }
    }

    @Test(expected = IdrepoDraftException.class)
    public void idrepoPublishDraftUinDetailsNotFoundTest()
            throws ApisResourceAccessException, IdrepoDraftException, IdrepoDraftReprocessableException {
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage("UIN details not found in draft");
        errorDTO.setErrorCode("IDR-IDC-015");
        IdResponseDTO errorResponse = new IdResponseDTO();
        errorResponse.setErrors(Lists.newArrayList(errorDTO));

        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOPUBLISHDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class)).thenReturn(errorResponse);

        IdResponseDTO discardIdresponseDto = new IdResponseDTO();
        discardIdresponseDto.setErrors(null);
        when(registrationProcessorRestClientService.deleteApi(
                ApiName.IDREPODISCARDDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class))
                .thenReturn(discardIdresponseDto);

        idrepoDraftService.idrepoPublishDraft(ID);
    }

    @Test
    public void idrepoUpdateDraftV2RecordAlreadyExists_ThrowsFailed() throws Exception {
        RequestDto requestDto = new RequestDto();
        requestDto.setIdentity(idResponseDTO.getResponse().getIdentity());
        IdRequestDto idRequestDto = new IdRequestDto();
        idRequestDto.setRequest(requestDto);

        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage("Record already exists in the system");
        errorDTO.setErrorCode("IDR-IDC-012");
        IdResponseDTO errorResponse = new IdResponseDTO();
        errorResponse.setErrors(Lists.newArrayList(errorDTO));

        when(registrationProcessorRestClientService.headApi(
                ApiName.IDREPOHASDRAFT, Lists.newArrayList(ID), null, null)).thenReturn(200);
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOGETDRAFT, Lists.newArrayList(ID), Lists.emptyList(), null, IdResponseDTO.class)).thenReturn(idResponseDTO);
        when(registrationProcessorRestClientService.patchApi(
                any(), any(), any(), any(), any(), any())).thenReturn(errorResponse);

        try {
            idrepoDraftService.idrepoUpdateDraftV2(ID, null, idRequestDto, true);
            fail("Expected IdrepoDraftException to be thrown");
        } catch (IdrepoDraftException e) {
            assertEquals("IDR-IDC-012", e.getErrorCode());
        }
        verify(registrationProcessorRestClientService, never())
                .deleteApi(eq(ApiName.IDREPODISCARDDRAFT), any(), any(), any(), any());
    }

    @Test
    public void idrepoUpdateDraftRecordAlreadyExists_DiscardsDraftAndThrowsFailed()
            throws ApisResourceAccessException, IdrepoDraftException, IOException, IdrepoDraftReprocessableException {
        RequestDto requestDto = new RequestDto();
        requestDto.setIdentity(idResponseDTO.getResponse().getIdentity());
        IdRequestDto idRequestDto = new IdRequestDto();
        idRequestDto.setRequest(requestDto);

        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage("Record already exists in the system");
        errorDTO.setErrorCode("IDR-IDC-012");
        IdResponseDTO errorResponse = new IdResponseDTO();
        errorResponse.setErrors(Lists.newArrayList(errorDTO));

        when(registrationProcessorRestClientService.headApi(
                ApiName.IDREPOHASDRAFT, Lists.newArrayList(ID), null, null)).thenReturn(200);
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOGETDRAFT, Lists.newArrayList(ID), Lists.emptyList(), null, IdResponseDTO.class)).thenReturn(idResponseDTO);
        when(registrationProcessorRestClientService.patchApi(
                any(), any(), any(), any(), any(), any())).thenReturn(errorResponse);

        IdResponseDTO discardIdresponseDto = new IdResponseDTO();
        discardIdresponseDto.setErrors(null);
        when(registrationProcessorRestClientService.deleteApi(
                ApiName.IDREPODISCARDDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class))
                .thenReturn(discardIdresponseDto);

        try {
            idrepoDraftService.idrepoUpdateDraft(ID, null, idRequestDto);
            fail("Expected IdrepoDraftException to be thrown");
        } catch (IdrepoDraftException e) {
            assertEquals("IDR-IDC-012", e.getErrorCode());
        }
        verify(registrationProcessorRestClientService)
                .deleteApi(eq(ApiName.IDREPODISCARDDRAFT), any(), any(), any(), any());
    }

    @Test
    public void idrepoUpdateDraftCreatesLegacyDraftWhenNotPresentNewPacketTest()
            throws ApisResourceAccessException, IdrepoDraftException, IOException, IdrepoDraftReprocessableException {
        RequestDto requestDto = new RequestDto();
        requestDto.setIdentity(idResponseDTO.getResponse().getIdentity());
        IdRequestDto idRequestDto = new IdRequestDto();
        idRequestDto.setRequest(requestDto);
        ResponseWrapper createResponse = new ResponseWrapper();

        when(registrationProcessorRestClientService.headApi(
                ApiName.IDREPOHASDRAFT, Lists.newArrayList(ID), null, null)).thenReturn(204);
        when(registrationProcessorRestClientService.postApi(
                eq(ApiName.IDREPOCREATEDRAFT), eq(Lists.newArrayList(ID)), isNull(), isNull(), isNull(), eq(ResponseWrapper.class)))
                .thenReturn(createResponse);
        when(registrationProcessorRestClientService.patchApi(
                any(), any(), any(), any(), any(), any())).thenReturn(idResponseDTO);

        IdResponseDTO result = idrepoDraftService.idrepoUpdateDraft(ID, null, idRequestDto);

        assertTrue(result.getResponse().getRegistrationId().equals(ID));
        verify(registrationProcessorRestClientService).postApi(
                eq(ApiName.IDREPOCREATEDRAFT), eq(Lists.newArrayList(ID)), isNull(), isNull(), isNull(), eq(ResponseWrapper.class));
    }

    @Test
    public void idrepoUpdateDraftCreatesLegacyDraftWhenNotPresentUpdatePacketTest()
            throws ApisResourceAccessException, IdrepoDraftException, IOException, IdrepoDraftReprocessableException {
        RequestDto requestDto = new RequestDto();
        requestDto.setIdentity(idResponseDTO.getResponse().getIdentity());
        IdRequestDto idRequestDto = new IdRequestDto();
        idRequestDto.setRequest(requestDto);
        ResponseWrapper createResponse = new ResponseWrapper();
        String existingUin = "1234567890123456";

        when(registrationProcessorRestClientService.headApi(
                ApiName.IDREPOHASDRAFT, Lists.newArrayList(ID), null, null)).thenReturn(204);
        when(registrationProcessorRestClientService.postApi(
                eq(ApiName.IDREPOCREATEDRAFT), eq(Lists.newArrayList(ID)), eq("UIN"), eq(existingUin), isNull(), eq(ResponseWrapper.class)))
                .thenReturn(createResponse);
        when(registrationProcessorRestClientService.patchApi(
                any(), any(), any(), any(), any(), any())).thenReturn(idResponseDTO);

        IdResponseDTO result = idrepoDraftService.idrepoUpdateDraft(ID, existingUin, idRequestDto);

        assertTrue(result.getResponse().getRegistrationId().equals(ID));
        verify(registrationProcessorRestClientService).postApi(
                eq(ApiName.IDREPOCREATEDRAFT), eq(Lists.newArrayList(ID)), eq("UIN"), eq(existingUin), isNull(), eq(ResponseWrapper.class));
    }

    @Test
    public void idrepoUpdateDraftV2CreatesDraftWhenNotPresentNewPacketTest()
            throws Exception {
        RequestDto requestDto = new RequestDto();
        requestDto.setIdentity(idResponseDTO.getResponse().getIdentity());
        IdRequestDto idRequestDto = new IdRequestDto();
        idRequestDto.setRequest(requestDto);
        ResponseWrapper createResponse = new ResponseWrapper();
        ArgumentCaptor<CreateDraftV2RequestDto> bodyCaptor = ArgumentCaptor.forClass(CreateDraftV2RequestDto.class);

        when(registrationProcessorRestClientService.headApi(
                ApiName.IDREPOHASDRAFT, Lists.newArrayList(ID), null, null)).thenReturn(204);
        when(registrationProcessorRestClientService.postApi(
                eq(ApiName.IDREPOCREATEDRAFT), any(), any(), any(), any(), eq(ResponseWrapper.class)))
                .thenReturn(createResponse);
        when(registrationProcessorRestClientService.patchApi(
                any(), any(), any(), any(), any(), any())).thenReturn(idResponseDTO);

        IdResponseDTO result = idrepoDraftService.idrepoUpdateDraftV2(ID, null, idRequestDto, true);

        assertTrue(result.getResponse().getRegistrationId().equals(ID));
        verify(registrationProcessorRestClientService).postApi(
                eq(ApiName.IDREPOCREATEDRAFT), any(), any(), any(), bodyCaptor.capture(), eq(ResponseWrapper.class));
        CreateDraftV2RequestDto captured = bodyCaptor.getValue();
        assertNull(captured.getUin());
        assertTrue(captured.isGenerateUin());
    }

    @Test
    public void idrepoUpdateDraftV2CreatesDraftWhenNotPresentUpdatePacketTest()
            throws Exception {
        RequestDto requestDto = new RequestDto();
        requestDto.setIdentity(idResponseDTO.getResponse().getIdentity());
        IdRequestDto idRequestDto = new IdRequestDto();
        idRequestDto.setRequest(requestDto);
        ResponseWrapper createResponse = new ResponseWrapper();
        ArgumentCaptor<CreateDraftV2RequestDto> bodyCaptor = ArgumentCaptor.forClass(CreateDraftV2RequestDto.class);
        String existingUin = "1234567890123456";

        when(registrationProcessorRestClientService.headApi(
                ApiName.IDREPOHASDRAFT, Lists.newArrayList(ID), null, null)).thenReturn(204);
        when(registrationProcessorRestClientService.postApi(
                eq(ApiName.IDREPOCREATEDRAFT), any(), any(), any(), any(), eq(ResponseWrapper.class)))
                .thenReturn(createResponse);
        when(registrationProcessorRestClientService.patchApi(
                any(), any(), any(), any(), any(), any())).thenReturn(idResponseDTO);

        IdResponseDTO result = idrepoDraftService.idrepoUpdateDraftV2(ID, existingUin, idRequestDto, false);

        assertTrue(result.getResponse().getRegistrationId().equals(ID));
        verify(registrationProcessorRestClientService).postApi(
                eq(ApiName.IDREPOCREATEDRAFT), any(), any(), any(), bodyCaptor.capture(), eq(ResponseWrapper.class));
        CreateDraftV2RequestDto captured = bodyCaptor.getValue();
        assertEquals(existingUin, captured.getUin());
        assertFalse(captured.isGenerateUin());
    }

    @Test
    public void idrepoUpdateDraftV2CreatesBareDraftWhenNotPresentLostPacketTest()
            throws Exception {
        RequestDto requestDto = new RequestDto();
        requestDto.setIdentity(idResponseDTO.getResponse().getIdentity());
        IdRequestDto idRequestDto = new IdRequestDto();
        idRequestDto.setRequest(requestDto);
        ResponseWrapper createResponse = new ResponseWrapper();
        ArgumentCaptor<CreateDraftV2RequestDto> bodyCaptor = ArgumentCaptor.forClass(CreateDraftV2RequestDto.class);

        when(registrationProcessorRestClientService.headApi(
                ApiName.IDREPOHASDRAFT, Lists.newArrayList(ID), null, null)).thenReturn(204);
        when(registrationProcessorRestClientService.postApi(
                eq(ApiName.IDREPOCREATEDRAFT), any(), any(), any(), any(), eq(ResponseWrapper.class)))
                .thenReturn(createResponse);
        when(registrationProcessorRestClientService.patchApi(
                any(), any(), any(), any(), any(), any())).thenReturn(idResponseDTO);

        IdResponseDTO result = idrepoDraftService.idrepoUpdateDraftV2(ID, null, idRequestDto, false);

        assertTrue(result.getResponse().getRegistrationId().equals(ID));
        verify(registrationProcessorRestClientService).postApi(
                eq(ApiName.IDREPOCREATEDRAFT), any(), any(), any(), bodyCaptor.capture(), eq(ResponseWrapper.class));
        CreateDraftV2RequestDto captured = bodyCaptor.getValue();
        assertNull(captured.getUin());
        assertFalse(captured.isGenerateUin());
    }

    @Test
    public void idrepoUpdateDraftV2NullGenerateUinDefaultsToNewPacketTest()
            throws Exception {
        RequestDto requestDto = new RequestDto();
        requestDto.setIdentity(idResponseDTO.getResponse().getIdentity());
        IdRequestDto idRequestDto = new IdRequestDto();
        idRequestDto.setRequest(requestDto);
        ResponseWrapper createResponse = new ResponseWrapper();
        ArgumentCaptor<CreateDraftV2RequestDto> bodyCaptor = ArgumentCaptor.forClass(CreateDraftV2RequestDto.class);

        when(registrationProcessorRestClientService.headApi(
                ApiName.IDREPOHASDRAFT, Lists.newArrayList(ID), null, null)).thenReturn(204);
        when(registrationProcessorRestClientService.postApi(
                eq(ApiName.IDREPOCREATEDRAFT), any(), any(), any(), any(), eq(ResponseWrapper.class)))
                .thenReturn(createResponse);
        when(registrationProcessorRestClientService.patchApi(
                any(), any(), any(), any(), any(), any())).thenReturn(idResponseDTO);

        IdResponseDTO result = idrepoDraftService.idrepoUpdateDraftV2(ID, null, idRequestDto, null);

        assertTrue(result.getResponse().getRegistrationId().equals(ID));
        verify(registrationProcessorRestClientService).postApi(
                eq(ApiName.IDREPOCREATEDRAFT), any(), any(), any(), bodyCaptor.capture(), eq(ResponseWrapper.class));
        CreateDraftV2RequestDto captured = bodyCaptor.getValue();
        assertNull(captured.getUin());
        assertTrue(captured.isGenerateUin());
    }

    @Test
    public void discardDraftSuccessTest() throws IdrepoDraftReprocessableException, IdrepoDraftException, ApisResourceAccessException {
        ResponseDTO discardresponseDTO = new ResponseDTO();
        discardresponseDTO.setStatus("Drafted");
        discardresponseDTO.setRegistrationId(ID);

        JSONObject jsonObject1 = new JSONObject();
        jsonObject1.put("UIN", "1234");
        discardresponseDTO.setIdentity(jsonObject1);
        IdResponseDTO discardIdresponseDto= new IdResponseDTO();
        discardIdresponseDto.setErrors(null);
        discardIdresponseDto.setId("id.uin.update");
        discardIdresponseDto.setResponse(discardresponseDTO);
        when(registrationProcessorRestClientService.
                deleteApi(ApiName.IDREPODISCARDDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class)).thenReturn(discardIdresponseDto);

        Boolean result= idrepoDraftService.idrepoDiscardDraft(ID);
        assertTrue(result);
    }

    @Test
    public void isReprocessableError_MatchesConfiguredDraftV2Codes() {
        assertTrue(idrepoDraftService.isReprocessableError("IDR-IDS-004"));
        assertTrue(idrepoDraftService.isReprocessableError("IDR-IDS-009"));
        assertTrue(idrepoDraftService.isReprocessableError("IDR-IDC-019"));
        assertTrue(idrepoDraftService.isReprocessableError("IDR-IDC-020"));
        assertTrue(idrepoDraftService.isReprocessableError("IDR-IDC-021"));
        assertTrue(idrepoDraftService.isReprocessableError("idr-ids-003"));
        assertFalse(idrepoDraftService.isReprocessableError("IDR-IDC-001"));
        assertFalse(idrepoDraftService.isReprocessableError("IDR-IDC-004"));
        assertFalse(idrepoDraftService.isReprocessableError("IDR-IDC-005"));
        assertFalse(idrepoDraftService.isReprocessableError("IDR-IDC-006"));
        assertFalse(idrepoDraftService.isReprocessableError("IDR-IDC-007"));
        assertFalse(idrepoDraftService.isReprocessableError("IDR-IDC-009"));
        assertFalse(idrepoDraftService.isReprocessableError("IDR-IDC-010"));
        assertFalse(idrepoDraftService.isReprocessableError("IDR-IDC-012"));
        assertFalse(idrepoDraftService.isReprocessableError("IDR-IDC-015"));
        assertFalse(idrepoDraftService.isReprocessableError("IDR-IDS-011"));
        assertFalse(idrepoDraftService.isReprocessableError("IDR-VID-002"));
        assertFalse(idrepoDraftService.isReprocessableError(null));
    }

    @Test
    public void idrepoCreateDraftV2RecordAlreadyExists_ThrowsFailed() throws Exception {
        when(registrationProcessorRestClientService.postApi(
                eq(ApiName.IDREPOCREATEDRAFT), any(), any(), any(), any(), eq(ResponseWrapper.class)))
                .thenReturn(errorWrapper("IDR-IDC-012", "Record already exists in DB"));

        try {
            idrepoDraftService.idrepoCreateDraftV2(ID, null, true);
            fail("Expected IdrepoDraftException to be thrown");
        } catch (IdrepoDraftException e) {
            assertEquals("IDR-IDC-012", e.getErrorCode());
        }
    }

    @Test
    public void idrepoCreateDraftV2NoRecord_ThrowsFailed() throws Exception {
        when(registrationProcessorRestClientService.postApi(
                eq(ApiName.IDREPOCREATEDRAFT), any(), any(), any(), any(), eq(ResponseWrapper.class)))
                .thenReturn(errorWrapper("IDR-IDC-007", "No Record(s) found"));

        try {
            idrepoDraftService.idrepoCreateDraftV2(ID, null, true);
            fail("Expected IdrepoDraftException to be thrown");
        } catch (IdrepoDraftException e) {
            assertEquals("IDR-IDC-007", e.getErrorCode());
        }
    }

    @Test
    public void idrepoCreateDraftV2DbError_ThrowsFailed() throws Exception {
        when(registrationProcessorRestClientService.postApi(
                eq(ApiName.IDREPOCREATEDRAFT), any(), any(), any(), any(), eq(ResponseWrapper.class)))
                .thenReturn(errorWrapper("IDR-IDC-006", "DB operations failed"));

        try {
            idrepoDraftService.idrepoCreateDraftV2(ID, null, true);
            fail("Expected IdrepoDraftException to be thrown");
        } catch (IdrepoDraftException e) {
            assertEquals("IDR-IDC-006", e.getErrorCode());
        }
    }

    @Test
    public void idrepoCreateDraftV2CopyError_ThrowsReprocessable() throws Exception {
        when(registrationProcessorRestClientService.postApi(
                eq(ApiName.IDREPOCREATEDRAFT), any(), any(), any(), any(), eq(ResponseWrapper.class)))
                .thenReturn(errorWrapper("IDR-IDC-021", "Failed to copy live object(s) to draft path"));

        try {
            idrepoDraftService.idrepoCreateDraftV2(ID, null, true);
            fail("Expected IdrepoDraftReprocessableException to be thrown");
        } catch (IdrepoDraftReprocessableException e) {
            assertEquals("IDR-IDC-021", e.getErrorCode());
        }
        verify(registrationProcessorRestClientService, never())
                .deleteApi(eq(ApiName.IDREPODISCARDDRAFT), any(), any(), any(), any());
    }

    @Test(expected = IdrepoDraftReprocessableException.class)
    public void idrepoCreateDraftV2UnknownError_ThrowsReprocessable() throws Exception {
        when(registrationProcessorRestClientService.postApi(
                eq(ApiName.IDREPOCREATEDRAFT), any(), any(), any(), any(), eq(ResponseWrapper.class)))
                .thenReturn(errorWrapper("IDR-IDC-001", "Unknown error occurred"));

        idrepoDraftService.idrepoCreateDraftV2(ID, null, true);
    }

    @Test
    public void idrepoCreateDraftV2ValidationError_ThrowsFailed() throws Exception {
        when(registrationProcessorRestClientService.postApi(
                eq(ApiName.IDREPOCREATEDRAFT), any(), any(), any(), any(), eq(ResponseWrapper.class)))
                .thenReturn(errorWrapper("IDR-IDC-002", "Invalid Input Parameter"));

        try {
            idrepoDraftService.idrepoCreateDraftV2(ID, null, true);
            fail("Expected IdrepoDraftException to be thrown");
        } catch (IdrepoDraftException e) {
            assertEquals("IDR-IDC-002", e.getErrorCode());
        }
    }

    @Test
    public void idrepoUpdateDraftV2ReprocessableError_DoesNotDiscard() throws Exception {
        RequestDto requestDto = new RequestDto();
        requestDto.setIdentity(idResponseDTO.getResponse().getIdentity());
        IdRequestDto idRequestDto = new IdRequestDto();
        idRequestDto.setRequest(requestDto);
        when(registrationProcessorRestClientService.headApi(
                ApiName.IDREPOHASDRAFT, Lists.newArrayList(ID), null, null)).thenReturn(200);
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOGETDRAFT, Lists.newArrayList(ID), Lists.emptyList(), null, IdResponseDTO.class))
                .thenReturn(idResponseDTO);
        when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
                .thenReturn(errorIdResponse("IDR-IDS-004", "Object store failed"));

        try {
            idrepoDraftService.idrepoUpdateDraftV2(ID, null, idRequestDto, true);
            fail("Expected IdrepoDraftReprocessableException to be thrown");
        } catch (IdrepoDraftReprocessableException e) {
            assertEquals("IDR-IDS-004", e.getErrorCode());
        }
        verify(registrationProcessorRestClientService, never())
                .deleteApi(eq(ApiName.IDREPODISCARDDRAFT), any(), any(), any(), any());
    }

    @Test(expected = IdrepoDraftReprocessableException.class)
    public void idrepoUpdateDraftV2UnknownError_ThrowsReprocessable() throws Exception {
        RequestDto requestDto = new RequestDto();
        requestDto.setIdentity(idResponseDTO.getResponse().getIdentity());
        IdRequestDto idRequestDto = new IdRequestDto();
        idRequestDto.setRequest(requestDto);
        when(registrationProcessorRestClientService.headApi(
                ApiName.IDREPOHASDRAFT, Lists.newArrayList(ID), null, null)).thenReturn(200);
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOGETDRAFT, Lists.newArrayList(ID), Lists.emptyList(), null, IdResponseDTO.class))
                .thenReturn(idResponseDTO);
        when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
                .thenReturn(errorIdResponse("IDR-IDC-001", "Unknown error occurred"));

        try {
            idrepoDraftService.idrepoUpdateDraftV2(ID, null, idRequestDto, true);
        } finally {
            verify(registrationProcessorRestClientService, never())
                    .deleteApi(eq(ApiName.IDREPODISCARDDRAFT), any(), any(), any(), any());
        }
    }

    @Test
    public void idrepoUpdateDraftV2ValidationError_ThrowsFailedAndDoesNotDiscard() throws Exception {
        RequestDto requestDto = new RequestDto();
        requestDto.setIdentity(idResponseDTO.getResponse().getIdentity());
        IdRequestDto idRequestDto = new IdRequestDto();
        idRequestDto.setRequest(requestDto);
        when(registrationProcessorRestClientService.headApi(
                ApiName.IDREPOHASDRAFT, Lists.newArrayList(ID), null, null)).thenReturn(200);
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOGETDRAFT, Lists.newArrayList(ID), Lists.emptyList(), null, IdResponseDTO.class))
                .thenReturn(idResponseDTO);
        when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
                .thenReturn(errorIdResponse("IDR-IDC-005", "Input Data Validation Failed"));

        try {
            idrepoDraftService.idrepoUpdateDraftV2(ID, null, idRequestDto, true);
            fail("Expected IdrepoDraftException to be thrown");
        } catch (IdrepoDraftException e) {
            assertEquals("IDR-IDC-005", e.getErrorCode());
        }
        verify(registrationProcessorRestClientService, never())
                .deleteApi(eq(ApiName.IDREPODISCARDDRAFT), any(), any(), any(), any());
    }

    @Test
    public void idrepoUpdateDraftV2NoRecord_ThrowsFailed() throws Exception {
        RequestDto requestDto = new RequestDto();
        requestDto.setIdentity(idResponseDTO.getResponse().getIdentity());
        IdRequestDto idRequestDto = new IdRequestDto();
        idRequestDto.setRequest(requestDto);
        when(registrationProcessorRestClientService.headApi(
                ApiName.IDREPOHASDRAFT, Lists.newArrayList(ID), null, null)).thenReturn(200);
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOGETDRAFT, Lists.newArrayList(ID), Lists.emptyList(), null, IdResponseDTO.class))
                .thenReturn(idResponseDTO);
        when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
                .thenReturn(errorIdResponse("IDR-IDC-007", "No Record(s) found"));

        try {
            idrepoDraftService.idrepoUpdateDraftV2(ID, null, idRequestDto, true);
            fail("Expected IdrepoDraftException to be thrown");
        } catch (IdrepoDraftException e) {
            assertEquals("IDR-IDC-007", e.getErrorCode());
        }
    }

    @Test
    public void idrepoGetDraftReprocessableError_ThrowsReprocessable() throws Exception {
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOGETDRAFT, Lists.newArrayList(ID), "type", "demographics", IdResponseDTO.class))
                .thenReturn(errorIdResponse("IDR-IDS-003", "Key manager failed"));

        try {
            idrepoDraftService.idrepoGetDraft(ID, "demographics");
            fail("Expected IdrepoDraftReprocessableException to be thrown");
        } catch (IdrepoDraftReprocessableException e) {
            assertEquals("IDR-IDS-003", e.getErrorCode());
        }
    }

    @Test(expected = IdrepoDraftException.class)
    public void idrepoGetDraftUnknownError_ThrowsFailed() throws Exception {
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOGETDRAFT, Lists.newArrayList(ID), "type", "demographics", IdResponseDTO.class))
                .thenReturn(errorIdResponse("IDR-IDC-001", "Unknown error occurred"));

        idrepoDraftService.idrepoGetDraft(ID, "demographics");
    }

    @Test
    public void idrepoGetDraftNoRecord_ThrowsFailed() throws Exception {
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOGETDRAFT, Lists.newArrayList(ID), "type", "demographics", IdResponseDTO.class))
                .thenReturn(errorIdResponse("IDR-IDC-007", "No Record(s) found"));

        try {
            idrepoDraftService.idrepoGetDraft(ID, "demographics");
            fail("Expected IdrepoDraftException to be thrown");
        } catch (IdrepoDraftException e) {
            assertEquals("IDR-IDC-007", e.getErrorCode());
        }
    }

    @Test
    public void idrepoGetDraftRecordExists_ThrowsFailed() throws Exception {
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOGETDRAFT, Lists.newArrayList(ID), "type", "demographics", IdResponseDTO.class))
                .thenReturn(errorIdResponse("IDR-IDC-012", "Record already exists in DB"));

        try {
            idrepoDraftService.idrepoGetDraft(ID, "demographics");
            fail("Expected IdrepoDraftException to be thrown");
        } catch (IdrepoDraftException e) {
            assertEquals("IDR-IDC-012", e.getErrorCode());
        }
    }

    @Test
    public void idrepoUpdateDraftUinReprocessableError_ThrowsReprocessable() throws Exception {
        when(mapper.createObjectNode()).thenReturn(new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode());
        when(registrationProcessorRestClientService.patchApi(
                eq(ApiName.IDREPOUPDATEDRAFTUIN), any(), any(), any(), any(), any()))
                .thenReturn(errorIdResponse("IDR-IDS-003", "Key manager failed"));

        try {
            idrepoDraftService.idrepoUpdateDraftUin(ID, "1234567890");
            fail("Expected IdrepoDraftReprocessableException to be thrown");
        } catch (IdrepoDraftReprocessableException e) {
            assertEquals("IDR-IDS-003", e.getErrorCode());
        }
    }

    @Test
    public void idrepoUpdateDraftUinRecordExists_ThrowsFailed() throws Exception {
        when(mapper.createObjectNode()).thenReturn(new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode());
        when(registrationProcessorRestClientService.patchApi(
                eq(ApiName.IDREPOUPDATEDRAFTUIN), any(), any(), any(), any(), any()))
                .thenReturn(errorIdResponse("IDR-IDC-012", "Record already exists in DB"));

        try {
            idrepoDraftService.idrepoUpdateDraftUin(ID, "1234567890");
            fail("Expected IdrepoDraftException to be thrown");
        } catch (IdrepoDraftException e) {
            assertEquals("IDR-IDC-012", e.getErrorCode());
        }
    }

    @Test
    public void idrepoPublishDraftReprocessableError_DoesNotDiscard() throws Exception {
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOPUBLISHDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class))
                .thenReturn(errorIdResponse("IDR-IDC-019", "Failed to move draft objects"));

        try {
            idrepoDraftService.idrepoPublishDraft(ID);
            fail("Expected IdrepoDraftReprocessableException to be thrown");
        } catch (IdrepoDraftReprocessableException e) {
            assertEquals("IDR-IDC-019", e.getErrorCode());
        }
        verify(registrationProcessorRestClientService, never())
                .deleteApi(eq(ApiName.IDREPODISCARDDRAFT), any(), any(), any(), any());
    }

    @Test
    public void idrepoPublishDraftVidError_ThrowsFailedAndDiscards() throws Exception {
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOPUBLISHDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class))
                .thenReturn(errorIdResponse("IDR-VID-002", "Failed to generate VID"));
        when(registrationProcessorRestClientService.deleteApi(
                ApiName.IDREPODISCARDDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class))
                .thenReturn(errorIdResponse("IDR-IDC-007", "No Record(s) found"));

        try {
            idrepoDraftService.idrepoPublishDraft(ID);
            fail("Expected IdrepoDraftException to be thrown");
        } catch (IdrepoDraftException e) {
            assertEquals("IDR-VID-002", e.getErrorCode());
        }
        verify(registrationProcessorRestClientService)
                .deleteApi(eq(ApiName.IDREPODISCARDDRAFT), any(), any(), any(), any());
    }

    @Test
    public void idrepoPublishDraftNoRecord_ThrowsFailed() throws Exception {
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOPUBLISHDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class))
                .thenReturn(errorIdResponse("IDR-IDC-007", "No Record(s) found"));
        when(registrationProcessorRestClientService.deleteApi(
                ApiName.IDREPODISCARDDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class))
                .thenReturn(errorIdResponse("IDR-IDC-007", "No Record(s) found"));

        try {
            idrepoDraftService.idrepoPublishDraft(ID);
            fail("Expected IdrepoDraftException to be thrown");
        } catch (IdrepoDraftException e) {
            assertEquals("IDR-IDC-007", e.getErrorCode());
        }
        verify(registrationProcessorRestClientService)
                .deleteApi(eq(ApiName.IDREPODISCARDDRAFT), any(), any(), any(), any());
    }

    @Test
    public void idrepoDiscardDraftNoRecord_TreatsAsSuccess() throws Exception {
        when(registrationProcessorRestClientService.deleteApi(
                ApiName.IDREPODISCARDDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class))
                .thenReturn(errorIdResponse("IDR-IDC-007", "No Record(s) found"));

        assertTrue(idrepoDraftService.idrepoDiscardDraft(ID));
    }

    @Test
    public void idrepoDiscardDraftObjectStoreDeleteError_ThrowsReprocessable() throws Exception {
        when(registrationProcessorRestClientService.deleteApi(
                ApiName.IDREPODISCARDDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class))
                .thenReturn(errorIdResponse("IDR-IDC-020", "Failed to delete draft objects"));

        try {
            idrepoDraftService.idrepoDiscardDraft(ID);
            fail("Expected IdrepoDraftReprocessableException to be thrown");
        } catch (IdrepoDraftReprocessableException e) {
            assertEquals("IDR-IDC-020", e.getErrorCode());
        }
    }

    @Test
    public void idrepoCreateDraftV2UinGenerateError_ThrowsFailed() throws Exception {
        when(registrationProcessorRestClientService.postApi(
                eq(ApiName.IDREPOCREATEDRAFT), any(), any(), any(), any(), eq(ResponseWrapper.class)))
                .thenReturn(errorWrapper("IDR-IDS-011", "Failed to generate UIN"));

        try {
            idrepoDraftService.idrepoCreateDraftV2(ID, null, true);
            fail("Expected IdrepoDraftException to be thrown");
        } catch (IdrepoDraftException e) {
            assertEquals("IDR-IDS-011", e.getErrorCode());
        }
    }

    @Test
    public void idrepoCreateDraftV2NullResponseTest() throws Exception {
        when(registrationProcessorRestClientService.postApi(
                eq(ApiName.IDREPOCREATEDRAFT), any(), any(), any(), any(), eq(ResponseWrapper.class)))
                .thenReturn(null);

        try {
            idrepoDraftService.idrepoCreateDraftV2(ID, null, true);
            fail("Expected ApisResourceAccessException to be thrown");
        } catch (ApisResourceAccessException e) {
            assertNullIdRepoResponse("create draft", e);
        }
    }

    @Test
    public void idrepoGetDraftNullResponseTest() throws Exception {
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOGETDRAFT, Lists.newArrayList(ID), Lists.emptyList(), null, IdResponseDTO.class))
                .thenReturn(null);

        try {
            idrepoDraftService.idrepoGetDraft(ID);
            fail("Expected ApisResourceAccessException to be thrown");
        } catch (ApisResourceAccessException e) {
            assertNullIdRepoResponse("get draft", e);
        }
    }

    @Test
    public void idrepoUpdateDraftV2NullResponseTest() throws Exception {
        RequestDto requestDto = new RequestDto();
        requestDto.setIdentity(idResponseDTO.getResponse().getIdentity());
        IdRequestDto idRequestDto = new IdRequestDto();
        idRequestDto.setRequest(requestDto);
        when(registrationProcessorRestClientService.headApi(
                ApiName.IDREPOHASDRAFT, Lists.newArrayList(ID), null, null)).thenReturn(200);
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOGETDRAFT, Lists.newArrayList(ID), Lists.emptyList(), null, IdResponseDTO.class))
                .thenReturn(idResponseDTO);
        when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
                .thenReturn(null);

        try {
            idrepoDraftService.idrepoUpdateDraftV2(ID, null, idRequestDto, true);
            fail("Expected ApisResourceAccessException to be thrown");
        } catch (ApisResourceAccessException e) {
            assertNullIdRepoResponse("update draft", e);
        }
        verify(registrationProcessorRestClientService, never())
                .deleteApi(eq(ApiName.IDREPODISCARDDRAFT), any(), any(), any(), any());
    }

    @Test
    public void idrepoPublishDraftNullResponseTest() throws Exception {
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOPUBLISHDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class))
                .thenReturn(null);

        try {
            idrepoDraftService.idrepoPublishDraft(ID);
            fail("Expected ApisResourceAccessException to be thrown");
        } catch (ApisResourceAccessException e) {
            assertNullIdRepoResponse("publish draft", e);
        }
        verify(registrationProcessorRestClientService, never())
                .deleteApi(eq(ApiName.IDREPODISCARDDRAFT), any(), any(), any(), any());
    }

    @Test
    public void idrepoDiscardDraftNullResponseTest() throws Exception {
        when(registrationProcessorRestClientService.deleteApi(
                ApiName.IDREPODISCARDDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class))
                .thenReturn(null);

        try {
            idrepoDraftService.idrepoDiscardDraft(ID);
            fail("Expected ApisResourceAccessException to be thrown");
        } catch (ApisResourceAccessException e) {
            assertNullIdRepoResponse("discard draft", e);
        }
    }

    @Test
    public void setReprocessableErrorCodes_NullConfig_ClearsDefaults() {
        idrepoDraftService.setReprocessableErrorCodes(null);

        assertFalse(idrepoDraftService.isReprocessableError("IDR-IDS-004"));
        assertFalse(idrepoDraftService.isReprocessableError("IDR-IDC-019"));
    }

    @Test
    public void setReprocessableErrorCodes_BlankConfig_ClearsDefaults() {
        idrepoDraftService.setReprocessableErrorCodes("   ");

        assertFalse(idrepoDraftService.isReprocessableError("IDR-IDS-004"));
        assertFalse(idrepoDraftService.isReprocessableError("IDR-IDS-003"));
    }

    @Test
    public void setReprocessableErrorCodes_SkipsWhitespaceAndEmptyTokens() {
        idrepoDraftService.setReprocessableErrorCodes(" IDR-IDS-004 ,  ,\t,IDR-IDC-019, ");

        assertTrue(idrepoDraftService.isReprocessableError("IDR-IDS-004"));
        assertTrue(idrepoDraftService.isReprocessableError("idr-idc-019"));
        assertFalse(idrepoDraftService.isReprocessableError(""));
        assertFalse(idrepoDraftService.isReprocessableError("IDR-IDS-003"));
    }

    @Test
    public void setReprocessableErrorCodes_OverrideReplacesDefaults() {
        idrepoDraftService.setReprocessableErrorCodes("CUSTOM-ERR-1");

        assertTrue(idrepoDraftService.isReprocessableError("CUSTOM-ERR-1"));
        assertTrue(idrepoDraftService.isReprocessableError("custom-err-1"));
        assertFalse(idrepoDraftService.isReprocessableError("IDR-IDS-004"));
        assertFalse(idrepoDraftService.isReprocessableError("IDR-IDC-019"));
    }

    @Test
    public void idrepoGetDraftEmptyErrors_Succeeds() throws Exception {
        IdResponseDTO response = new IdResponseDTO();
        response.setErrors(Collections.emptyList());
        response.setResponse(idResponseDTO.getResponse());
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOGETDRAFT, Lists.newArrayList(ID), Lists.emptyList(), null, IdResponseDTO.class))
                .thenReturn(response);

        ResponseDTO result = idrepoDraftService.idrepoGetDraft(ID);

        assertEquals(ID, result.getRegistrationId());
    }

    @Test
    public void idrepoGetDraftNullErrorEntry_Succeeds() throws Exception {
        ArrayList<ErrorDTO> errors = new ArrayList<>();
        errors.add(null);
        IdResponseDTO response = new IdResponseDTO();
        response.setErrors(errors);
        response.setResponse(idResponseDTO.getResponse());
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOGETDRAFT, Lists.newArrayList(ID), Lists.emptyList(), null, IdResponseDTO.class))
                .thenReturn(response);

        ResponseDTO result = idrepoDraftService.idrepoGetDraft(ID);

        assertEquals(ID, result.getRegistrationId());
    }

    @Test(expected = IdrepoDraftException.class)
    public void idrepoCreateDraftV2NullErrorCode_ThrowsFailed() throws Exception {
        when(registrationProcessorRestClientService.postApi(
                eq(ApiName.IDREPOCREATEDRAFT), any(), any(), any(), any(), eq(ResponseWrapper.class)))
                .thenReturn(errorWrapper(null, "unknown"));

        idrepoDraftService.idrepoCreateDraftV2(ID, null, true);
    }

    @Test(expected = IdrepoDraftException.class)
    public void idrepoUpdateDraftV2NullErrorCode_ThrowsFailed() throws Exception {
        IdRequestDto idRequestDto = requestWithIdentity(idResponseDTO.getResponse().getIdentity());
        stubDraftPresent();
        when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
                .thenReturn(errorIdResponse(null, "unknown"));

        idrepoDraftService.idrepoUpdateDraftV2(ID, null, idRequestDto, true);
    }

    @Test(expected = IdrepoDraftReprocessableException.class)
    public void idrepoCreateDraftV2LowercaseUnknownError_ThrowsReprocessable() throws Exception {
        when(registrationProcessorRestClientService.postApi(
                eq(ApiName.IDREPOCREATEDRAFT), any(), any(), any(), any(), eq(ResponseWrapper.class)))
                .thenReturn(errorWrapper("idr-idc-004", "Unknown error occurred"));

        idrepoDraftService.idrepoCreateDraftV2(ID, null, true);
    }

    @Test(expected = IdrepoDraftReprocessableException.class)
    public void idrepoUpdateDraftV2LowercaseUnknownError_ThrowsReprocessable() throws Exception {
        IdRequestDto idRequestDto = requestWithIdentity(idResponseDTO.getResponse().getIdentity());
        stubDraftPresent();
        when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
                .thenReturn(errorIdResponse("idr-idc-004", "Unknown error occurred"));

        try {
            idrepoDraftService.idrepoUpdateDraftV2(ID, null, idRequestDto, true);
        } finally {
            verify(registrationProcessorRestClientService, never())
                    .deleteApi(eq(ApiName.IDREPODISCARDDRAFT), any(), any(), any(), any());
        }
    }

    @Test(expected = IdrepoDraftException.class)
    public void idrepoPublishDraftUnknownError_ThrowsFailedAndDiscards() throws Exception {
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOPUBLISHDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class))
                .thenReturn(errorIdResponse("IDR-IDC-001", "Unknown error occurred"));
        stubDiscardSuccess();

        try {
            idrepoDraftService.idrepoPublishDraft(ID);
        } finally {
            verify(registrationProcessorRestClientService)
                    .deleteApi(eq(ApiName.IDREPODISCARDDRAFT), any(), any(), any(), any());
        }
    }

    @Test(expected = IdrepoDraftException.class)
    public void idrepoDiscardDraftUnknownError_ThrowsFailed() throws Exception {
        when(registrationProcessorRestClientService.deleteApi(
                ApiName.IDREPODISCARDDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class))
                .thenReturn(errorIdResponse("IDR-IDC-001", "Unknown error occurred"));

        idrepoDraftService.idrepoDiscardDraft(ID);
    }

    @Test(expected = IdrepoDraftException.class)
    public void idrepoUpdateDraftUinUnknownError_ThrowsFailed() throws Exception {
        when(mapper.createObjectNode()).thenReturn(new ObjectMapper().createObjectNode());
        when(registrationProcessorRestClientService.patchApi(
                eq(ApiName.IDREPOUPDATEDRAFTUIN), any(), any(), any(), any(), any()))
                .thenReturn(errorIdResponse("IDR-IDC-001", "Unknown error occurred"));

        idrepoDraftService.idrepoUpdateDraftUin(ID, "1234567890");
    }

    @Test(expected = IdrepoDraftException.class)
    public void idrepoDiscardDraftHardFailCode_ThrowsFailed() throws Exception {
        when(registrationProcessorRestClientService.deleteApi(
                ApiName.IDREPODISCARDDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class))
                .thenReturn(errorIdResponse("IDR-IDC-005", "Input Data Validation Failed"));

        idrepoDraftService.idrepoDiscardDraft(ID);
    }

    @Test
    public void idrepoUpdateDraftV2Merge_ExistingIdentityNull_UsesIncomingIdentity() throws Exception {
        JSONObject incomingIdentity = new JSONObject();
        incomingIdentity.put("email", "lost@example.com");
        IdRequestDto idRequestDto = requestWithIdentity(incomingIdentity);
        idResponseDTO.getResponse().setIdentity(null);
        stubDraftPresent();
        stubIdentityMapper(incomingIdentity);
        when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
                .thenReturn(idResponseDTO);

        idrepoDraftService.idrepoUpdateDraftV2(ID, null, idRequestDto, false);

        JSONObject merged = capturePatchedIdentity();
        assertEquals("lost@example.com", merged.get("email"));
        assertFalse(merged.containsKey("UIN"));
    }

    @Test
    public void idrepoUpdateDraftV2Merge_IncomingIdentityNull_CopiesExistingUin() throws Exception {
        JSONObject existingIdentity = new JSONObject();
        existingIdentity.put("UIN", "9999");
        existingIdentity.put("fullName", "Bob");
        idResponseDTO.getResponse().setIdentity(existingIdentity);
        IdRequestDto idRequestDto = requestWithIdentity(null);
        stubDraftPresent();
        stubIdentityMapper(existingIdentity);
        when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
                .thenReturn(idResponseDTO);

        idrepoDraftService.idrepoUpdateDraftV2(ID, null, idRequestDto, false);

        JSONObject merged = capturePatchedIdentity();
        assertEquals("9999", merged.get("UIN"));
        assertFalse(merged.containsKey("fullName"));
        assertFalse(merged.containsKey("email"));
    }

    @Test
    public void idrepoUpdateDraftV2Merge_ExistingIdentityWithoutUin_DoesNotStampUin() throws Exception {
        JSONObject existingIdentity = new JSONObject();
        existingIdentity.put("fullName", "Bob");
        JSONObject incomingIdentity = new JSONObject();
        incomingIdentity.put("email", "lost@example.com");
        idResponseDTO.getResponse().setIdentity(existingIdentity);
        IdRequestDto idRequestDto = requestWithIdentity(incomingIdentity);
        stubDraftPresent();
        when(mapper.writeValueAsString(any())).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            return arg == existingIdentity ? "existing" : "incoming";
        });
        when(mapper.readValue("existing", JSONObject.class)).thenReturn(existingIdentity);
        when(mapper.readValue("incoming", JSONObject.class)).thenReturn(incomingIdentity);
        when(registrationProcessorRestClientService.patchApi(any(), any(), any(), any(), any(), any()))
                .thenReturn(idResponseDTO);

        idrepoDraftService.idrepoUpdateDraftV2(ID, null, idRequestDto, false);

        JSONObject merged = capturePatchedIdentity();
        assertEquals("lost@example.com", merged.get("email"));
        assertFalse(merged.containsKey("UIN"));
        assertFalse(merged.containsKey("fullName"));
    }

    private IdRequestDto requestWithIdentity(Object identity) {
        RequestDto requestDto = new RequestDto();
        requestDto.setIdentity(identity);
        IdRequestDto idRequestDto = new IdRequestDto();
        idRequestDto.setRequest(requestDto);
        return idRequestDto;
    }

    private void stubDraftPresent() throws Exception {
        when(registrationProcessorRestClientService.headApi(
                ApiName.IDREPOHASDRAFT, Lists.newArrayList(ID), null, null)).thenReturn(200);
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOGETDRAFT, Lists.newArrayList(ID), Lists.emptyList(), null, IdResponseDTO.class))
                .thenReturn(idResponseDTO);
    }

    private void stubDiscardSuccess() throws Exception {
        IdResponseDTO discardResponse = new IdResponseDTO();
        discardResponse.setErrors(null);
        when(registrationProcessorRestClientService.deleteApi(
                ApiName.IDREPODISCARDDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class))
                .thenReturn(discardResponse);
    }

    private void stubIdentityMapper(JSONObject identity) throws Exception {
        when(mapper.writeValueAsString(any())).thenReturn("identity");
        when(mapper.readValue("identity", JSONObject.class)).thenReturn(identity);
    }

    private JSONObject capturePatchedIdentity() throws Exception {
        ArgumentCaptor<IdRequestDto> requestCaptor = ArgumentCaptor.forClass(IdRequestDto.class);
        verify(registrationProcessorRestClientService).patchApi(any(), any(), any(), any(), requestCaptor.capture(), any());
        return (JSONObject) requestCaptor.getValue().getRequest().getIdentity();
    }

    private void assertNullIdRepoResponse(String apiName, ApisResourceAccessException e) {
        assertEquals(PlatformErrorMessages.RPR_CDS_IDREPO_NULL_RESPONSE.getCode(), e.getErrorCode());
        assertEquals(String.format(PlatformErrorMessages.RPR_CDS_IDREPO_NULL_RESPONSE.getMessage(), apiName),
                e.getErrorText());
    }

    private static ErrorDTO error(String code, String message) {
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setErrorCode(code);
        errorDTO.setMessage(message);
        return errorDTO;
    }

    private static IdResponseDTO errorIdResponse(String code, String message) {
        IdResponseDTO dto = new IdResponseDTO();
        dto.setErrors(Lists.newArrayList(error(code, message)));
        return dto;
    }

    private static ResponseWrapper errorWrapper(String code, String message) {
        ResponseWrapper wrapper = new ResponseWrapper();
        wrapper.setErrors(Lists.newArrayList(error(code, message)));
        return wrapper;
    }
}
