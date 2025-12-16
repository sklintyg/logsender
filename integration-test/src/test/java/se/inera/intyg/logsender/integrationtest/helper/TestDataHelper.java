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
package se.inera.intyg.logsender.integrationtest.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import se.inera.intyg.logsender.mapper.CustomObjectMapper;
import se.inera.intyg.logsender.model.ActivityPurpose;
import se.inera.intyg.logsender.model.ActivityType;
import se.inera.intyg.logsender.model.Enhet;
import se.inera.intyg.logsender.model.Patient;
import se.inera.intyg.logsender.model.PdlLogMessage;
import se.inera.intyg.logsender.model.PdlResource;
import se.inera.intyg.logsender.model.ResourceType;


public class TestDataHelper {

  public static final ObjectMapper OBJECT_MAPPER = new CustomObjectMapper();


  public static String buildBasePdlLogMessageAsJson(ActivityType activityType) {
    try {
      return OBJECT_MAPPER.writeValueAsString(
          buildBasePdlLogMessage(activityType, 1, ValueInclude.INCLUDE, ValueInclude.INCLUDE));
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(
          "Could not build test data log message, JSON could not be produced: " + e.getMessage());
    }
  }

  public static String buildBasePdlLogMessageAsJson(ActivityType activityType,
      int numberOfResources) {
    try {
      return OBJECT_MAPPER
          .writeValueAsString(
              buildBasePdlLogMessage(activityType, numberOfResources, ValueInclude.INCLUDE,
                  ValueInclude.INCLUDE));
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(
          "Could not build test data log message, JSON could not be produced: " + e.getMessage());
    }
  }

  public static PdlLogMessage buildBasePdlLogMessage(ActivityType activityType,
      int numberOfResources,
      ValueInclude patientNameInclude,
      ValueInclude userNameInclude) {

    PdlLogMessage pdlLogMessage = new PdlLogMessage();
    pdlLogMessage.setUserId("user-123");
    pdlLogMessage.setUserName(getUserName(userNameInclude));
    pdlLogMessage.setSystemId("webcert");
    pdlLogMessage.setSystemName("webcert");
    pdlLogMessage.setUserCareUnit(buildEnhet());
    pdlLogMessage.setActivityType(activityType);
    pdlLogMessage.setTimestamp(LocalDateTime.now());
    pdlLogMessage.setPurpose(ActivityPurpose.CARE_TREATMENT);

    for (int a = 0; a < numberOfResources; a++) {
      PdlResource pdlResource = new PdlResource();
      pdlResource.setPatient(buildPatient(patientNameInclude));
      pdlResource.setResourceOwner(buildEnhet());
      pdlResource.setResourceType(ResourceType.RESOURCE_TYPE_INTYG.getResourceTypeName());
      pdlLogMessage.getPdlResourceList().add(pdlResource);
    }

    return pdlLogMessage;
  }

  private static Enhet buildEnhet() {
    return new Enhet("enhet-1", "Enhet nr 1", "vardgivare-1", "Vårdgivare 1");
  }

  private static Patient buildPatient(ValueInclude patientNameInclude) {
    String patientName = switch (patientNameInclude) {
      case BLANK_WITH_SPACE -> " ";
      case INCLUDE -> "Tolvan Tolvansson";
      default -> null;
    };
    return new Patient("19121212-1212", patientName);
  }

  private static String getUserName(ValueInclude userNameInclude) {
    return switch (userNameInclude) {
      case BLANK_WITH_SPACE -> " ";
      case INCLUDE -> "Stein Ivarsdottir";
      default -> null;
    };
  }

}
