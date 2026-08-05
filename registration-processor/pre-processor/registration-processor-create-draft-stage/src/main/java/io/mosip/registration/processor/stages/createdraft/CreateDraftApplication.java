package io.mosip.registration.processor.stages.createdraft;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import io.mosip.registration.processor.stages.createdraft.stage.CreateDraftStage;

/**
 * Spring Boot launcher for the Create Draft stage.
 */
@SpringBootApplication
public class CreateDraftApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(CreateDraftApplication.class, args);
        CreateDraftStage stage = context.getBean(CreateDraftStage.class);
        stage.deployVerticle();
    }
}
