# Copyright (c) 2019 ChromaWay Inc. See README for license information.
# Postchain Node Docker file
FROM openjdk:8-jre-alpine

# Install bash, curl
RUN apk update && apk add bash curl

ADD ./postchain-subnode-3.2.1-SNAPSHOT-dist.tar.gz /opt/chromaway/
ADD ./entrypoint.sh /opt/chromaway/postchain-subnode/entrypoint.sh

EXPOSE 7741

ENV BUILD_RELL_DAPP false

WORKDIR /opt/chromaway/postchain-subnode
ENTRYPOINT ["sh", "/opt/chromaway/postchain-subnode/entrypoint.sh"]

