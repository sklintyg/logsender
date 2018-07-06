#!/bin/bash

export ACTIVEMQ_BROKER_USERNAME=${ACTIVEMQ_BROKER_USERNAME:-admin}
export ACTIVEMQ_BROKER_PASSWORD=${ACTIVEMQ_BROKER_PASSWORD:-admin}

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"

export CATALINA_OPTS_APPEND="\
-Dconfig.file=/opt/$APP_NAME/config/logsender.properties \
-Dlogback.file=classpath:logback-ocp.xml \
-Dcertificate.folder=/opt/$APP_NAME/env \
-Djava.awt.headless=true \
-Dcredentials.file=/opt/$APP_NAME/env/secret-env.properties \
-Dstatistics.resources.folder=/tmp/resources \
-Dfile.encoding=UTF-8 \
-DbaseUrl=http://${APP_NAME}:8080"
