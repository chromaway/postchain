# Copyright (c) 2017 ChromaWay Inc. See README for license information.
# Postchain Node Docker file
FROM openjdk:8-jre-alpine

RUN apk update && apk add bash

ADD ./postchain-base/target/postchain-base-2.3.5-SNAPSHOT-jar-with-dependencies.jar /opt/chromaway/postchain-2.3.5-SNAPSHOT.jar
ADD ./configuration_78967baa.xml /opt/chromaway/configuration_78967baa.xml
ADD ./entrypoint.sh /opt/chromaway/entrypoint.sh
# ADD ./wait-for-it.sh /opt/chromaway/wait-for-it.sh

ENTRYPOINT ["bash", "/opt/chromaway/entrypoint.sh"]
