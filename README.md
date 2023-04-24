# LogSender

För att logsender lokalt gäller följande:

Läs vidare i gemensam dokumentation [devops/develop README-filen](https://github.com/sklintyg/devops/tree/release/2021-1/develop/README.md)

## Aggregering av loggposter
Kom ihåg att standardinställningen för aggregering av loggmeddelanden från producenter är 5 st, dvs. om man vill testa från lokal Webcert (https://wc.localtest.me) så kan man förslagsvis logga in, gå in på 191212121212, skapa ett Utkast och sedan klicka sig in och ut på Utkastet ytterligare 4 gånger via sidan för Ej signerade utkast. Varje "titt" på utkastet skapar en loggpost och efter totalt 5 st så kommer logsender sammanställa ett loggmeddelande utifrån samtliga aggregerade och skicka till PDL-tjänsten.

## Kontrollera stubbe

Lokalt är förstås tjänsten stubbad, man bör kunna kika på innehållet i stubben på:

    http://localhost:8010/logsender/api/loggtjanst-api
    
## Stubbens Testbarhets-API 
    
Följande operationer kan utföras mha GET-anrop till stubben
    
Avaktivera stubbe

    http://localhost:8010/logsender/api/loggtjanst-api/offline
    
Återaktivera stubbe

    http://localhost:8010/logsender/api/loggtjanst-api/online
    
Fejka fel (errorType = någon av NONE,ERROR,VALIDATION)

    http://localhost:8010/logsender/api/loggtjanst-api/error/{errorType}
    
Fejka latency, (latencyMs = artificiell fördröjning i millisekunder)

     http://localhost:8010/logsender/api/loggtjanst-api/latency/{latencyMs}

## Se utgående SOAP-meddelanden
Ibland vill man se exakt vilken XML som skickas till loggtjänsten i form av StoreLogRequests. För att slå på loggning av dessa, öppna logback-dev.xml och kommentera in:

    <!-- Uncomment to get SOAP logging
       <logger name="org.apache.cxf" level="INFO" />
    -->

## Titta på köer i develop
Läs under ActiveMQ i [devops/develop README-filen](https://github.com/sklintyg/devops/tree/release/2021-1/develop/README.md)

## Titta på köer i OpenShift
Logga in i OpenShift och gå till `broker-amq-xxx` podden. Klicka på länken `Open Java Console`.

## Licens
Copyright (C) 2021 Inera AB (http://www.inera.se)

Logsender is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

Logsender is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.

Se även [LICENSE.md](LICENSE.md). 