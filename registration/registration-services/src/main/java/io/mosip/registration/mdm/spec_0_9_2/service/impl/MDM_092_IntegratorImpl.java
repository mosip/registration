package io.mosip.registration.mdm.spec_0_9_2.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.List;

import org.apache.http.ParseException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.mosip.registration.dto.json.metadata.DigitalId;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.mdm.dto.CaptureResponseDto;
import io.mosip.registration.mdm.dto.DeviceDiscoveryResponsetDto;
import io.mosip.registration.mdm.integrator.IMosipBioDeviceIntegrator;
import io.mosip.registration.mdm.spec_0_9_2.dto.response.DeviceInfoResponse;

@Service
public class MDM_092_IntegratorImpl implements IMosipBioDeviceIntegrator {

	@Override
	public Object getDeviceInfo(String url, Class<?> responseType) throws RegBaseCheckedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<DeviceDiscoveryResponsetDto> getDeviceDiscovery(String url, String deviceType, Class<?> responseType)
			throws RegBaseCheckedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CaptureResponseDto capture(String url, Object request, Class<?> responseType)
			throws RegBaseCheckedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CaptureResponseDto getFrame() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CaptureResponseDto forceCapture() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CaptureResponseDto responseParsing() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean jwsValidation(String jwsResponse) throws RegBaseCheckedException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object decodeRCaptureData(String data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object rCapture(String url, Object registrationCaptureRequestDto)
			throws JsonParseException, JsonMappingException, ParseException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream stream(String url, Object registrationStreamRequestDto)
			throws MalformedURLException, IOException {
		// TODO Auto-generated method stub
		return null;
	}}
