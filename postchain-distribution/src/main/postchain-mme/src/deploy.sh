#!/bin/sh

# This script produces blockchain configurations which
# include Rell source code

set -eu

rm -rf rte
mkdir rte

# Node2 and node3 connect to the network established by node1.
# Therefore they use the same build of chain_zero dapp (compiled for node1)

# Deploying chain_zero dapp for node1 (and for node2 and node3 too)
bash ./postchain-node/multigen.sh -d config/node1/chain_zero -o rte config/node1/chain_zero/manifest.xml

# Building node2 configs and copying them to rte (built one line above)
if [ $NODE = "node2" ]
then
  bash ./postchain-node/multigen.sh -d config/node2/chain_zero -o rte2 config/node2/chain_zero/manifest.xml
  cp rte2/*.properties rte
  rm -rf rte2
fi

# Building node3 configs and copying them to rte (built one line above)
if [ $NODE = "node3" ]
then
  bash ./postchain-node/multigen.sh -d config/node3/chain_zero -o rte3 config/node3/chain_zero/manifest.xml
  cp rte3/*.properties rte
  rm -rf rte3
fi


