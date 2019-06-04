## Nya LogSender (Camel)

För att köra nya Camel-baserade logsender lokalt gäller följande:

### Starta logsender

Observera att vi kör mot tcp://localhost:61616 som standard, det är ingen poäng att starta logsender lokalt med vm://...

Det innebär att följande saker behöver vara uppfyllda:

(1) Starta en riktig ActiveMQ lokalt, t.ex:


    ./$AMQ_HOME/bin/activemq start

(2) Starta logsender:

    ./gradlew appRun

### Starta logsender i debugläge

För att starta applikationen i debugläge används:

    ./gradlew appRunDebug

Applikationen kommer då att starta upp med debugPort = **5009**. Det är denna port du ska använda när du sätter upp din 
debug-konfiguration i din utvecklingsmiljö.
    
### Konfigurera Webcert    

(1) Öppna /webcert/web/webcert-dev.properties och lägg till 

     activemq.broker.url=tcp://localhost:61616\
     ?jms.nonBlockingRedelivery=true\
     &jms.redeliveryPolicy.maximumRedeliveries=3\
     &jms.redeliveryPolicy.maximumRedeliveryDelay=6000\
     &jms.redeliveryPolicy.initialRedeliveryDelay=4000\
     &jms.redeliveryPolicy.useExponentialBackOff=true\
     &jms.redeliveryPolicy.backOffMultiplier=2
     activemq.broker.username=<username>
     activemq.broker.password=<password>
      
(2) Bygg om och starta webcert:

    ./gradlew build appRun

### Aggregering av loggposter
Kom ihåg att standardinställningen för aggregering av loggmeddelanden från producenter är 5 st, dvs. om man vill testa från lokal Webcert (localhost:9088) så kan man förslagsvis logga in, gå in på 191212121212, skapa ett Utkast och sedan klicka sig in och ut på Utkastet ytterligare 4 gånger via sidan för Ej signerade utkast. Varje "titt" på utkastet skapar en loggpost och efter totalt 5 st så kommer logsender sammanställa ett loggmeddelande utifrån samtliga aggregerade och skicka till PDL-tjänsten.

### Kontrollera stubbe

Lokalt är förstås tjänsten stubbad, man bör kunna kika på innehållet i stubben på:

    http://localhost:9099/logsender/loggtjanst-stub
    
### Stubbens Testbarhets-API 
    
Följande operationer kan utföras mha GET-anrop till stubben
    
Avaktivera stubbe

    http://localhost:9099/logsender/loggtjanst-stub/offline
    
Återaktivera stubbe

    http://localhost:9099/logsender/loggtjanst-stub/online
    
Fejka fel (errorType = någon av NONE,ERROR,VALIDATION)

    http://localhost:9099/logsender/loggtjanst-stub/error/{errorType}
    
Fejka latency, (latencyMs = artificiell fördröjning i millisekunder)

     http://localhost:9099/logsender/loggtjanst-stub/latency/{latencyMs}

### Se utgående SOAP-meddelanden
Ibland vill man se exakt vilken XML som skickas till loggtjänsten i form av StoreLogRequests. För att slå på loggning av dessa,
öppna logback.xml och kommentera in:

    <!-- Uncomment to get SOAP logging
       <logger name="org.apache.cxf" level="INFO" />
    -->

### Titta på köer i OpenShift demo
Den JBoss AMQ-paketering vi använder i OpenShift demomiljö har inget GUI för att titta på köer. Det går dock att gå in i activemq-amq-tcp poddens terminal och köra följande:

    > cd /opt/amq/bin
    > ./activemq-admin browse --user admin --password admin --amqurl tcp://localhost:61616 demo.logging.queue

För olika köer ändra _demo.logging.queue_ i slutet.
