package io.mosip.registration.device.scanner.impl;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_DOC_SCAN_CONTROLLER;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import au.com.southsky.jfreesane.SaneDevice;
import au.com.southsky.jfreesane.SaneException;
import au.com.southsky.jfreesane.SaneSession;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;

/**
 * This class is used to provide document scanner functionalities for Linux
 * platform through SANE Daemon service
 * 
 * @author balamurugan.ramamoorthy
 * @since 1.0.0
 *
 */
@Service
public class DocumentScannerSaneServiceImpl extends DocumentScannerService {

	private static final Logger LOGGER = AppConfig.getLogger(DocumentScannerSaneServiceImpl.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.device.scanner.impl.DocumentScannerService#
	 * isScannerConnected()
	 */
	@Override
	public boolean isConnected() {

		return isListNotEmpty(getScannerDevices());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.device.scanner.impl.DocumentScannerService#scanDocument
	 * ()
	 */
	@Override
	public BufferedImage scan() {

		BufferedImage bufferedImage = null;
		try {
			SaneSession session = SaneSession.withRemoteSane(
					InetAddress.getByName(
							(String) ApplicationContext.map().get(RegistrationConstants.DOCUMENT_SCANNER_HOST)),
					(Long) ApplicationContext.map().get(RegistrationConstants.DOCUMENT_SCANNER_TIMEOUT),
					TimeUnit.MILLISECONDS);
			List<SaneDevice> saneDevices = session.listDevices();
			if (isListNotEmpty(saneDevices)) {
				SaneDevice saneDevice = saneDevices.get(0);

				saneDevice.open();

				setScannerSettings(saneDevice);
				bufferedImage = saneDevice.acquireImage();

				saneDevice.close();
				session.close();
			}
		} catch (IOException | SaneException exception) {
			LOGGER.error(LOG_REG_DOC_SCAN_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, exception.getMessage());
		}
		return bufferedImage;
	}

	/**
	 * This method is used to set the scanner settings for the given scanner device
	 * 
	 * @param saneDevice
	 *            - the scanner device
	 * @throws IOException
	 *             - holds the ioexception
	 * @throws SaneException
	 *             - holds the sane device exception
	 */
	private void setScannerSettings(SaneDevice saneDevice) throws IOException, SaneException {
		/* setting the resolution in dpi for the quality of the document */
		saneDevice.getOption("resolution").setIntegerValue(
				Integer.parseInt(String.valueOf(ApplicationContext.map().get(RegistrationConstants.DOC_SCAN_DPI))));

		saneDevice.getOption("brightness").setIntegerValue(Integer
				.parseInt(String.valueOf(ApplicationContext.map().get(RegistrationConstants.DOC_SCAN_BRIGHTNESS))));

		saneDevice.getOption("contrast").setIntegerValue(Integer
				.parseInt(String.valueOf(ApplicationContext.map().get(RegistrationConstants.DOC_SCAN_CONTRAST))));

		saneDevice.getOption("depth")
				.setIntegerValue((Integer) ApplicationContext.map().get(RegistrationConstants.DOCUMENT_SCANNER_DEPTH));

	}

	/**
	 * This method is used to get the lists of scanners connected to the machine
	 * 
	 * @return List<SaneDevice> - The list of connected scanner devices
	 */
	private List<SaneDevice> getScannerDevices() {
		List<SaneDevice> saneDevices = null;
		try {
			SaneSession session = SaneSession.withRemoteSane(
					InetAddress.getByName(
							(String) ApplicationContext.map().get(RegistrationConstants.DOCUMENT_SCANNER_HOST)),
					(Long) ApplicationContext.map().get(RegistrationConstants.DOCUMENT_SCANNER_TIMEOUT),
					TimeUnit.MILLISECONDS);
			saneDevices = session.listDevices();
			session.close();
		} catch (IOException | SaneException exception) {
			LOGGER.error(LOG_REG_DOC_SCAN_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, exception.getMessage());
		}
		return saneDevices;
	}
}
