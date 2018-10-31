#!/bin/bash

export ACTIVEMQ_BROKER_USERNAME=${ACTIVEMQ_BROKER_USERNAME:-admin}
export ACTIVEMQ_BROKER_PASSWORD=${ACTIVEMQ_BROKER_PASSWORD:-admin}

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-test,wc-all-stubs,wc-security-test,testability-api,caching-enabled}"

export CATALINA_OPTS_APPEND="\
-Dlogsender.config.file=/opt/$APP_NAME/config/logsender.properties \
-Dconfig.dir=/opt/$APP_NAME/config \
-Dlogback.file=classpath:logback-ocp.xml \
-Dcertificate.folder=/opt/$APP_NAME/certifikat \
-Djava.awt.headless=true \
-Dcredentials.file=/opt/$APP_NAME/env/secret-env.properties \
-Dresources.folder=/tmp/resources \
-Dfile.encoding=UTF-8 \
-DbaseUrl=http://${APP_NAME}:8080"
