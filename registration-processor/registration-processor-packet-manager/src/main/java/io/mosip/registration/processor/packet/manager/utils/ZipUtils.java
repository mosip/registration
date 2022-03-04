package io.mosip.registration.processor.packet.manager.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;

/**
 * Class to unzip the packets
 * 
 * @author Abhishek Kumar
 * @since 1.0.0
 */
public class ZipUtils {

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(ZipUtils.class);

	private ZipUtils() {
		// DONOT DELETE
	}

	/**
	 * Method to unzip the file in-memeory and search the required file and return
	 * it
	 * 
	 * @param packetStream
	 *            zip file to be unzipped
	 * @param file
	 *            file to search within zip file
	 * @return return the corresponding file as inputStream
	 * @throws IOException
	 *             if any error occored while unzipping the file
	 */
	public static InputStream unzipAndGetFile(InputStream packetStream, String file) throws IOException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"ZipUtils::unzipAndGetFile()::entry");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		boolean flag = false;
		byte[] buffer = new byte[2048];
		try (ZipInputStream zis = new ZipInputStream(packetStream)) {
			ZipEntry ze = zis.getNextEntry();
			while (ze != null) {
				String fileName = ze.getName();
				String fileNameWithOutExt = FilenameUtils.removeExtension(fileName);
				if (FilenameUtils.equals(fileNameWithOutExt, file, true, IOCase.INSENSITIVE)) {
					int len;
					flag = true;
					while ((len = zis.read(buffer)) > 0) {
						out.write(buffer, 0, len);
					}
					break;
				}
				zis.closeEntry();
				ze = zis.getNextEntry();
			}
			zis.closeEntry();
		} finally {
			packetStream.close();
			out.close();
		}
		if (flag) {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", "ZipUtils::unzipAndGetFile()::exit");

			return new ByteArrayInputStream(out.toByteArray());
		}

		return null;
	}

	/**
	 * Get all files from inside zip
	 *
	 * @param packetStream
	 * @return
	 * @throws IOException
	 */
	public static Map<String, InputStream> unzipAndGetFiles(InputStream packetStream) throws IOException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"ZipUtils::unzipAndGetFiles()::entry");
		Map<String, InputStream> outList = new HashMap<>();
		try (ZipInputStream zis = new ZipInputStream(packetStream)) {
			ZipEntry ze = zis.getNextEntry();
			while (ze != null) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				int len;
				byte[] buffer = new byte[2048];
				while ((len = zis.read(buffer)) > 0) {
					out.write(buffer, 0, len);
				}
				outList.put(ze.getName(), new ByteArrayInputStream(out.toByteArray()));
				zis.closeEntry();
				ze = zis.getNextEntry();

				out.close();
			}
		} finally {
			packetStream.close();
		}

		return outList;
	}
}
