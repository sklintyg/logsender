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
package se.inera.intyg.logsender.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import se.inera.intyg.logsender.logging.MdcLogConstants;
import se.inera.intyg.logsender.logging.PerformanceLogging;
import se.riv.informationsecurity.auditing.log.StoreLog.v2.rivtabp21.StoreLogResponderInterface;
import se.riv.informationsecurity.auditing.log.StoreLogResponder.v2.StoreLogResponseType;
import se.riv.informationsecurity.auditing.log.StoreLogResponder.v2.StoreLogType;

@Service
@RequiredArgsConstructor
public class SoapIntegrationServiceImpl implements SoapIntegrationService {

  private final StoreLogResponderInterface storeLogResponder;

  @Override
  @PerformanceLogging(eventAction = "store-log-message", eventType = MdcLogConstants.EVENT_TYPE_CHANGE)
  public StoreLogResponseType storeLog(String logicalAddress, StoreLogType request) {
      return storeLogResponder.storeLog(logicalAddress, request);
  }
}
