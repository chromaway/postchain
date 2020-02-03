#!/bin/sh
# Copyright (c) 2017 ChromaWay Inc. See README for license information.

set -eu

ls

pwd

# Deploy chain-zero dapp
sh ./deploy.sh

# Launch a node
sh ./run.sh WIPE_DB
