
package io.mosip.registration.processor.packet.manager.systemexception.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;

import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.spi.filesystem.manager.FileManager;
import io.mosip.registration.processor.packet.manager.dto.DirectoryPathDto;
import io.mosip.registration.processor.packet.manager.exception.systemexception.UnexpectedException;
import io.mosip.registration.processor.packet.manager.service.impl.FileManagerImpl;

/**
 * @author M1022006
 *
 */
@RefreshScope
@RunWith(SpringRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class UnexceptedExceptionTest {

	@Mock
	private Environment env;

	@InjectMocks
	private FileManager<DirectoryPathDto, InputStream> fileManager = new FileManagerImpl();

	private InputStream file;

	@Test
	public void TestUnexceptedException() throws IOException {
		String fileName = "sample";
		UnexpectedException ex = new UnexpectedException(
				PlatformErrorMessages.RPR_SYS_UNEXCEPTED_EXCEPTION.getMessage());
		Mockito.when(env.getProperty(any())).thenThrow(ex);

		try {
			fileManager.put(fileName, file, DirectoryPathDto.ARCHIVE_LOCATION);
			fail();
		} catch (UnexpectedException e) {
			assertThat("Should throw  Unexpected Exception  with correct error codes",
					e.getErrorCode().equalsIgnoreCase(PlatformErrorMessages.RPR_SYS_UNEXCEPTED_EXCEPTION.getCode()));
			assertThat("Should throw  Unexpected Exception  with correct messages",
					e.getErrorText().equalsIgnoreCase(PlatformErrorMessages.RPR_SYS_UNEXCEPTED_EXCEPTION.getMessage()));
		}

	}

}
