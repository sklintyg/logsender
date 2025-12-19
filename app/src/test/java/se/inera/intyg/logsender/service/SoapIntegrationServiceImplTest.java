package se.inera.intyg.logsender.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.riv.informationsecurity.auditing.log.StoreLog.v2.rivtabp21.StoreLogResponderInterface;
import se.riv.informationsecurity.auditing.log.StoreLogResponder.v2.StoreLogResponseType;
import se.riv.informationsecurity.auditing.log.StoreLogResponder.v2.StoreLogType;

@ExtendWith(MockitoExtension.class)
class SoapIntegrationServiceImplTest {

  @Mock
  StoreLogResponderInterface storeLogResponderInterface;

  @InjectMocks
  SoapIntegrationServiceImpl soapIntegrationServiceImpl;

  @Test
  void shouldStoreLog() {
    final var expected = new StoreLogResponseType();
    final var request = new StoreLogType();

    when(storeLogResponderInterface.storeLog("logicalAddress", request))
        .thenReturn(expected);

    assertEquals(expected, soapIntegrationServiceImpl.storeLog("logicalAddress", request));
  }
}
