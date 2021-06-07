#!/bin/sh

# This script produces blockchain configurations which
# include Rell source code

set -eu

rm -rf rte
mkdir rte

# Deploying dapp
bash ./postchain-node/multigen.sh -d config/$DAPP_NAME -o rte config/$DAPP_NAME/manifest.xml

