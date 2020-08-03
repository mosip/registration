package io.mosip.registration.device.webcam.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.JPanel;

import org.springframework.stereotype.Component;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.device.webcam.MosipWebcamServiceImpl;

/**
 * class to access the webcam and its functionalities.
 *
 * @author Himaja Dhanyamraju
 */
@Component
public class WebcamSarxosServiceImpl extends MosipWebcamServiceImpl {
	/**
	 * Instance of {@link MosipLogger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(WebcamSarxosServiceImpl.class);

	private Webcam webcam;

	private JPanel jPanelWindow;

	@Override
	public JPanel getCameraPanel() {
		if (jPanelWindow == null) {
			getJPanel(webcam);
		}
		return jPanelWindow;
	}

	public JPanel getJPanel(Webcam webcam) {
		WebcamPanel cameraPanel = new WebcamPanel(webcam);
		jPanelWindow = new JPanel();
		jPanelWindow.add(cameraPanel);
		jPanelWindow.setVisible(true);
		return jPanelWindow;
	}

	@Override
	public boolean isWebcamConnected() {
		return webcam != null ? webcam.isOpen() : false;
	}
	
	public boolean isWebcamConnected(Webcam webcam) {
		return webcam != null ? webcam.isOpen() : false;
	}

	@Override
	public void connect(int width, int height) {
		LOGGER.info("REGISTRATION - WEBCAMDEVICE", APPLICATION_NAME, APPLICATION_ID, "connecting to webcam");

		String webcamName = String.valueOf(ApplicationContext.map().get(RegistrationConstants.WEBCAM_NAME));

		boolean found = false;
		List<Webcam> webcams = Webcam.getWebcams();

		StringBuilder webcamNames = new StringBuilder();
		String prefix = RegistrationConstants.EMPTY;
		for (Webcam webcamera : webcams) {
			webcamNames.append(prefix);
			prefix = RegistrationConstants.COMMA;
			webcamNames.append(webcamera.getName());
		}
		LOGGER.info("REGISTRATION - WEBCAMDEVICE", APPLICATION_NAME, APPLICATION_ID,
				"Available webcams that are plugged in: " + webcamNames.toString());

		if (!webcams.isEmpty()) {
			for (Webcam webcamera : webcams) {
				if (webcamera.getName().toLowerCase().contains(webcamName.toLowerCase())) {
					webcam = webcamera;
					found = true;
					break;
				}
			}
			if (!found) {
				webcam = webcams.get(0);
			}
			if (!webcam.isOpen()) {
				Dimension requiredDimension = new Dimension(width, height);
				webcam.setViewSize(requiredDimension);
				webcam.getLock().disable();
				webcam.open();
				Webcam.getDiscoveryService().stop();
			}
		}
	}

	@Override
	public BufferedImage captureImage() {
		LOGGER.info("REGISTRATION - WEBCAMDEVICE", APPLICATION_NAME, APPLICATION_ID, "capturing the image from webcam");
		return webcam != null ? webcam.getImage() : null;
	}
	
	/**
	 * Gets the web cams.
	 *
	 * @return the web cams
	 */
	public List<Webcam> getWebCams() {

		LOGGER.info("REGISTRATION - WEBCAMDEVICE", APPLICATION_NAME, APPLICATION_ID, "Get All Web cams");
		return Webcam.getWebcams();
	}
	

	@Override
	public void close() {
		LOGGER.info("REGISTRATION - WEBCAMDEVICE", APPLICATION_NAME, APPLICATION_ID, "closing the webcam");

		if (webcam != null && webcam.isOpen()) {
			jPanelWindow = null;
			webcam.close();
		}
	}
	
	@Override
	public void openWebCam(Webcam webcam, int width, int height) {

		LOGGER.info("REGISTRATION - WEBCAMDEVICE", APPLICATION_NAME, APPLICATION_ID,
				"Opening web cam with width : " + width + " and height : " + height);

		if (webcam != null && !webcam.isOpen()) {
			Dimension requiredDimension = new Dimension(width, height);
		
			webcam.setCustomViewSizes(new Dimension[] {
				   requiredDimension,         
				});
			webcam.setViewSize(requiredDimension);
			webcam.getLock().disable();
			
			//Open Web camera
			webcam.open();
			this.webcam = webcam;

			LOGGER.info("REGISTRATION - WEBCAMDEVICE", APPLICATION_NAME, APPLICATION_ID,
					"Stopping web camera discovery");

			Webcam.getDiscoveryService().stop();
		}
	}

	@Override
	public BufferedImage captureImage(Webcam webcam) {
		LOGGER.info("REGISTRATION - WEBCAMDEVICE", APPLICATION_NAME, APPLICATION_ID, "capturing the image from webcam");
		return webcam != null ? webcam.getImage() : null;
	}
	
	public void close(Webcam webcam) {
		LOGGER.info("REGISTRATION - WEBCAMDEVICE", APPLICATION_NAME, APPLICATION_ID, "closing the webcam");

		if (webcam != null && webcam.isOpen()) {
			jPanelWindow = null;
			webcam.close();
		}
	}
}
