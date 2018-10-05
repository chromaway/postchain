#!/bin/sh
# Copyright (c) 2017 ChromaWay Inc. See README for license information.

POSTCHAIN=/opt/chromaway/postchain-2.3.5-SNAPSHOT.jar
NODE_CONFIG=${ENV_NODE_CONFIG:-config/config.0.properties}
BLOCKCHAIN_CONFIG=${ENV_BLOCKCHAIN_CONFIG:-./opt/chromaway/configuration_78967baa.xml}
BLOCKCHAIN_RID=${ENV_BLOCKCHAIN_RID:-78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a3}
CHAIN_ID=${ENV_CHAIN_ID:-1}

# Workaround to start 'postchain' docker container after 'postgres' one get ready
# TODO: Use 'wait-for-it' tool: https://docs.docker.com/compose/startup-order/
sleep 10


# Add Blockchain defined in blockchain config file
java -jar $POSTCHAIN add-blockchain \
	-nc $NODE_CONFIG \
	-brid $BLOCKCHAIN_RID \
	-cid $CHAIN_ID \
    -bc $BLOCKCHAIN_CONFIG

# Launch Postchain node
java -jar $POSTCHAIN run-node \
    -nc $NODE_CONFIG \
    -cid $CHAIN_ID
