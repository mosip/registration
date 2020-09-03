package io.mosip.registration.test.dao.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import io.mosip.kernel.auditmanager.entity.Audit;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dao.impl.AuditDAOImpl;
import io.mosip.registration.entity.RegistrationAuditDates;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.repositories.RegAuditRepository;

public class AuditDAOTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	@InjectMocks
	private AuditDAOImpl auditDAO;
	@Mock
	private RegAuditRepository auditRepository;
	private static List<Audit> audits;

	@BeforeClass
	public static void initialize() {
		new ArrayList<>();
		audits = new LinkedList<>();
		Audit audit = new Audit();
		audit.setUuid(UUID.randomUUID().toString());
		audit.setCreatedAt(LocalDateTime.now());
		audits.add(audit);
		audit = new Audit();
		audit.setUuid(UUID.randomUUID().toString());
		audit.setCreatedAt(LocalDateTime.now());
		audits.add(audit);
	}

	@Test
	public void testGetAudits() {
		when(auditRepository.findByIdOrderByCreatedAtAsc("1234"))
				.thenReturn(audits);

		Assert.assertThat(auditDAO.getAudits(null, "1234"), is(audits));
	}

	@Test
	public void testGetAuditsByNullAuditLogToTime() {
		when(auditRepository.findByIdOrderByCreatedAtAsc("1234"))
				.thenReturn(audits);

		Assert.assertThat(auditDAO.getAudits(new RegistrationAuditDates() {

			@Override
			public Timestamp getAuditLogToDateTime() {
				return null;
			}

			@Override
			public Timestamp getAuditLogFromDateTime() {
				return null;
			}
		}, "1234"), is(audits));
	}

	@Test
	public void testGetAuditsByAuditLogToTime() {
		when(auditRepository.findByCreatedAtGreaterThanOrderByCreatedAtAsc(Mockito.any(LocalDateTime.class)))
				.thenReturn(audits);

		Assert.assertThat(auditDAO.getAudits(new RegistrationAuditDates() {

			@Override
			public Timestamp getAuditLogToDateTime() {
				return Timestamp.valueOf(LocalDateTime.now().minusDays(1));
			}

			@Override
			public Timestamp getAuditLogFromDateTime() {
				return null;
			}
		}, "1234"), is(audits));
	}

	@Test(expected = RegBaseUncheckedException.class)
	public void testGetAuditsRuntimeException() {
		when(auditRepository.findByIdOrderByCreatedAtAsc("1234"))
				.thenThrow(new RuntimeException("SQL exception"));

		auditDAO.getAudits(null, "1234");
	}

	@Test
	public void deleteAllTest() {
		LocalDateTime fromTime = new Timestamp(System.currentTimeMillis()).toLocalDateTime();
		LocalDateTime toTime = new Timestamp(System.currentTimeMillis()).toLocalDateTime();

		Mockito.doNothing().when(auditRepository).deleteAllInBatchBycreatedAtBetween(fromTime, toTime);
		auditDAO.deleteAll(fromTime, toTime);

	}

}
