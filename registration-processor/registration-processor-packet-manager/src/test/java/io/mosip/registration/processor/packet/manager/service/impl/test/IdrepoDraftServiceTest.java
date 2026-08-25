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

import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

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

        ReflectionTestUtils.setField(idrepoDraftService, "uinStampMaxRetry", 3);
        ReflectionTestUtils.setField(idrepoDraftService, "uinStampRetryDelayMs", 0L);
    }

    @Test
    public void idrepoDraftPresentTest() throws ApisResourceAccessException, IdrepoDraftException {

        when(registrationProcessorRestClientService.headApi
                (ApiName.IDREPOHASDRAFT, Lists.newArrayList(ID), null, null)).thenReturn(200);

        boolean result = idrepoDraftService.idrepoHasDraft(ID);

        assertTrue(result);
    }

    @Test
    public void idrepoDraftNotPresentTest() throws ApisResourceAccessException, IdrepoDraftException {

        when(registrationProcessorRestClientService.headApi
                (ApiName.IDREPOHASDRAFT, Lists.newArrayList(ID), null, null)).thenReturn(204);

        boolean result = idrepoDraftService.idrepoHasDraft(ID);

        assertFalse(result);
    }

    @Test(expected = IdrepoDraftException.class)
    public void idrepoDraftCheckExceptionTest() throws ApisResourceAccessException, IdrepoDraftException {

        when(registrationProcessorRestClientService.headApi
                (ApiName.IDREPOHASDRAFT, Lists.newArrayList(ID), null, null)).thenReturn(500);


        idrepoDraftService.idrepoHasDraft(ID);

    }

    @Test
    public void idrepoGetDraftSuccessTest() throws ApisResourceAccessException, IdrepoDraftException {

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
    public void idrepoCreateDraftSuccessTest() throws ApisResourceAccessException, IdrepoDraftException {
        ResponseWrapper responseWrapper = new ResponseWrapper();

        when(registrationProcessorRestClientService.postApi(
                ApiName.IDREPOCREATEDRAFT, Lists.newArrayList(ID), null, null, null, ResponseWrapper.class)).thenReturn(responseWrapper);

        boolean result = idrepoDraftService.idrepoCreateDraft(ID, null);
        assertTrue(result);
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
			throws ApisResourceAccessException, IdrepoDraftException, IOException {
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
			// expected — key manager error causes discard + reprocessable exception
		}
		verify(registrationProcessorRestClientService)
				.deleteApi(ApiName.IDREPODISCARDDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class);
	}
    @Test
    public void idrepoGetDraftWithTypeSuccessTest() throws ApisResourceAccessException, IdrepoDraftException {
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOGETDRAFT, Lists.newArrayList(ID), "type", "demographics", IdResponseDTO.class)).thenReturn(idResponseDTO);

        ResponseDTO result = idrepoDraftService.idrepoGetDraft(ID, "demographics");

        assertTrue(result.getRegistrationId().equals(ID));
    }

    @Test
    public void idrepoGetDraftWithBiometricsTypeTest() throws ApisResourceAccessException, IdrepoDraftException {
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOGETDRAFT, Lists.newArrayList(ID), "type", "biometrics", IdResponseDTO.class)).thenReturn(idResponseDTO);

        ResponseDTO result = idrepoDraftService.idrepoGetDraft(ID, "biometrics");

        assertTrue(result.getRegistrationId().equals(ID));
    }

    @Test
    public void idrepoGetDraftWithSupportingDocumentsTypeTest() throws ApisResourceAccessException, IdrepoDraftException {
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOGETDRAFT, Lists.newArrayList(ID), "type", "supportingDocuments", IdResponseDTO.class)).thenReturn(idResponseDTO);

        ResponseDTO result = idrepoDraftService.idrepoGetDraft(ID, "supportingDocuments");

        assertTrue(result.getRegistrationId().equals(ID));
    }

    @Test(expected = IdrepoDraftException.class)
    public void idrepoGetDraftWithTypeErrorTest() throws ApisResourceAccessException, IdrepoDraftException {
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
    public void idrepoCreateDraftV2SuccessTest() throws ApisResourceAccessException, IdrepoDraftException, IdrepoDraftReprocessableException {
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
    public void idrepoCreateDraftV2ExceptionTest() throws ApisResourceAccessException, IdrepoDraftException, IdrepoDraftReprocessableException {
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
    public void idrepoCreateDraftV2ConflictDiscardSucceedsRecreateSucceedsTest()
            throws ApisResourceAccessException, IdrepoDraftException, IdrepoDraftReprocessableException {
        ErrorDTO alreadyExistsError = new ErrorDTO();
        alreadyExistsError.setErrorCode("IDR-IDC-012");
        alreadyExistsError.setMessage("Record already exists");
        ResponseWrapper conflictResponse = new ResponseWrapper();
        conflictResponse.setErrors(Lists.newArrayList(alreadyExistsError));

        ResponseWrapper successResponse = new ResponseWrapper();

        when(registrationProcessorRestClientService.postApi(
                eq(ApiName.IDREPOCREATEDRAFT), any(), any(), any(), any(), eq(ResponseWrapper.class)))
                .thenReturn(conflictResponse, successResponse);

        IdResponseDTO discardIdResponseDto = new IdResponseDTO();
        discardIdResponseDto.setErrors(null);
        when(registrationProcessorRestClientService.deleteApi(
                ApiName.IDREPODISCARDDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class))
                .thenReturn(discardIdResponseDto);

        boolean result = idrepoDraftService.idrepoCreateDraftV2(ID, null, true);

        assertTrue(result);
    }

    @Test(expected = IdrepoDraftReprocessableException.class)
    public void idrepoCreateDraftV2ConflictDiscardFailsWithNoRecordFoundTest()
            throws ApisResourceAccessException, IdrepoDraftException, IdrepoDraftReprocessableException {
        ErrorDTO alreadyExistsError = new ErrorDTO();
        alreadyExistsError.setErrorCode("IDR-IDC-012");
        alreadyExistsError.setMessage("Record already exists");
        ResponseWrapper conflictResponse = new ResponseWrapper();
        conflictResponse.setErrors(Lists.newArrayList(alreadyExistsError));

        when(registrationProcessorRestClientService.postApi(
                eq(ApiName.IDREPOCREATEDRAFT), any(), any(), any(), any(), eq(ResponseWrapper.class)))
                .thenReturn(conflictResponse);

        ErrorDTO noRecordFoundError = new ErrorDTO();
        noRecordFoundError.setErrorCode("IDR-IDC-007");
        noRecordFoundError.setMessage("No record found");
        IdResponseDTO discardIdResponseDto = new IdResponseDTO();
        discardIdResponseDto.setErrors(Lists.newArrayList(noRecordFoundError));
        when(registrationProcessorRestClientService.deleteApi(
                ApiName.IDREPODISCARDDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class))
                .thenReturn(discardIdResponseDto);

        idrepoDraftService.idrepoCreateDraftV2(ID, null, true);
    }

    @Test
    public void idrepoUpdateDraftUinSuccessTest() throws ApisResourceAccessException, IdrepoDraftException, IdrepoDraftReprocessableException {
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
    public void idrepoUpdateDraftUinExceptionTest() throws ApisResourceAccessException, IdrepoDraftException, IdrepoDraftReprocessableException {
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

    @Test(expected = IdrepoDraftException.class)
    public void idrepoUpdateDraftUinNullResponseTest() throws ApisResourceAccessException, IdrepoDraftException, IdrepoDraftReprocessableException {
        when(mapper.createObjectNode()).thenReturn(new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode());
        when(registrationProcessorRestClientService.patchApi(
                eq(ApiName.IDREPOUPDATEDRAFTUIN), any(), any(), any(), any(), any())).thenReturn(null);

        idrepoDraftService.idrepoUpdateDraftUin(ID, "1234567890");
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

        idrepoDraftService.idrepoPublishDraft(ID);
    }

    @Test
    public void idrepoUpdateDraftRecordAlreadyExistsIdempotentTest()
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

        IdResponseDTO result = idrepoDraftService.idrepoUpdateDraft(ID, null, idRequestDto);

        assertTrue(result.getErrors().get(0).getErrorCode().equals("IDR-IDC-012"));
        verify(registrationProcessorRestClientService, never())
                .deleteApi(any(), any(), any(), any(), any());
    }

    @Test
    public void idrepoUpdateDraftUinRetrySuccessTest() throws ApisResourceAccessException, IdrepoDraftException, IdrepoDraftReprocessableException {
        ErrorDTO keyManagerError = new ErrorDTO();
        keyManagerError.setMessage("Key Manager error");
        keyManagerError.setErrorCode("IDR-IDS-003");
        IdResponseDTO keyManagerErrorResponse = new IdResponseDTO();
        keyManagerErrorResponse.setErrors(Lists.newArrayList(keyManagerError));

        when(mapper.createObjectNode()).thenReturn(new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode());
        when(registrationProcessorRestClientService.patchApi(
                eq(ApiName.IDREPOUPDATEDRAFTUIN), any(), any(), any(), any(), any()))
                .thenReturn(keyManagerErrorResponse)
                .thenReturn(idResponseDTO);

        boolean result = idrepoDraftService.idrepoUpdateDraftUin(ID, "1234567890");
        assertTrue(result);
        verify(registrationProcessorRestClientService, times(2)).patchApi(eq(ApiName.IDREPOUPDATEDRAFTUIN), any(), any(), any(), any(), any());
    }

    @Test(expected = IdrepoDraftReprocessableException.class)
    public void idrepoUpdateDraftUinRetryExhaustedTest() throws ApisResourceAccessException, IdrepoDraftException, IdrepoDraftReprocessableException {
        ErrorDTO keyManagerError = new ErrorDTO();
        keyManagerError.setMessage("Key Manager error");
        keyManagerError.setErrorCode("IDR-IDS-003");
        IdResponseDTO keyManagerErrorResponse = new IdResponseDTO();
        keyManagerErrorResponse.setErrors(Lists.newArrayList(keyManagerError));

        when(mapper.createObjectNode()).thenReturn(new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode());
        when(registrationProcessorRestClientService.patchApi(
                eq(ApiName.IDREPOUPDATEDRAFTUIN), any(), any(), any(), any(), any()))
                .thenReturn(keyManagerErrorResponse);

        idrepoDraftService.idrepoUpdateDraftUin(ID, "1234567890");
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
}
