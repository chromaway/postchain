# Copyright (c) 2019 ChromaWay Inc. See README for license information.
# Postchain Node Docker file
FROM openjdk:8-jre-alpine

# Install bash
RUN apk update && apk add bash curl

ADD ./postchain-mme-testtools-3.2.0-SNAPSHOT-dist.tar.gz /opt/chromaway/

WORKDIR /opt/chromaway/postchain-mme-testtools
CMD tail -f /dev/null
#ENTRYPOINT ["sh"]

