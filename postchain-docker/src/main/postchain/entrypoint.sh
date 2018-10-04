#!/bin/sh
# Copyright (c) 2017 ChromaWay Inc. See README for license information.

postchain=/opt/chromaway/postchain-2.3.5-SNAPSHOT.jar
NODE_CONFIG=${ENV_NODE_CONFIG:-config/config.0.properties}
BLOCKCHAIN_CONFIG=${ENV_BLOCKCHAIN_CONFIG:-./opt/chromaway/configuration_78967baa.xml}


# Workaround to start 'postchain' docker container after 'postgres' one get ready
# TODO: Use 'wait-for-it' tool: https://docs.docker.com/compose/startup-order/
sleep 10


# Add Blockchain defined in blockchain config file
java -jar $postchain add-blockchain \
	-nc $NODE_CONFIG \
	-cid 1 \
	-brid 78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a3 \
    -bc $BLOCKCHAIN_CONFIG

# Add configuration of the blockchain
java -jar $postchain add-configuration \
	-nc $NODE_CONFIG \
	-cid 1 \
	-h 0 \
	-bc $BLOCKCHAIN_CONFIG

# Launch Postchain node
java -jar $postchain run-node \
    -nc $NODE_CONFIG \
    -cid 1
