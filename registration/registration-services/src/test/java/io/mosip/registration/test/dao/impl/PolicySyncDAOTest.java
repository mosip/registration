package io.mosip.registration.test.dao.impl;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.dao.impl.PolicySyncDAOImpl;
import io.mosip.registration.entity.KeyStore;
import io.mosip.registration.repositories.PolicySyncRepository;

public class PolicySyncDAOTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	@InjectMocks
	private PolicySyncDAOImpl policySyncDAOImpl;
	@Mock
	private PolicySyncRepository policySyncRepository;

	@Test
	public void findByMaxExpireTime() {
		KeyStore keyStore = new KeyStore();
		keyStore.setCreatedBy("createdBy");
		Mockito.when(policySyncRepository.findFirst1ByOrderByValidTillDtimesDesc()).thenReturn(keyStore);
		policySyncDAOImpl.findByMaxExpireTime();

	}

	@Test
	public void updatePolicy() {
		KeyStore keyStore = new KeyStore();
		keyStore.setCreatedBy("createdBy");
		Mockito.when(policySyncRepository.save(Mockito.any(KeyStore.class))).thenReturn(keyStore);
		policySyncDAOImpl.updatePolicy(keyStore);

	}
	
	@Test
	public void getPublicKey() {
		List<KeyStore> keyStoreList = new ArrayList<>();
		KeyStore keyStore = new KeyStore();
		keyStore.setCreatedBy("createdBy");
		String from = "2019-01-01T13:00:00.325234Z";
		String to = "2020-04-01T13:00:00.325234Z";
	    DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSS'Z'");
	    LocalDateTime frmTime = LocalDateTime.parse(from, dateFormat);
	    LocalDateTime toTime = LocalDateTime.parse(to, dateFormat);
		keyStore.setValidFromDtimes(Timestamp.valueOf(frmTime));
		keyStore.setValidTillDtimes(Timestamp.valueOf(toTime));
		keyStoreList.add(keyStore);
		Mockito.when(policySyncRepository.findByRefIdOrderByValidTillDtimesDesc(Mockito.anyString())).thenReturn(keyStoreList);
		policySyncDAOImpl.getPublicKey(Mockito.anyString());

	}
	
	@Test
	public void getPublicKeyEmpty() {
		List<KeyStore> keyStoreList = new ArrayList<>();
		Mockito.when(policySyncRepository.findByRefIdOrderByValidTillDtimesDesc(Mockito.anyString())).thenReturn(keyStoreList);
		policySyncDAOImpl.getPublicKey(Mockito.anyString());

	}
	@Test
	public void getAllKeyStore() {
		List<KeyStore> keyStoreList = new ArrayList<>();
		KeyStore keyStore = new KeyStore();
		keyStore.setCreatedBy("createdBy");
		keyStore.setValidFromDtimes(Timestamp.valueOf(LocalDateTime.now()));
		keyStore.setValidTillDtimes(Timestamp.valueOf(LocalDateTime.now()));
		keyStoreList.add(keyStore);
		Mockito.when(policySyncRepository.findByRefIdOrderByValidTillDtimesDesc(Mockito.anyString())).thenReturn(keyStoreList);
		policySyncDAOImpl.getAllKeyStore(Mockito.anyString());

	}
	
	@Test
	public void getPublicKeyDate() {
		List<KeyStore> keyStoreList = new ArrayList<>();
		KeyStore keyStore = new KeyStore();
		keyStore.setCreatedBy("createdBy");
		String from = "2019-01-01T13:00:00.325234Z";
		String to = "2020-02-01T13:00:00.325234Z";
	    DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSS'Z'");
	    LocalDateTime frmTime = LocalDateTime.parse(from, dateFormat);
	    LocalDateTime toTime = LocalDateTime.parse(to, dateFormat);
		keyStore.setValidFromDtimes(Timestamp.valueOf(frmTime));
		keyStore.setValidTillDtimes(Timestamp.valueOf(toTime));
		keyStoreList.add(keyStore);
		Mockito.when(policySyncRepository.findByRefIdOrderByValidTillDtimesDesc(Mockito.anyString())).thenReturn(keyStoreList);
		policySyncDAOImpl.getPublicKey(Mockito.anyString());

	}
	
}
