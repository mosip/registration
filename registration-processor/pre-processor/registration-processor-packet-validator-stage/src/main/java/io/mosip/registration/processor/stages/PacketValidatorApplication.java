package io.mosip.registration.processor.stages;

import org.springframework.beans.BeansException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;

/**
 * The Class PacketValidatorApplication.
 */
@Configuration
@EnableAutoConfiguration
@SpringBootApplication
@ComponentScan(basePackages = { "${app.componentscan.basepackages}" })
public class PacketValidatorApplication {

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws ClassNotFoundException
	 * @throws BeansException
	 */
	public static void main(String[] args) throws BeansException, ClassNotFoundException {
		ConfigurableApplicationContext context = 
			SpringApplication.run(PacketValidatorApplication.class, args);

		String verticleClassesString = context.getEnvironment().getProperty(
			"mosip.regproc.verticle.deploy.classes");

		String[] verticleClasses = verticleClassesString.split(",");
		for(String verticleClass : verticleClasses) {
			MosipVerticleAPIManager stage = 
				(MosipVerticleAPIManager)context.getBean(Class.forName(verticleClass));
			stage.deployVerticle();
		}
	}
}
