#!/bin/bash

# This script produces blockchain configurations which
# include Rell source code

set -eu

rm -rf rte
mkdir rte

bash ./postchain-node/multigen.sh -d config/chain_zero -o rte config/chain_zero/manifest.xml
