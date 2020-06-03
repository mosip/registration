package io.mosip.registration.device.scanner.impl;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_DOC_SCAN_CONTROLLER;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.device.scanner.IMosipDocumentScannerService;

/**
 * This class is used to handle all the requests related to scanner devices
 * through Sane Daemon service
 * 
 * @author balamurugan.ramamoorthy
 * @since 1.0.0
 */
@Service
public abstract class DocumentScannerService implements IMosipDocumentScannerService {

//	@Value("${DOCUMENT_SCANNER_DEPTH}")
//	protected int scannerDepth;
//
//	@Value("${DOCUMENT_SCANNER_HOST}")
//	protected String scannerhost;
//
//	@Value("${DOCUMENT_SCANNER_PORT}")
//	protected int scannerPort;
//
//	@Value("${DOCUMENT_SCANNER_TIMEOUT}")
//	protected long scannerTimeout;

	private static final Logger LOGGER = AppConfig.getLogger(DocumentScannerService.class);

	/**
	 * This method converts the BufferedImage to byte[]
	 * 
	 * @param bufferedImage
	 *            - holds the scanned image from the scanner
	 * @return byte[] - scanned document Content
	 * @throws IOException - holds the IOExcepion
	 */
	public byte[] getImageBytesFromBufferedImage(BufferedImage bufferedImage) throws IOException {
		byte[] imageInByte;

		ByteArrayOutputStream imagebyteArray = new ByteArrayOutputStream();
		ImageIO.write(bufferedImage, RegistrationConstants.SCANNER_IMG_TYPE, imagebyteArray);
		imagebyteArray.flush();
		imageInByte = imagebyteArray.toByteArray();
		imagebyteArray.close();

		return imageInByte;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.device.scanner.impl.DocumentScannerService#
	 * getSinglePDFInBytes(java.util.List)
	 */
	@Override
	public byte[] asPDF(List<BufferedImage> bufferedImages) {

		byte[] scannedPdfFile = null;
		Document document = new Document();
		try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

			PdfWriter writer = PdfWriter.getInstance(document, byteArrayOutputStream);
			document.open();

			PdfContentByte pdfPage = new PdfContentByte(writer);
			for (BufferedImage bufferedImage : bufferedImages) {
				Image image = Image.getInstance(pdfPage, bufferedImage, 1);
				image.scaleToFit(600, 750);
				document.add(image);
			}

			document.close();
			writer.close();
			scannedPdfFile = byteArrayOutputStream.toByteArray();
			byteArrayOutputStream.close();
		} catch (DocumentException | IOException exception) {
			LOGGER.error(LOG_REG_DOC_SCAN_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, exception.getMessage());
		}
		return scannedPdfFile;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.device.scanner.impl.DocumentScannerService#
	 * getSingleImageFromList(java.util.List)
	 */
	@Override
	public byte[] asImage(List<BufferedImage> bufferedImages) throws IOException {
		byte[] newSingleImage = null;
		if (isListNotEmpty(bufferedImages)) {

			if (bufferedImages.size() == 1) {
				return getImageBytesFromBufferedImage(bufferedImages.get(0));
			}
			int offset = 2;
			int width = offset;
			for (BufferedImage bufferedImage : bufferedImages) {
				width += bufferedImage.getWidth();
			}
			int height = Math.max(bufferedImages.get(0).getHeight(), bufferedImages.get(1).getHeight()) + offset;
			BufferedImage singleBufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = singleBufferedImage.createGraphics();
			Color oldColor = g2.getColor();
			g2.setPaint(Color.BLACK);
			g2.fillRect(0, 0, width, height);
			g2.setColor(oldColor);
			for (int i = 0; i < bufferedImages.size(); i++) {
				g2.drawImage(bufferedImages.get(i), null, (i * bufferedImages.get(i).getWidth()) + offset, 0);
			}

			g2.dispose();

			newSingleImage = getImageBytesFromBufferedImage(singleBufferedImage);

		}

		return newSingleImage;

	}

	@Override
	public List<BufferedImage> pdfToImages(byte[] pdfBytes) throws IOException {

		List<BufferedImage> bufferedImages = new ArrayList<>();
		PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes));
		@SuppressWarnings("unchecked")
		List<PDPage> list = document.getDocumentCatalog().getAllPages();

		for (PDPage page : list) {
			BufferedImage image = page.convertToImage();
			bufferedImages.add(image);
		}
		document.close();
		return bufferedImages;
	}

	/**
	 * converts bytes to BufferedImage
	 * 
	 * @param imageBytes
	 *            - scanned image file in bytes
	 * @return BufferedImage - image file in bufferedimage format
	 * @throws IOException
	 *             - holds the ioexception
	 */
	protected BufferedImage getBufferedImageFromBytes(byte[] imageBytes) throws IOException {

		return ImageIO.read(new ByteArrayInputStream(imageBytes));
	}

	protected boolean isListNotEmpty(List<?> values) {
		return values != null && !values.isEmpty();
	}
}
