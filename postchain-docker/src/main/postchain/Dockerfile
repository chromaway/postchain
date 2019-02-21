# Copyright (c) 2017 ChromaWay Inc. See README for license information.
# Postchain Node Docker file
FROM openjdk:8-jre-alpine

ADD ./postchain-base-2.4.3-SNAPSHOT-jar-with-dependencies.jar /opt/chromaway/postchain-2.4.3-SNAPSHOT-jwd.jar
ADD ./entrypoint.sh /opt/chromaway/entrypoint.sh

WORKDIR /postchain-node
ENTRYPOINT ["sh", "/opt/chromaway/entrypoint.sh"]
