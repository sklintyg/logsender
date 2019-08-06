/*
 * Copyright (C) 2019 Inera AB (http://www.inera.se)
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

import org.springframework.stereotype.Service;
import se.inera.intyg.infra.logmessages.Enhet;
import se.inera.intyg.infra.logmessages.Patient;
import se.inera.intyg.infra.logmessages.PdlLogMessage;
import se.inera.intyg.infra.logmessages.PdlResource;
import se.inera.intyg.schemas.contract.Personnummer;
import se.riv.informationsecurity.auditing.log.v2.ActivityType;
import se.riv.informationsecurity.auditing.log.v2.CareProviderType;
import se.riv.informationsecurity.auditing.log.v2.CareUnitType;
import se.riv.informationsecurity.auditing.log.v2.IIType;
import se.riv.informationsecurity.auditing.log.v2.LogType;
import se.riv.informationsecurity.auditing.log.v2.PatientType;
import se.riv.informationsecurity.auditing.log.v2.ResourceType;
import se.riv.informationsecurity.auditing.log.v2.ResourcesType;
import se.riv.informationsecurity.auditing.log.v2.SystemType;
import se.riv.informationsecurity.auditing.log.v2.UserType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Encapsulates PdlLogMessage (internal format) -> LogType conversion.
 *
 * Created by eriklupander on 2016-02-29.
 */
@Service
public class LogTypeFactoryImpl implements LogTypeFactory {

    private static LogTypeFactoryUtil util = LogTypeFactoryUtil.getInstance();

    @Override
    public LogType convert(PdlLogMessage source) {
        LogType logType = new LogType();
        logType.setLogId(source.getLogId());

        buildSystemType(source, logType);
        buildActivityType(source, logType);
        buildUserType(source, logType);

        logType.setResources(new ResourcesType());

        List<ResourceType> resources = source.getPdlResourceList()
                .stream()
                .map(this::buildResource)
                .collect(Collectors.toList());
        logType.getResources().getResource().addAll(resources);

        return logType;
    }

    private void buildUserType(PdlLogMessage source, LogType logType) {
        UserType user = new UserType();
        user.setUserId(util.trim(source.getUserId()));
        user.setCareProvider(careProvider(source.getUserCareUnit()));
        user.setCareUnit(careUnit(source.getUserCareUnit()));

        // optional according to XML schema
        user.setName(util.trimToNull(source.getUserName()));
        user.setAssignment(util.trimToNull(source.getUserAssignment()));
        user.setTitle(util.trimToNull(source.getUserTitle()));

        logType.setUser(user);
    }

    private void buildSystemType(PdlLogMessage source, LogType logType) {
        SystemType system = new SystemType();
        system.setSystemId(util.trim(source.getSystemId()));

        // optional according to XML schema
        system.setSystemName(util.trimToNull(source.getSystemName()));

        logType.setSystem(system);
    }

    private void buildActivityType(PdlLogMessage source, LogType logType) {
        ActivityType activity = new ActivityType();
        activity.setActivityType(source.getActivityType().getType());
        activity.setStartDate(source.getTimestamp());
        activity.setPurpose(source.getPurpose().getType());

        // optional according to XML schema
        activity.setActivityLevel(util.trimToNull(source.getActivityLevel()));
        activity.setActivityArgs(util.trimToNull(source.getActivityArgs()));

        logType.setActivity(activity);
    }

    private PatientType patient(Patient source) {
        String id = util.trim(source.getPatientId());

        final Personnummer personnummer = Personnummer.createPersonnummer(id)
            .orElseThrow(() -> new IllegalArgumentException("PatientId must be a valid personnummer or samordningsnummer"));

        IIType patientId = new IIType();
        patientId.setRoot(util.isSamordningsNummer(personnummer) ? util.getSamordningsNummerRoot() : util.getPersonnummerRoot());
        patientId.setExtension(util.trim(source.getPatientId()));

        PatientType patient = new PatientType();
        patient.setPatientId(patientId);

        // optional according to XML schema
        patient.setPatientName(util.trimToNull(source.getPatientNamn()));

        return patient;
    }

    private CareUnitType careUnit(Enhet source) {
        CareUnitType careUnit = new CareUnitType();
        careUnit.setCareUnitId(util.trim(source.getEnhetsId()));

        // optional according to XML schema
        careUnit.setCareUnitName(util.trimToNull(source.getEnhetsNamn()));

        return careUnit;
    }

    private CareProviderType careProvider(Enhet source) {
        CareProviderType careProvider = new CareProviderType();
        careProvider.setCareProviderId(util.trim(source.getVardgivareId()));

        // optional according to XML schema
        careProvider.setCareProviderName(util.trimToNull(source.getVardgivareNamn()));

        return careProvider;
    }

    private ResourceType buildResource(PdlResource source) {
        ResourceType resource = new ResourceType();
        resource.setResourceType(source.getResourceType());
        resource.setCareProvider(careProvider(source.getResourceOwner()));
        resource.setCareUnit(careUnit(source.getResourceOwner()));

        // optional according to XML schema
        resource.setPatient(patient(source.getPatient()));

        return resource;
    }

}
