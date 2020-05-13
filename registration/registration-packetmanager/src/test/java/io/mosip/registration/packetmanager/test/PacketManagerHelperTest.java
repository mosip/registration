package io.mosip.registration.packetmanager.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import io.mosip.kernel.core.idobjectvalidator.spi.IdObjectValidator;
import io.mosip.registration.packetmanager.util.PacketManagerHelper;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class, loader = AnnotationConfigContextLoader.class)
public class PacketManagerHelperTest {

	@Autowired
	private PacketManagerHelper packetManagerHelper;
	
	@Autowired
	private IdObjectValidator idObjectValidator;
	
	
	@Test
	public void generateHashTestWithNull() throws Exception {
		byte[] hash = packetManagerHelper.generateHash(null, null);
		assertNull(hash);		
	}
	
	@Test
	public void generateHashTestNoData() {
		byte[] hash = packetManagerHelper.generateHash(new ArrayList<String>(), null);
		assertNull(hash);
	}
	
	@Test(expected = NullPointerException.class)
	public void generateHashTestWithMissingData() {
		List<String> order = new LinkedList<String>();
		order.add("registrationId");
		order.add("Id.json");
		
		Map<String, byte[]> data = new HashMap<>();
		data.put("Id.json", "{\"fullName\":\"test User\",\"age\":10}".getBytes());
		
		packetManagerHelper.generateHash(order, data);		
	}
	
	@Test
	public void generateHashTest() {
		String expected = "F437EF1C93EA5ABF8F4794C2199BEE444B89114E2F66C6236FE8628EB6170FDC";
		List<String> order = new LinkedList<String>();
		order.add("registrationId");
		order.add("Id.json");
		
		Map<String, byte[]> data = new HashMap<>();
		data.put("registrationId", "registrationId".getBytes());
		data.put("Id.json", "{\"fullName\":\"test User\",\"age\":10}".getBytes());
		
		byte[] hash = packetManagerHelper.generateHash(order, data);
		assertEquals(expected, new String(hash));
	}
}
