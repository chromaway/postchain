#!/bin/bash

set -e

# ./deploy.sh

if [[ $1 == "WIPE_DB" ]]; then
	echo "Deleting the database..."
	postchain-node/postchain.sh wipe-db -nc rte/node-config.properties
 
  echo "Adding my peer-info..."
  postchain-node/postchain.sh peerinfo-add -nc rte/node-config.properties -h $NODE_HOST -p $NODE_PORT -pk $NODE_PUBKEY

fi


exec postchain-node/postchain.sh run-node-auto -d rte

