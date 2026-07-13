package io.mosip.registration.processor.stages.createdraft;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.mosip.registration.processor.stages.createdraft.stage.CreateDraftStage;

/**
 * Spring Boot launcher for the Create Draft stage.
 */
@SpringBootApplication
public class CreateDraftApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(CreateDraftApplication.class);
        app.run(args);
        CreateDraftStage stage = new CreateDraftStage();
        stage.deployVerticle();
    }
}
