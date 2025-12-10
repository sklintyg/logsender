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

//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNull;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
//import org.junit.Test;
//import org.junit.jupiter.api.Test;
import se.inera.intyg.logsender.model.ActivityType;
import se.inera.intyg.logsender.model.Enhet;
import se.inera.intyg.logsender.model.PdlLogMessage;
import se.inera.intyg.logsender.helper.TestDataHelper;
import se.inera.intyg.logsender.helper.ValueInclude;
import se.riv.informationsecurity.auditing.log.v2.LogType;
import se.riv.informationsecurity.auditing.log.v2.ResourceType;

/**
 * Tests that {@link PdlLogMessage} is properly converted into a {@link LogType}.
 *
 * Created by eriklupander on 2016-03-08.
 */
public class LogTypeFactoryImplTest {

    private LogTypeFactoryImpl testee = new LogTypeFactoryImpl();

    @Test
    public void testConvertOk() {
        PdlLogMessage pdlLogMessage = TestDataHelper.buildBasePdlLogMessage(ActivityType.READ);
        pdlLogMessage.setActivityArgs("");

        LogType logType = testee.convert(pdlLogMessage);

        assertEquals(logType.getActivity().getActivityType(), pdlLogMessage.getActivityType().getType());
        assertNull(logType.getActivity().getActivityArgs());
        assertEquals(logType.getLogId(), pdlLogMessage.getLogId());
        assertEquals(logType.getSystem().getSystemId(), pdlLogMessage.getSystemId());
        assertEquals(logType.getSystem().getSystemName(), pdlLogMessage.getSystemName());

        assertEquals(logType.getUser().getUserId(), pdlLogMessage.getUserId());
        assertEquals(logType.getUser().getName(), pdlLogMessage.getUserName());
        assertEquals(logType.getUser().getCareUnit().getCareUnitId(), pdlLogMessage.getUserCareUnit().getEnhetsId());
        assertEquals(logType.getUser().getCareUnit().getCareUnitName(), pdlLogMessage.getUserCareUnit().getEnhetsNamn());
        assertEquals(logType.getUser().getCareProvider().getCareProviderId(), pdlLogMessage.getUserCareUnit().getVardgivareId());
        assertEquals(logType.getUser().getCareProvider().getCareProviderName(), pdlLogMessage.getUserCareUnit().getVardgivareNamn());

        assertEquals(1, logType.getResources().getResource().size());
        ResourceType resourceType = logType.getResources().getResource().get(0);

        assertEquals(resourceType.getPatient().getPatientId().getExtension(),
            pdlLogMessage.getPdlResourceList().get(0).getPatient().getPatientId());
        assertEquals(resourceType.getPatient().getPatientName(), pdlLogMessage.getPdlResourceList().get(0).getPatient().getPatientNamn());
        assertEquals(resourceType.getResourceType(), pdlLogMessage.getPdlResourceList().get(0).getResourceType());

        assertEquals(resourceType.getCareUnit().getCareUnitId(),
            pdlLogMessage.getPdlResourceList().get(0).getResourceOwner().getEnhetsId());
        assertEquals(resourceType.getCareUnit().getCareUnitName(),
            pdlLogMessage.getPdlResourceList().get(0).getResourceOwner().getEnhetsNamn());

        assertEquals(resourceType.getCareProvider().getCareProviderId(),
            pdlLogMessage.getPdlResourceList().get(0).getResourceOwner().getVardgivareId());
        assertEquals(resourceType.getCareProvider().getCareProviderName(),
            pdlLogMessage.getPdlResourceList().get(0).getResourceOwner().getVardgivareNamn());
    }

    @Test
    public void testConvertWithActivityArgs() {
        PdlLogMessage pdlLogMessage = TestDataHelper.buildBasePdlLogMessage(ActivityType.READ);
        pdlLogMessage.setActivityArgs("activityArgs");

        LogType logType = testee.convert(pdlLogMessage);

        assertEquals(logType.getActivity().getActivityType(), pdlLogMessage.getActivityType().getType());
        assertEquals(logType.getActivity().getActivityArgs(), pdlLogMessage.getActivityArgs());
        assertEquals(logType.getLogId(), pdlLogMessage.getLogId());
        assertEquals(logType.getSystem().getSystemId(), pdlLogMessage.getSystemId());
        assertEquals(logType.getSystem().getSystemName(), pdlLogMessage.getSystemName());

        assertEquals(logType.getUser().getUserId(), pdlLogMessage.getUserId());
        assertEquals(logType.getUser().getName(), pdlLogMessage.getUserName());
        assertEquals(logType.getUser().getCareUnit().getCareUnitId(), pdlLogMessage.getUserCareUnit().getEnhetsId());
        assertEquals(logType.getUser().getCareUnit().getCareUnitName(), pdlLogMessage.getUserCareUnit().getEnhetsNamn());
        assertEquals(logType.getUser().getCareProvider().getCareProviderId(), pdlLogMessage.getUserCareUnit().getVardgivareId());
        assertEquals(logType.getUser().getCareProvider().getCareProviderName(), pdlLogMessage.getUserCareUnit().getVardgivareNamn());

        assertEquals(1, logType.getResources().getResource().size());
        ResourceType resourceType = logType.getResources().getResource().get(0);

        assertEquals(resourceType.getPatient().getPatientId().getExtension(),
            pdlLogMessage.getPdlResourceList().get(0).getPatient().getPatientId());
        assertEquals(resourceType.getPatient().getPatientName(), pdlLogMessage.getPdlResourceList().get(0).getPatient().getPatientNamn());
        assertEquals(resourceType.getResourceType(), pdlLogMessage.getPdlResourceList().get(0).getResourceType());

        assertEquals(resourceType.getCareUnit().getCareUnitId(),
            pdlLogMessage.getPdlResourceList().get(0).getResourceOwner().getEnhetsId());
        assertEquals(resourceType.getCareUnit().getCareUnitName(),
            pdlLogMessage.getPdlResourceList().get(0).getResourceOwner().getEnhetsNamn());

        assertEquals(resourceType.getCareProvider().getCareProviderId(),
            pdlLogMessage.getPdlResourceList().get(0).getResourceOwner().getVardgivareId());
        assertEquals(resourceType.getCareProvider().getCareProviderName(),
            pdlLogMessage.getPdlResourceList().get(0).getResourceOwner().getVardgivareNamn());
    }

    @Test
    public void testLeadingAndTrailingWhitespacesAreTrimmed() {

        Enhet enhet = new Enhet(" enhet-1", " enhets namn ", "vardgivare-1 ", "Vardgivare namn ");

        PdlLogMessage pdlLogMessage = TestDataHelper.buildBasePdlLogMessage(ActivityType.READ);
        pdlLogMessage.setUserCareUnit(enhet);

        LogType logType = testee.convert(pdlLogMessage);

        assertEquals("enhet-1", logType.getUser().getCareUnit().getCareUnitId());
        assertEquals("enhets namn", logType.getUser().getCareUnit().getCareUnitName());
        assertEquals("vardgivare-1", logType.getUser().getCareProvider().getCareProviderId());
        assertEquals("Vardgivare namn", logType.getUser().getCareProvider().getCareProviderName());
    }

    @Test
    public void testBlankPatientNameIsConvertedToNull() {
        PdlLogMessage pdlLogMessage = TestDataHelper
            .buildBasePdlLogMessage(ActivityType.READ, ValueInclude.BLANK_WITH_SPACE, ValueInclude.INCLUDE);
        pdlLogMessage.setActivityArgs("activityArgs");

        LogType logType = testee.convert(pdlLogMessage);

        ResourceType resourceType = logType.getResources().getResource().get(0);
        assertEquals(resourceType.getPatient().getPatientId().getExtension(),
            pdlLogMessage.getPdlResourceList().get(0).getPatient().getPatientId());
        assertNull(resourceType.getPatient().getPatientName());
    }

    @Test
    public void testBlankUserNameIsConvertedToNull() {
        PdlLogMessage pdlLogMessage = TestDataHelper
            .buildBasePdlLogMessage(ActivityType.READ, ValueInclude.INCLUDE, ValueInclude.BLANK_WITH_SPACE);
        pdlLogMessage.setActivityArgs("activityArgs");

        LogType logType = testee.convert(pdlLogMessage);

        assertEquals(logType.getUser().getUserId(), pdlLogMessage.getUserId());
        assertNull(logType.getUser().getName());
    }

}
