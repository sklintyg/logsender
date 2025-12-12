/*
 * Copyright (C) 2025 Inera AB (http://www.inera.se)
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
package se.inera.intyg.logsender.integrationtest.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import se.inera.intyg.logsender.model.ActivityPurpose;
import se.inera.intyg.logsender.model.ActivityType;
import se.inera.intyg.logsender.model.Enhet;
import se.inera.intyg.logsender.model.Patient;
import se.inera.intyg.logsender.model.PdlLogMessage;
import se.inera.intyg.logsender.model.PdlResource;
import se.inera.intyg.logsender.model.ResourceType;

/**
 * Builder for creating test PDL log messages with fluent API.
 * <p>
 * Usage:
 * <pre>
 * String json = TestMessageBuilder.create()
 *     .withActivityType(ActivityType.READ)
 *     .withResources(3)
 *     .withPatientId("191212121212")
 *     .buildJson();
 * </pre>
 */
public class TestMessageBuilder {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .registerModule(new JavaTimeModule());

  private ActivityType activityType = ActivityType.READ;
  private int numberOfResources = 1;
  private String userId = "user-123";
  private String userName = "Test User";
  private String systemId = "webcert";
  private String patientId = "191212121212";
  private String patientName = "Test Patient";
  private String enhetId = "enhet-1";
  private String enhetName = "Enhet nr 1";
  private String vardgivareId = "vardgivare-1";
  private String vardgivareName = "Vårdgivare 1";
  private ActivityPurpose purpose = ActivityPurpose.CARE_TREATMENT;
  private LocalDateTime timestamp = LocalDateTime.now();
  private ResourceType resourceType = ResourceType.RESOURCE_TYPE_INTYG;

  private TestMessageBuilder() {
  }

  /**
   * Create a new builder with default values.
   */
  public static TestMessageBuilder create() {
    return new TestMessageBuilder();
  }

  /**
   * Set the activity type (READ, CREATE, UPDATE, DELETE, etc.).
   */
  public TestMessageBuilder withActivityType(ActivityType activityType) {
    this.activityType = activityType;
    return this;
  }

  /**
   * Set the number of resources in the message. Each resource will result in a separate split
   * message.
   */
  public TestMessageBuilder withResources(int numberOfResources) {
    this.numberOfResources = numberOfResources;
    return this;
  }

  /**
   * Set the user ID (läkare/vårdpersonal).
   */
  public TestMessageBuilder withUserId(String userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Set the user name.
   */
  public TestMessageBuilder withUserName(String userName) {
    this.userName = userName;
    return this;
  }

  /**
   * Set the system ID (webcert, intygstjanst, etc.).
   */
  public TestMessageBuilder withSystemId(String systemId) {
    this.systemId = systemId;
    return this;
  }

  /**
   * Set the patient ID (personnummer).
   */
  public TestMessageBuilder withPatientId(String patientId) {
    this.patientId = patientId;
    return this;
  }

  /**
   * Set the patient name.
   */
  public TestMessageBuilder withPatientName(String patientName) {
    this.patientName = patientName;
    return this;
  }

  /**
   * Set the enhet ID.
   */
  public TestMessageBuilder withEnhetId(String enhetId) {
    this.enhetId = enhetId;
    return this;
  }

  /**
   * Set the purpose (CARE_TREATMENT, STATISTICS, etc.).
   */
  public TestMessageBuilder withPurpose(ActivityPurpose purpose) {
    this.purpose = purpose;
    return this;
  }

  /**
   * Set the timestamp.
   */
  public TestMessageBuilder withTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  /**
   * Set the resource type.
   */
  public TestMessageBuilder withResourceType(ResourceType resourceType) {
    this.resourceType = resourceType;
    return this;
  }

  /**
   * Build and return the PdlLogMessage object.
   */
  public PdlLogMessage build() {
    PdlLogMessage message = new PdlLogMessage();
    message.setUserId(userId);
    message.setUserName(userName);
    message.setSystemId(systemId);
    message.setSystemName(systemId);
    message.setUserCareUnit(buildEnhet());
    message.setActivityType(activityType);
    message.setTimestamp(timestamp);
    message.setPurpose(purpose);

    for (int i = 0; i < numberOfResources; i++) {
      PdlResource resource = new PdlResource();
      resource.setPatient(buildPatient());
      resource.setResourceOwner(buildEnhet());
      resource.setResourceType(resourceType.getResourceTypeName());
      message.getPdlResourceList().add(resource);
    }

    return message;
  }

  /**
   * Build and return the message as JSON string.
   */
  public String buildJson() {
    try {
      return OBJECT_MAPPER.writeValueAsString(build());
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize message to JSON", e);
    }
  }

  private Enhet buildEnhet() {
    return new Enhet(enhetId, enhetName, vardgivareId, vardgivareName);
  }

  private Patient buildPatient() {
    Patient patient = new Patient();
    patient.setPatientId(patientId);
    patient.setPatientNamn(patientName);
    return patient;
  }
}

