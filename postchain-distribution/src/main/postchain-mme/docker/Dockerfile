# Copyright (c) 2019 ChromaWay Inc. See README for license information.
# Postchain Node Docker file
FROM openjdk:8-jre-alpine

# Install bash
RUN apk update && apk add bash

ADD ./postchain-mme-3.2.0-SNAPSHOT-dist.tar.gz /opt/chromaway/
ADD ./entrypoint.sh /opt/chromaway/postchain-mme/entrypoint.sh

WORKDIR /opt/chromaway/postchain-mme
ENTRYPOINT ["sh", "/opt/chromaway/postchain-mme/entrypoint.sh"]

