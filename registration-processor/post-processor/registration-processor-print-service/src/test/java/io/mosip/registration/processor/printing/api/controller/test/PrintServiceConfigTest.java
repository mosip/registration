package io.mosip.registration.processor.printing.api.controller.test;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import io.mosip.kernel.core.idvalidator.spi.RidValidator;
import io.mosip.kernel.core.idvalidator.spi.UinValidator;
import io.mosip.kernel.core.idvalidator.spi.VidValidator;
import io.mosip.kernel.fsadapter.hdfs.util.ConnectionUtils;
import io.mosip.registration.processor.packet.storage.utils.Utilities;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {
		"io.mosip.registration.processor.printing.api.*" })
public class PrintServiceConfigTest {

	@MockBean
	public ConnectionUtils connectionUtil;

	@MockBean
	public RidValidator<?> ridValidator;

	@MockBean
	public UinValidator<?> uinValidator;

	@MockBean
	public VidValidator<?> vidValidatorImpl;

	@MockBean
	private Utilities utilities;
}
