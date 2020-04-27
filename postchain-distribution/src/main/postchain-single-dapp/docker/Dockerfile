# Copyright (c) 2019 ChromaWay Inc. See README for license information.
# Postchain Node Docker file
FROM openjdk:8-jre-alpine

# Install bash, curl
RUN apk update && apk add bash curl

ADD ./postchain-single-dapp-3.2.0-SNAPSHOT-dist.tar.gz /opt/chromaway/
ADD ./entrypoint.sh /opt/chromaway/postchain-single-dapp/entrypoint.sh

EXPOSE 7741

WORKDIR /opt/chromaway/postchain-single-dapp
ENTRYPOINT ["sh", "/opt/chromaway/postchain-single-dapp/entrypoint.sh"]

