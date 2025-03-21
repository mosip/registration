package io.mosip.registration.processor.packet.storage.config;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;

import io.mosip.kernel.core.idvalidator.spi.VidValidator;
import io.mosip.kernel.idvalidator.vid.impl.VidValidatorImpl;
import io.mosip.registration.processor.packet.storage.helper.PacketManagerHelper;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import io.mosip.kernel.core.crypto.spi.CryptoCoreSpec;
import io.mosip.kernel.crypto.jce.core.CryptoCore;
import io.mosip.kernel.dataaccess.hibernate.config.HibernateDaoConfig;
import io.mosip.kernel.dataaccess.hibernate.repository.impl.HibernateRepositoryImpl;
import io.mosip.kernel.keygenerator.bouncycastle.KeyGenerator;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.manager.idreposervice.impl.IdRepoServiceImpl;
import io.mosip.registration.processor.packet.storage.dao.PacketInfoDao;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.service.impl.PacketInfoManagerImpl;
import io.mosip.registration.processor.packet.storage.utils.ABISHandlerUtil;
import io.mosip.registration.processor.packet.storage.utils.BioSdkUtil;
import io.mosip.registration.processor.packet.storage.utils.IdSchemaUtil;
import io.mosip.registration.processor.packet.storage.utils.Utilities;

@Configuration
@PropertySource("classpath:bootstrap.properties")
@EnableConfigurationProperties
@Import({ HibernateDaoConfig.class })
@EnableJpaRepositories(basePackages = "io.mosip.registration.processor", repositoryBaseClass = HibernateRepositoryImpl.class)
public class PacketStorageBeanConfig {

	@Bean
	@ConfigurationProperties(prefix = "provider.packetreader")
	public Map<String, String> readerConfiguration() {
		return new HashMap<>();
	}

	@Bean
	@ConfigurationProperties(prefix = "packetmanager.provider")
	public Map<String, String> providerConfiguration() {
		return new HashMap<>();
	}

	@Bean
	@ConfigurationProperties(prefix = "provider.packetwriter")
	public Map<String, String> writerConfiguration() {
		return new HashMap<>();
	}

	@PostConstruct
	public void initialize() {
		Utilities.initialize(readerConfiguration(), writerConfiguration());
		PriorityBasedPacketManagerService.initialize(providerConfiguration());
	}

	@Bean
	public PacketInfoManager<Identity, ApplicantInfoDto> getPacketInfoManager() {
		return new PacketInfoManagerImpl();
	}

	@Bean
	public PacketInfoDao getPacketInfoDao() {
		return new PacketInfoDao();
	}

	@Bean
	public Utilities getUtilities() {
		return new Utilities();
	}

	@Bean
	public PacketManagerService packetManagerService() {
		return new PacketManagerService();
	}

	@Bean
	public ABISHandlerUtil getABISHandlerUtil() {
		return new ABISHandlerUtil();
	}

	
	@Bean
	public BioSdkUtil getBioSdkUtil() {
		return new BioSdkUtil();
	}
	

	@Bean
	public KeyGenerator getKeyGenerator() {
		return new KeyGenerator();
	}

	@Bean
	@Primary
	public CryptoCoreSpec<byte[], byte[], SecretKey, PublicKey, PrivateKey, String> getEncryptor() {
		return new CryptoCore();
	}

	@Bean
	public IdRepoService getIdRepoService() {
		return new IdRepoServiceImpl();
	}

	@Bean
	public PacketManagerHelper packetManagerHelper() {
		return new PacketManagerHelper();
	}

	@Bean
	public IdSchemaUtil getIdSchemaUtil() {
		return new IdSchemaUtil();
	}

	@Bean
	public VidValidator<String> vidValidator(){return new VidValidatorImpl();}
}
