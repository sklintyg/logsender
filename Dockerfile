FROM openjdk:8-jre-alpine

EXPOSE 9099

ADD ./build/libs/*.jar app.jar
ADD healthchecker-linux-amd64 /

HEALTHCHECK --interval=10s --timeout=30s CMD ["./healthchecker-linux-amd64", "-port=9099"] || exit 1

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Dcredentials.file=/opt/inera/logsender-konfiguration/credentials.properties", "-Dbasedir=/opt/inera/logsender-konfiguration", "-jar", "-Xmx64M", "/app.jar"]
