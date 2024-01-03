
package io.mosip.registration.processor.packet.storage.utils;

import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.constant.AbisConstant;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.abis.AbisResponseDetDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisResponseDto;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.storage.dao.PacketInfoDao;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.mapper.PacketInfoMapper;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.entity.RegistrationStatusEntity;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * The Class PacketInfoManagerImplTest.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Utilities.class, PacketInfoMapper.class })
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*","com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
public class ABISHandlerUtilTest {

    private static final String registrationId = "10002100820001420210108085956";
    private static final String registrationType = "NEW";
    private static final String latestTransactionId = "123-456-789";
    List<String> matchedRids = new ArrayList<>();


    @InjectMocks
    private ABISHandlerUtil abisHandlerUtil;

    @Mock
    private Utilities utilities;

    @Mock
    private PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;

    @Mock
    private PacketInfoDao packetInfoDao;

    @Mock
    private IdRepoService idRepoService;

    List<String> lst=new ArrayList<>();

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(utilities.getLatestTransactionId(any(),any(),anyInt(), any())).thenReturn(latestTransactionId);

        List<String> regBioRefIds = new ArrayList<>();
        regBioRefIds.add("cf1c941a-142c-44f1-9543-4606b4a7884e");

        when(packetInfoDao.getAbisRefIdByWorkflowInstanceId(any())).thenReturn(regBioRefIds);
        when(utilities.getGetRegProcessorDemographicIdentity()).thenReturn(new String());

        List<RegistrationStatusEntity> registrationStatusEntityList = new ArrayList<>();

        RegistrationStatusEntity registrationEntity1 = new RegistrationStatusEntity();
        registrationEntity1.setRegId("10002100820001420210108085103");
        registrationEntity1.setStatusCode(RegistrationStatusCode.PROCESSED.toString());
        registrationStatusEntityList.add(registrationEntity1);
        RegistrationStatusEntity registrationEntity2 = new RegistrationStatusEntity();
        registrationEntity2.setRegId("10002100820001420210108085100");
        registrationEntity2.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
        registrationStatusEntityList.add(registrationEntity2);
        RegistrationStatusEntity registrationEntity3 = new RegistrationStatusEntity();
        registrationEntity3.setRegId("10002100820001420210108085102");
        registrationEntity3.setStatusCode(RegistrationStatusCode.PROCESSED.toString());
        registrationStatusEntityList.add(registrationEntity3);
        matchedRids.add("10002100820001420210108085100");
        matchedRids.add("10002100820001420210108085103");
        matchedRids.add("10002100820001420210108085101");// REJECTED
        matchedRids.add("10002100820001420210108085102");

        List<AbisResponseDto> abisResponseDtoList = new ArrayList<>();
        matchedRids.forEach(matchedRid -> {
            AbisResponseDto abisResponseDto = new AbisResponseDto();
            abisResponseDto.setId(matchedRid);
            abisResponseDtoList.add(abisResponseDto);
        });

        lst.add(RegistrationTransactionStatusCode.PROCESSED.toString());lst.add(RegistrationTransactionStatusCode.PROCESSING.toString());

        when(packetInfoManager.getAbisResponseRecords(regBioRefIds.get(0),
                latestTransactionId, AbisConstant.IDENTIFY)).thenReturn(abisResponseDtoList);

        List<AbisResponseDetDto> abisResponseDetDtoList = new ArrayList<>();
        matchedRids.forEach(matchedRid -> {
            AbisResponseDetDto abisResponseDto = new AbisResponseDetDto();
            abisResponseDto.setMatchedBioRefId(matchedRid);
            abisResponseDetDtoList.add(abisResponseDto);
        });
        for (AbisResponseDetDto dto : abisResponseDetDtoList) {
            AbisResponseDetDto responseDetDto = new AbisResponseDetDto();
            responseDetDto.setMatchedBioRefId(dto.getMatchedBioRefId());
            when(packetInfoManager.getAbisResponseDetails(dto.getMatchedBioRefId())).thenReturn(Lists.newArrayList(responseDetDto));
        }
        when(packetInfoDao.getAbisRefRegIdsByMatchedRefIds(matchedRids)).thenReturn(matchedRids);
        when(packetInfoDao.getWithoutStatusCode(matchedRids, RegistrationStatusCode.REJECTED.toString()))
                .thenReturn(registrationStatusEntityList);
        when(idRepoService.getUinByRid("10002100820001420210108085103", new String())).thenReturn("123456789");
        when(idRepoService.getUinByRid("10002100820001420210108085102", new String())).thenReturn("987654321");
    }

    @Test
    public void testProcesssedWithUniqueUin() throws ApisResourceAccessException, JsonProcessingException, PacketManagerException, IOException, io.mosip.kernel.core.exception.IOException {

        Set<String> uniqueRids = abisHandlerUtil.getUniqueRegIds(registrationId, registrationType, 1, "", ProviderStageName.BIO_DEDUPE);
// expected to pick 2 rids from processedMatchedIds list because different uin.
        // Total should be 1(inprogress) + 2(processed)
        assertEquals(3, uniqueRids.size());
    }

    @Test
    public void testProcesssedWithSameUin() throws ApisResourceAccessException, JsonProcessingException, PacketManagerException, IOException, io.mosip.kernel.core.exception.IOException {

        when(idRepoService.getUinByRid(anyString(), anyString())).thenReturn("987654321");

        Set<String> uniqueRids = abisHandlerUtil.getUniqueRegIds(registrationId, registrationType,1, "", ProviderStageName.BIO_DEDUPE);
        // expected to pick only 1 rid from processedMatchedIds list because same uin. Total should be 1(inprogress) + 1(processed)
        assertEquals(2, uniqueRids.size());
    }

    @Test
    public void testDonotReturnRejected() throws ApisResourceAccessException, JsonProcessingException, PacketManagerException, IOException, io.mosip.kernel.core.exception.IOException {

//        List<String> uniqueRids = abisHandlerUtil.getUniqueRegIds(registrationId, registrationType, ProviderStageName.BIO_DEDUPE);
        Set<String> uniqueRids= abisHandlerUtil.getUniqueRegIds(registrationId,registrationType,1,"",ProviderStageName.BIO_DEDUPE);
        // expected to pick only processingandprocessed list i.e 3 records.
        assertEquals(3, uniqueRids.size());
    }

    @Test
    public void testReturnAllInprogress() throws ApisResourceAccessException, JsonProcessingException, PacketManagerException, IOException, io.mosip.kernel.core.exception.IOException {

        when(idRepoService.getUinByRid(anyString(), anyString())).thenReturn(null);

        Set<String> uniqueRids = abisHandlerUtil.getUniqueRegIds(registrationId, registrationType,1, "", ProviderStageName.BIO_DEDUPE);
        // expected not to pick processedMatchedIds list i.e 1 records..
        assertEquals(1, uniqueRids.size());
    }

}
