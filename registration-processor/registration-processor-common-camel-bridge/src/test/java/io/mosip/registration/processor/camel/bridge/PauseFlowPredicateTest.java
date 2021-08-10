
package io.mosip.registration.processor.camel.bridge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Consumer;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.camel.bridge.intercepter.PauseFlowPredicate;
import io.mosip.registration.processor.camel.bridge.model.Setting;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.WorkflowInternalActionDTO;
import io.mosip.registration.processor.core.code.WorkflowInternalActionCode;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(VertxUnitRunner.class)
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*" })

@AutoConfigureMockMvc
public class PauseFlowPredicateTest {

	private PauseFlowPredicate pauseFlowPredicate;

	private ObjectMapper objectMapper;

	private Exchange exchange;

	private static final Logger LOGGER = RegProcessorLogger.getLogger(PauseFlowPredicateTest.class);

	@Before
	public void setup() {
		objectMapper = new ObjectMapper();
		pauseFlowPredicate = new PauseFlowPredicate();

		Setting[] settings = null;
		try {
			settings = objectMapper.readValue(
					"[{\"ruleId\" :\"HOTLISTED_OPERATOR\",\"matchExpression\": \"$.tags[?(@['HOTLISTED'] == 'operator')]\",\"pauseFor\": 432000,\"defaultResumeAction\": \"STOP_PROCESSING\",\"fromAddress\": \"bio-debup-bus-out\",\"ruleDescription\": \"HotListed paused\"},{\"ruleId\" :\"NON_RESIDENT_CHILD_APPLICANT\",\"matchExpression\": \"$.tags[?(@['AGE_GROUP']== 'CHILD' && @['ID_OBJECT-residenceStatus'] == 'nonResident')]\",\"pauseFor\": 400,\"defaultResumeAction\": \"RESUME_PROCESSING\",\"fromAddress\": \"bio-debup-bus-out\",\"ruleDescription\": \"Non resident child applicant paused\"}]",
					Setting[].class);
		} catch (IOException e) {
			LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
					"RoutePredicate::exception " + e.getMessage());
		}
		ReflectionTestUtils.setField(pauseFlowPredicate, "settings", settings);
		ReflectionTestUtils.setField(pauseFlowPredicate, "objectMapper", objectMapper);
		
		Endpoint endpoint = new DefaultEndpoint() {

			@Override
			public String toString() {

				return "bio-debup-bus-out";
			}

			@Override
			public boolean isSingleton() {

				return false;
			}

			@Override
			public Producer createProducer() throws Exception {

				return null;
			}

			@Override
			public Consumer createConsumer(Processor processor) throws Exception {

				return null;
			}
		};

		exchange = new DefaultExchange(endpoint);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testRoutePredicatePositive() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10002100741000120201231071308");
		Map<String, String> tags = new HashMap<>();
		tags.put("HOTLISTED", "operator");
		tags.put("AGE_GROUP", "CHILD");
		tags.put("ID_OBJECT-residenceStatus", "nonResident");
		messageDTO.setTags(tags);
		exchange.getMessage().setBody(objectMapper.writeValueAsString(messageDTO));
		LocalDateTime dateTimeBefore = DateUtils.getUTCCurrentDateTime().plusSeconds(432000);
		assertTrue(pauseFlowPredicate.matches(exchange));
		LocalDateTime dateTimeAfter = DateUtils.getUTCCurrentDateTime().plusSeconds(432000);
		JsonObject json = new JsonObject((String) exchange.getMessage().getBody());
		assertEquals("STOP_PROCESSING", json.getString("defaultResumeAction"));
		assertTrue(dateTimeBefore.isBefore(DateUtils.parseToLocalDateTime(json.getString("resumeTimestamp"))) && dateTimeAfter.isAfter(DateUtils.parseToLocalDateTime(json.getString("resumeTimestamp"))));
		WorkflowInternalActionDTO workflowInternalActionDTO = objectMapper
				.readValue(exchange.getMessage().getBody().toString(), WorkflowInternalActionDTO.class);
		assertEquals(WorkflowInternalActionCode.MARK_AS_PAUSED.toString(),
				workflowInternalActionDTO.getActionCode());
		assertEquals(2,
				workflowInternalActionDTO.getMatchedRuleIds().size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testRoutePredicateHotListedNotFound() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10002100741000120201231071308");
		Map<String, String> tags = new HashMap<>();
		messageDTO.setTags(tags);
		exchange.getMessage().setBody(objectMapper.writeValueAsString(messageDTO));
		assertFalse(pauseFlowPredicate.matches(exchange));

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testRoutePredicateFromAddressMismatch() throws Exception {
		Endpoint wrongEndpoint = new DefaultEndpoint() {
			@Override
			public String toString() {

				return "wrongendpointtest";
			}

			@Override
			public boolean isSingleton() {

				return false;
			}

			@Override
			public Producer createProducer() throws Exception {

				return null;
			}

			@Override
			public Consumer createConsumer(Processor processor) throws Exception {

				return null;
			}
		};
		exchange.setFromEndpoint(wrongEndpoint);
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10002100741000120201231071308");
		Map<String, String> tags = new HashMap<>();
		tags.put("HOTLISTED", "operator");
		messageDTO.setTags(tags);
		exchange.getMessage().setBody(objectMapper.writeValueAsString(messageDTO));
		assertFalse(pauseFlowPredicate.matches(exchange));

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testRoutePredicateHotListedReasonMismatch() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10002100741000120201231071308");
		Map<String, String> tags = new HashMap<>();
		tags.put("HOTLISTED", "hostlistedwrongtest");
		messageDTO.setTags(tags);
		exchange.getMessage().setBody(objectMapper.writeValueAsString(messageDTO));
		assertFalse(pauseFlowPredicate.matches(exchange));

	}
	@SuppressWarnings("unchecked")
	@Test
	public void testRoutePredicatePositiveWithRuleIdsIgnore() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("10002100741000120201231071308");
		Map<String, String> tags = new HashMap<>();
		tags.put("HOTLISTED", "operator");
		tags.put("AGE_GROUP", "CHILD");
		tags.put("ID_OBJECT-residenceStatus", "nonResident");
		tags.put("PAUSE_IMMUNITY_RULE_IDS", "NON_RESIDENT_CHILD_APPLICANT");
		messageDTO.setTags(tags);
		exchange.getMessage().setBody(objectMapper.writeValueAsString(messageDTO));
		LocalDateTime dateTimeBefore = DateUtils.getUTCCurrentDateTime().plusSeconds(432000);
		assertTrue(pauseFlowPredicate.matches(exchange));
		LocalDateTime dateTimeAfter = DateUtils.getUTCCurrentDateTime().plusSeconds(432000);
		JsonObject json = new JsonObject((String) exchange.getMessage().getBody());
		assertEquals("STOP_PROCESSING", json.getString("defaultResumeAction"));
		assertTrue(dateTimeBefore.isBefore(DateUtils.parseToLocalDateTime(json.getString("resumeTimestamp"))) && dateTimeAfter.isAfter(DateUtils.parseToLocalDateTime(json.getString("resumeTimestamp"))));
		WorkflowInternalActionDTO workflowInternalActionDTO = objectMapper
				.readValue(exchange.getMessage().getBody().toString(), WorkflowInternalActionDTO.class);
		assertEquals(WorkflowInternalActionCode.MARK_AS_PAUSED.toString(),
				workflowInternalActionDTO.getActionCode());
		assertEquals(1,
				workflowInternalActionDTO.getMatchedRuleIds().size());
	}
}
