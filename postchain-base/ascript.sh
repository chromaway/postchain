#!/usr/bin/env bash

I=$1
CONF=$PWD/src/main/resources/config/config.${I}.properties

java -cp target/postchain-base-2.4.3-SNAPSHOT-jar-with-dependencies.jar \
net.postchain.AppKt add-blockchain \
-nc ${CONF} \
-brid 78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a3 \
-cid 1 \
-bc $PWD/src/main/resources/blockchain-config/configuration1.xml

java -cp target/postchain-base-2.4.3-SNAPSHOT-jar-with-dependencies.jar \
net.postchain.AppKt run-node \
-nc ${CONF} \
-cid 1 \
-i ${I}
