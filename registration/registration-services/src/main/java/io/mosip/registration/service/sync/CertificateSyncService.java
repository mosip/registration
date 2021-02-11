package io.mosip.registration.service.sync;

import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.exception.RegBaseCheckedException;

import java.net.SocketTimeoutException;

public interface CertificateSyncService {

    public ResponseDTO getCACertificates(String triggerPoint) throws RegBaseCheckedException;
}
