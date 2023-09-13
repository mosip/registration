<<<<<<<< HEAD:registration-processor/post-processor/registration-processor-credential-requestor-stage/src/main/java/io/mosip/registration/processor/credentialrequestor/CredentialRequestorStageApplication.java
package io.mosip.registration.processor.credentialrequestor;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.mosip.registration.processor.credentialrequestor.stage.CredentialRequestorStage;
========
package io.mosip.registration.processor.eventhandler;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.mosip.registration.processor.eventhandler.stage.EventHandlerStage;
>>>>>>>> df41852ca05 (MOSIP-28121 : renamed print stage to event handler stage):registration-processor/post-processor/registration-processor-event-handler-stage/src/main/java/io/mosip/registration/processor/eventhandler/EventHandlerStageApplication.java

/**
 * @author M1048399
 *
 */
<<<<<<<< HEAD:registration-processor/post-processor/registration-processor-credential-requestor-stage/src/main/java/io/mosip/registration/processor/credentialrequestor/CredentialRequestorStageApplication.java
public class CredentialRequestorStageApplication {
========
public class EventHandlerStageApplication {
>>>>>>>> df41852ca05 (MOSIP-28121 : renamed print stage to event handler stage):registration-processor/post-processor/registration-processor-event-handler-stage/src/main/java/io/mosip/registration/processor/eventhandler/EventHandlerStageApplication.java

	public static void main(String[] args) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.scan("io.mosip.registration.processor.core.config", "io.mosip.registration.processor.print.config",
				"io.mosip.registration.processor.rest.client.config",
				"io.mosip.registration.processor.core.kernel.beans",
				"io.mosip.registration.processor.status.config",
				"io.mosip.registration.processor.packet.storage.config");
		ctx.refresh();

<<<<<<<< HEAD:registration-processor/post-processor/registration-processor-credential-requestor-stage/src/main/java/io/mosip/registration/processor/credentialrequestor/CredentialRequestorStageApplication.java
		CredentialRequestorStage printStage = ctx.getBean(CredentialRequestorStage.class);
========
		EventHandlerStage printStage = ctx.getBean(EventHandlerStage.class);
>>>>>>>> df41852ca05 (MOSIP-28121 : renamed print stage to event handler stage):registration-processor/post-processor/registration-processor-event-handler-stage/src/main/java/io/mosip/registration/processor/eventhandler/EventHandlerStageApplication.java
		printStage.deployVerticle();

	}
}
