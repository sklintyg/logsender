## Nya log-sender.war (Camel)

För att köra nya Camel-baserade log-sender lokalt gäller följande:

### Starta log-sender

Observera att vi kör mot tcp://localhost:61616 som standard, det är ingen poäng att starta log-sender lokalt med vm://...

Det innebär att följande saker behöver vara uppfyllda:

(1) Starta en riktig ActiveMQ lokalt, t.ex:


    ./$AMQ_HOME/bin/activemq start

(2) Starta log-sender:

    ./gradlew appRun

### Starta log-sender i debugläge

För att starta applikationen i debugläge används:

    ./gradlew appRunDebug

Applikationen kommer då att starta upp med debugPort = **5009**. Det är denna port du ska använda när du sätter upp din 
debug-konfiguration i din utvecklingsmiljö.
    
### Konfigurera Webcert    

(1) Öppna /webcert/web/src/main/webapp/WEB-INF/jetty-web.xml och ändra så jms/AsyncConnectionFactory går över TCP och port 61616:


     <New id="ConnectionFactory" class="org.eclipse.jetty.plus.jndi.Resource">
        <Arg>
          <Ref refid="webAppContext" />
        </Arg>
        <Arg>jms/ConnectionFactory</Arg>
        <Arg>
          <New class="org.apache.activemq.ActiveMQConnectionFactory">           
              <Arg>tcp://localhost:61616?jms.nonBlockingRedelivery=true&amp;jms.redeliveryPolicy.maximumRedeliveries=3&amp;jms.redeliveryPolicy.maximumRedeliveryDelay=6000&amp;jms.redeliveryPolicy.initialRedeliveryDelay=4000&amp;jms.redeliveryPolicy.useExponentialBackOff=true&amp;jms.redeliveryPolicy.backOffMultiplier=2</Arg>-->     
          </New>
        </Arg>
      </New>
      
(2) Bygg om och starta webcert:


    ./gradlew build appRun

### Aggregering av loggposter
Kom ihåg att standardinställningen för aggregering av loggmeddelanden från producenter är 5 st, dvs. om man vill testa från lokal Webcert (localhost:9088) så kan man förslagsvis logga in, gå in på 191212121212, skapa ett Utkast och sedan klicka sig in och ut på Utkastet ytterligare 4 gånger via sidan för Ej signerade utkast. Varje "titt" på utkastet skapar en loggpost och efter totalt 5 st så kommer log-sender sammanställa ett loggmeddelande utifrån samtliga aggregerade och skicka till PDL-tjänsten.

### Kontrollera stubbe

Lokalt är förstås tjänsten stubbad, man bör kunna kika på innehållet i stubben på:

    http://localhost:9099/log-sender/loggtjanst-stub
    
### Stubbens Testbarhets-API 
    
Följande operationer kan utföras mha GET-anrop till stubben
    
Avaktivera stubbe

    http://localhost:9099/log-sender/loggtjanst-stub/offline
    
Återaktivera stubbe

    http://localhost:9099/log-sender/loggtjanst-stub/online
    
Fejka fel (errorType = någon av NONE,ERROR,VALIDATION)

    http://localhost:9099/log-sender/loggtjanst-stub/error/{errorType}
    
Fejka latency, (latencyMs = artificiell fördröjning i millisekunder)

     http://localhost:9099/log-sender/loggtjanst-stub/latency/{latencyMs}

### Se utgående SOAP-meddelanden
Ibland vill man se exakt vilken XML som skickas till loggtjänsten i form av StoreLogRequests. För att slå på loggning av dessa,
öppna log-sender-logback.xml och kommentera in:

    <!-- Uncomment to get SOAP logging
       <logger name="org.apache.cxf" level="INFO" />
    -->

