# LogSender

LogSender is a Spring Boot application that aggregates and sends log messages to the PDL (Patient
Data Log) service.

## Running Locally

For running LogSender locally, refer to the common documentation
in [devops/develop README](https://github.com/sklintyg/devops/tree/release/2021-1/develop/README.md)

## Log Entry Aggregation

The default setting for aggregating log messages from producers is 5 entries. For example, when
testing from local Webcert (https://wc.localtest.me):

1. Log in
2. Navigate to patient 191212121212
3. Create a draft certificate
4. Open and close the draft 4 additional times via the "Unsigned drafts" page

Each view of the draft creates a log entry. After 5 entries total, LogSender will compile a log
message from all aggregated entries and send it to the PDL service.

## Checking the Stub

When running locally, the service is stubbed by activating spring profile 'testability' (active by
default when running gradlew appRun). You can view the stub contents at:

```
http://localhost:8010/api/loggtjanst-api
```

## Stub Testability API

The following operations can be performed using GET requests to the stub:

**Deactivate stub:**

```
http://localhost:8010/api/loggtjanst-api/offline
```

**Reactivate stub:**

```
http://localhost:8010/api/loggtjanst-api/online
```

**Simulate errors** (errorType = NONE, ERROR, or VALIDATION):

```
http://localhost:8010/api/loggtjanst-api/error/{errorType}
```

**Simulate latency** (latencyMs = artificial delay in milliseconds):

```
http://localhost:8010/api/loggtjanst-api/latency/{latencyMs}
```

## Viewing Queues in Development

See the ActiveMQ section
in [devops/develop README](https://github.com/sklintyg/devops/tree/release/2021-1/develop/README.md)

## License

Copyright (C) 2021 Inera AB (http://www.inera.se)

LogSender is free software: you can redistribute it and/or modify it under the terms of the GNU
Affero General Public License as published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

LogSender is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero
General Public License for more details.

See also [LICENSE.md](LICENSE.md). 
