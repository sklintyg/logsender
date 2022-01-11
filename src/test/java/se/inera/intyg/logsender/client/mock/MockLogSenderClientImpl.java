/*
 * Copyright (C) 2022 Inera AB (http://www.inera.se)
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
package se.inera.intyg.logsender.client.mock;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.ws.WebServiceException;

import se.inera.intyg.infra.logmessages.ActivityType;
import se.riv.informationsecurity.auditing.log.StoreLog.v2.rivtabp21.StoreLogResponderInterface;
import se.riv.informationsecurity.auditing.log.StoreLogResponder.v2.StoreLogResponseType;
import se.riv.informationsecurity.auditing.log.StoreLogResponder.v2.StoreLogType;
import se.riv.informationsecurity.auditing.log.v2.LogType;
import se.riv.informationsecurity.auditing.log.v2.ResultCodeType;
import se.riv.informationsecurity.auditing.log.v2.ResultType;

/**
 * Created by eriklupander on 2016-02-29.
 */
public class MockLogSenderClientImpl implements StoreLogResponderInterface {

    private AtomicInteger count = new AtomicInteger(0);

    private ConcurrentHashMap<String, AtomicInteger> attemptsPerMessage = new ConcurrentHashMap<>();
    private List<String> store = new CopyOnWriteArrayList<>();

    @Override
    public StoreLogResponseType storeLog(String logicalAddress, StoreLogType storeLogRequestType) {
        count.incrementAndGet();

        StoreLogResponseType resp = new StoreLogResponseType();
        ResultType resultType = new ResultType();
        if (storeLogRequestType.getLog().size() == 0) {
            resultType.setResultCode(ResultCodeType.INFO);
            resultType.setResultText("No log messages to store, doing nothing...");
            resp.setResult(resultType);
            return resp;
        }

        increaseAttemptsPerMessage(storeLogRequestType);

        // Use the ActivityType.EMERGENCY_ACCESS to fake failures that should trigger a resend.
        if (storeLogRequestType.getLog().get(0).getActivity().getActivityType().equals(ActivityType.EMERGENCY_ACCESS.getType())) {
            throw new WebServiceException("This is an expected error since we got the EMERGENCY_ACCESS type");
        }

        // Use mechanism to trigger VALIDATION_ERROR
        for (LogType logType : storeLogRequestType.getLog()) {
            if (logType.getSystem().getSystemId().equals("invalid")) {
                resultType.setResultCode(ResultCodeType.VALIDATION_ERROR);
                resp.setResult(resultType);
                return resp;
            }
        }

        resultType.setResultCode(ResultCodeType.OK);
        resp.setResult(resultType);

        store.add(storeLogRequestType.getLog().get(0).getLogId());

        return resp;
    }

    private void increaseAttemptsPerMessage(StoreLogType storeLogRequestType) {
        String key = storeLogRequestType.getLog().get(0).getLogId();
        if (!attemptsPerMessage.containsKey(key)) {
            attemptsPerMessage.put(key, new AtomicInteger(1));
        } else {
            attemptsPerMessage.get(key).incrementAndGet();
        }
    }

    public int getNumberOfReceivedMessages() {
        return count.get();
    }

    public int getNumberOfSentMessages() {
        return store.size();
    }

    public void reset() {
        count = new AtomicInteger(0);
        attemptsPerMessage = new ConcurrentHashMap<>();
        store = new CopyOnWriteArrayList<>();
    }
}
