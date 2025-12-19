/*
 * Copyright (C) 2024 Inera AB (http://www.inera.se)
 *
 * This file is part of sklintyg (https://github.com/sklintyg).
 *
 * sklintyg is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * sklintyg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.inera.intyg.logsender.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import se.inera.intyg.logsender.model.ActivityType;
import se.inera.intyg.logsender.model.Enhet;
import se.inera.intyg.logsender.helper.TestDataHelper;
import se.inera.intyg.logsender.helper.ValueInclude;

class LogTypeFactoryImplTest {

  private final LogTypeFactoryImpl logTypeFactory = new LogTypeFactoryImpl();

  @Test
  void testConvertOk() {
    final var pdlLogMessage = TestDataHelper.buildBasePdlLogMessage(ActivityType.READ);
    pdlLogMessage.setActivityArgs("");

    final var logType = logTypeFactory.convert(pdlLogMessage);

    assertEquals(logType.getActivity().getActivityType(),
        pdlLogMessage.getActivityType().getType());
    assertNull(logType.getActivity().getActivityArgs());
    assertEquals(logType.getLogId(), pdlLogMessage.getLogId());
    assertEquals(logType.getSystem().getSystemId(), pdlLogMessage.getSystemId());
    assertEquals(logType.getSystem().getSystemName(), pdlLogMessage.getSystemName());

    assertEquals(logType.getUser().getUserId(), pdlLogMessage.getUserId());
    assertEquals(logType.getUser().getName(), pdlLogMessage.getUserName());
    assertEquals(logType.getUser().getCareUnit().getCareUnitId(),
        pdlLogMessage.getUserCareUnit().getEnhetsId());
    assertEquals(logType.getUser().getCareUnit().getCareUnitName(),
        pdlLogMessage.getUserCareUnit().getEnhetsNamn());
    assertEquals(logType.getUser().getCareProvider().getCareProviderId(),
        pdlLogMessage.getUserCareUnit().getVardgivareId());
    assertEquals(logType.getUser().getCareProvider().getCareProviderName(),
        pdlLogMessage.getUserCareUnit().getVardgivareNamn());

    assertEquals(1, logType.getResources().getResource().size());
    final var resourceType = logType.getResources().getResource().getFirst();

    assertEquals(resourceType.getPatient().getPatientId().getExtension(),
        pdlLogMessage.getPdlResourceList().getFirst().getPatient().getPatientId());
    assertEquals(resourceType.getPatient().getPatientName(),
        pdlLogMessage.getPdlResourceList().getFirst().getPatient().getPatientNamn());
    assertEquals(resourceType.getResourceType(),
        pdlLogMessage.getPdlResourceList().getFirst().getResourceType());

    assertEquals(resourceType.getCareUnit().getCareUnitId(),
        pdlLogMessage.getPdlResourceList().getFirst().getResourceOwner().getEnhetsId());
    assertEquals(resourceType.getCareUnit().getCareUnitName(),
        pdlLogMessage.getPdlResourceList().getFirst().getResourceOwner().getEnhetsNamn());

    assertEquals(resourceType.getCareProvider().getCareProviderId(),
        pdlLogMessage.getPdlResourceList().getFirst().getResourceOwner().getVardgivareId());
    assertEquals(resourceType.getCareProvider().getCareProviderName(),
        pdlLogMessage.getPdlResourceList().getFirst().getResourceOwner().getVardgivareNamn());
  }

  @Test
  void testConvertWithActivityArgs() {
    final var pdlLogMessage = TestDataHelper.buildBasePdlLogMessage(ActivityType.READ);
    pdlLogMessage.setActivityArgs("activityArgs");

    final var logType = logTypeFactory.convert(pdlLogMessage);
    assertEquals(logType.getActivity().getActivityType(),
        pdlLogMessage.getActivityType().getType());
    assertEquals(logType.getActivity().getActivityArgs(), pdlLogMessage.getActivityArgs());
    assertEquals(logType.getLogId(), pdlLogMessage.getLogId());
    assertEquals(logType.getSystem().getSystemId(), pdlLogMessage.getSystemId());
    assertEquals(logType.getSystem().getSystemName(), pdlLogMessage.getSystemName());
    assertEquals(logType.getUser().getUserId(), pdlLogMessage.getUserId());
    assertEquals(logType.getUser().getName(), pdlLogMessage.getUserName());
    assertEquals(logType.getUser().getCareUnit().getCareUnitId(),
        pdlLogMessage.getUserCareUnit().getEnhetsId());
    assertEquals(logType.getUser().getCareUnit().getCareUnitName(),
        pdlLogMessage.getUserCareUnit().getEnhetsNamn());
    assertEquals(logType.getUser().getCareProvider().getCareProviderId(),
        pdlLogMessage.getUserCareUnit().getVardgivareId());
    assertEquals(logType.getUser().getCareProvider().getCareProviderName(),
        pdlLogMessage.getUserCareUnit().getVardgivareNamn());
    assertEquals(1, logType.getResources().getResource().size());

    final var resourceType = logType.getResources().getResource().getFirst();
    assertEquals(resourceType.getPatient().getPatientId().getExtension(),
        pdlLogMessage.getPdlResourceList().getFirst().getPatient().getPatientId());
    assertEquals(resourceType.getPatient().getPatientName(),
        pdlLogMessage.getPdlResourceList().getFirst().getPatient().getPatientNamn());
    assertEquals(resourceType.getResourceType(),
        pdlLogMessage.getPdlResourceList().getFirst().getResourceType());
    assertEquals(resourceType.getCareUnit().getCareUnitId(),
        pdlLogMessage.getPdlResourceList().getFirst().getResourceOwner().getEnhetsId());
    assertEquals(resourceType.getCareUnit().getCareUnitName(),
        pdlLogMessage.getPdlResourceList().getFirst().getResourceOwner().getEnhetsNamn());
    assertEquals(resourceType.getCareProvider().getCareProviderId(),
        pdlLogMessage.getPdlResourceList().getFirst().getResourceOwner().getVardgivareId());
    assertEquals(resourceType.getCareProvider().getCareProviderName(),
        pdlLogMessage.getPdlResourceList().getFirst().getResourceOwner().getVardgivareNamn());
  }

  @Test
  void testLeadingAndTrailingWhitespacesAreTrimmed() {
    final var enhet = new Enhet(" enhet-1", " enhets namn ", "vardgivare-1 ", "Vardgivare namn ");
    final var pdlLogMessage = TestDataHelper.buildBasePdlLogMessage(ActivityType.READ);
    pdlLogMessage.setUserCareUnit(enhet);

    final var logType = logTypeFactory.convert(pdlLogMessage);
    assertEquals("enhet-1", logType.getUser().getCareUnit().getCareUnitId());
    assertEquals("enhets namn", logType.getUser().getCareUnit().getCareUnitName());
    assertEquals("vardgivare-1", logType.getUser().getCareProvider().getCareProviderId());
    assertEquals("Vardgivare namn", logType.getUser().getCareProvider().getCareProviderName());
  }

  @Test
  void testBlankPatientNameIsConvertedToNull() {
    final var pdlLogMessage = TestDataHelper.buildBasePdlLogMessage(ActivityType.READ,
        ValueInclude.BLANK_WITH_SPACE, ValueInclude.INCLUDE);
    pdlLogMessage.setActivityArgs("activityArgs");

    final var logType = logTypeFactory.convert(pdlLogMessage);
    final var resourceType = logType.getResources().getResource().getFirst();
    assertEquals(resourceType.getPatient().getPatientId().getExtension(),
        pdlLogMessage.getPdlResourceList().getFirst().getPatient().getPatientId());
    assertNull(resourceType.getPatient().getPatientName());
  }

  @Test
  void testBlankUserNameIsConvertedToNull() {
    final var pdlLogMessage = TestDataHelper.buildBasePdlLogMessage(ActivityType.READ,
        ValueInclude.INCLUDE, ValueInclude.BLANK_WITH_SPACE);
    pdlLogMessage.setActivityArgs("activityArgs");

    final var logType = logTypeFactory.convert(pdlLogMessage);
    assertEquals(logType.getUser().getUserId(), pdlLogMessage.getUserId());
    assertNull(logType.getUser().getName());
  }

}
