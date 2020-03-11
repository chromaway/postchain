#!/bin/sh

# This script produces blockchain configurations which
# include Rell source code

set -eu

#########################################
#   ChainZero
#########################################

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

#########################################
#   Chain City
#########################################

# We'll deploy the city dapp via test code.
# So we don't need to compile the city dapp in a container

# Building city dapp for node1 only (b/c other nodes will receive city dapp's config by network)
#if [ $NODE = "node1" ]
#then
#  rm -rf rte-city
#  mkdir rte-city
#  bash ./postchain-node/multigen.sh -d config/node1/city -o rte-city config/node1/city/manifest.xml
#fi
