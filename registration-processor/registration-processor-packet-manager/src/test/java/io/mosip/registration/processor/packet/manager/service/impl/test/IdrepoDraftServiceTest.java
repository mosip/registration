package io.mosip.registration.processor.packet.manager.service.impl.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Lists;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

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
import io.mosip.registration.processor.packet.manager.idreposervice.IdrepoDraftService;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ IOUtils.class, HMACUtils2.class, Gson.class })
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
    public void idrepoPublishDraftSuccessTest() throws ApisResourceAccessException, IdrepoDraftException {

        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOPUBLISHDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class)).thenReturn(idResponseDTO);

        IdResponseDTO result = idrepoDraftService.idrepoPublishDraft(ID);

        assertTrue(result.getResponse().getRegistrationId().equals(ID));
    }
    
    @Test(expected = IdrepoDraftException.class)
    public void idrepoPublishDraftExceptionTest() throws ApisResourceAccessException, IdrepoDraftException {
    	RequestDto requestDto = new RequestDto();
        requestDto.setIdentity(idResponseDTO.getResponse().getIdentity());
        IdRequestDto idRequestDto = new IdRequestDto();
        idRequestDto.setRequest(requestDto);

        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage("ERROR");
        errorDTO.setErrorCode("ERROR");
        IdResponseDTO idResponseDTO1 = new IdResponseDTO();
        idResponseDTO1.setErrors(Lists.newArrayList(errorDTO));
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOPUBLISHDRAFT, Lists.newArrayList(ID), "", "", IdResponseDTO.class)).thenReturn(idResponseDTO1);

        idrepoDraftService.idrepoPublishDraft(ID);
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
    public void idrepoUpdateDraftSuccessTest() throws ApisResourceAccessException, IdrepoDraftException, IOException {
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

    @Test(expected = IdrepoDraftException.class)
    public void idrepoUpdateDraftExceptionTest() throws ApisResourceAccessException, IdrepoDraftException, IOException {
        RequestDto requestDto = new RequestDto();
        requestDto.setIdentity(idResponseDTO.getResponse().getIdentity());
        IdRequestDto idRequestDto = new IdRequestDto();
        idRequestDto.setRequest(requestDto);

        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage("ERROR");
        errorDTO.setErrorCode("ERROR");
        IdResponseDTO idResponseDTO1 = new IdResponseDTO();
        idResponseDTO1.setErrors(Lists.newArrayList(errorDTO));

        when(registrationProcessorRestClientService.headApi
                (ApiName.IDREPOHASDRAFT, Lists.newArrayList(ID), null, null)).thenReturn(200);
        when(registrationProcessorRestClientService.getApi(
                ApiName.IDREPOGETDRAFT, Lists.newArrayList(ID), Lists.emptyList(), null, IdResponseDTO.class)).thenReturn(idResponseDTO);
        when(registrationProcessorRestClientService.patchApi(
                any(), any(), any(), any(), any(), any())).thenReturn(idResponseDTO1);

        idrepoDraftService.idrepoUpdateDraft(ID, null, idRequestDto);

    }


}
