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
package se.inera.intyg.logsender.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PdlLogMessage implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private final String logId;
  private String systemId;
  private String systemName;

  private String activityLevel;
  private String activityArgs;
  private ActivityType activityType;
  private ActivityPurpose purpose;

  private LocalDateTime timestamp;

  private String userId;
  private String userName;
  private String userTitle;
  private String userAssignment;
  private Enhet userCareUnit;

  private List<PdlResource> pdlResourceList;

  public PdlLogMessage() {
    this.logId = UUID.randomUUID().toString();
  }

  public PdlLogMessage(String logId) {
    this.logId = logId;
  }

  public PdlLogMessage(ActivityType activityType) {
    this(activityType, ActivityPurpose.CARE_TREATMENT);
  }

  public PdlLogMessage(ActivityType activityType, ActivityPurpose activityPurpose) {
    this(activityType, activityPurpose, new ArrayList<>());
  }

  public PdlLogMessage(
      ActivityType activityType, ActivityPurpose purpose, List<PdlResource> pdlResourceList) {
    this.logId = UUID.randomUUID().toString();
    this.activityType = activityType;
    this.purpose = purpose;
    this.pdlResourceList = pdlResourceList;
    this.timestamp = LocalDateTime.now();
  }

  public List<PdlResource> getPdlResourceList() {
    if (pdlResourceList == null) {
      pdlResourceList = new ArrayList<>();
    }
    return pdlResourceList;
  }

  public PdlLogMessage copy(boolean includeResourceList) {
    PdlLogMessage msg = new PdlLogMessage(this.activityType, this.purpose);
    msg.setActivityArgs(this.activityArgs);
    msg.setActivityLevel(this.activityLevel);
    msg.setSystemId(this.systemId);
    msg.setSystemName(this.systemName);
    msg.setTimestamp(this.timestamp);
    msg.setUserCareUnit(this.userCareUnit);
    msg.setUserId(this.userId);
    msg.setUserName(this.userName);
    msg.setUserAssignment(this.userAssignment);
    msg.setUserTitle(this.userTitle);

    if (includeResourceList) {
      msg.setPdlResourceList(pdlResourceList);
    }

    return msg;
  }
}
