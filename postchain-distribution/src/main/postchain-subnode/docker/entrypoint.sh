#!/bin/sh
# Copyright (c) 2017 ChromaWay Inc. See README for license information.

set -eu

# Seting env-s
export NODE_HOST=127.0.0.1
export NODE_PORT=9871
export NODE_PUBKEY=0350fe40766bc0ce8d08b3f5b810e49a8352fdd458606bd5fafe5acdcdc8ff3f57
export DAPP_NAME=dapp

# just sh
#sh

# Deploying chain-zero dapp
if [ $BUILD_RELL_DAPP = "true" ]
then
	sh ./deploy.sh
fi

# Launching a node
sh ./run.sh
