package io.mosip.registration.processor.core.abstractverticle;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link MosipVerticleAPIManager#getWorkerPoolSize()} behaviour:
 * default from worker.pool.size vs per-stage override from {getPropertyPrefix()}worker.pool.size.
 */
@RunWith(MockitoJUnitRunner.class)
public class MosipVerticleAPIManagerWorkerPoolSizeTest {

	private static final String STAGE_PREFIX = "mosip.regproc.test.stage.";
	private static final String OVERRIDE_KEY = STAGE_PREFIX + "worker.pool.size";

	private TestMosipVerticleAPIManager manager;

	private static class TestMosipVerticleAPIManager extends MosipVerticleAPIManager {
		@Override
		protected String getPropertyPrefix() {
			return STAGE_PREFIX;
		}

		@Override
		public MessageDTO process(MessageDTO object) {
			return object;
		}
	}

	@Before
	public void setUp() {
		manager = new TestMosipVerticleAPIManager();
		ReflectionTestUtils.setField(manager, "environment", new StandardEnvironment());
		ReflectionTestUtils.setField(manager, "defaultWorkerPoolSize", 10);
	}

	@Test
	public void getWorkerPoolSize_whenNoOverride_returnsDefault() {
		assertEquals(10, manager.getWorkerPoolSize());
	}

	@Test
	public void getWorkerPoolSize_whenOverrideSet_returnsOverride() {
		ReflectionTestUtils.setField(manager, "environment", new StandardEnvironment() {
			@Override
			public String getProperty(String key) {
				return OVERRIDE_KEY.equals(key) ? "20" : super.getProperty(key);
			}
		});
		assertEquals(20, manager.getWorkerPoolSize());
	}

	@Test
	public void getWorkerPoolSize_whenOverrideEmpty_returnsDefault() {
		ReflectionTestUtils.setField(manager, "environment", new StandardEnvironment() {
			@Override
			public String getProperty(String key) {
				return OVERRIDE_KEY.equals(key) ? "" : super.getProperty(key);
			}
		});
		assertEquals(10, manager.getWorkerPoolSize());
	}

	@Test
	public void getWorkerPoolSize_whenOverrideInvalid_returnsDefault() {
		ReflectionTestUtils.setField(manager, "environment", new StandardEnvironment() {
			@Override
			public String getProperty(String key) {
				return OVERRIDE_KEY.equals(key) ? "not-a-number" : super.getProperty(key);
			}
		});
		assertEquals(10, manager.getWorkerPoolSize());
	}

	@Test
	public void getWorkerPoolSize_whenDefaultNullAndNoOverride_returnsOne() {
		ReflectionTestUtils.setField(manager, "defaultWorkerPoolSize", null);
		assertEquals(1, manager.getWorkerPoolSize());
	}
}
