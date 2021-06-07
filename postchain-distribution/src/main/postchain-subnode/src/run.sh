#!/bin/sh

set -eu

# Wiping db if required
# [ $1 = "WIPE_DB" ] || [ $WIPE_DB = "true" ]

ALREADY_INITED="ALREADY_INITED"

if [ ! -e $ALREADY_INITED ]
then
	echo "Deleting the database..."
	postchain-node/postchain.sh wipe-db -nc rte/node-config.properties

  echo "Adding my peer-info..."
  postchain-node/postchain.sh peerinfo-add -nc rte/node-config.properties -h $NODE_HOST -p $NODE_PORT -pk $NODE_PUBKEY

  touch $ALREADY_INITED

fi

# Launching a node
exec postchain-node/postchain.sh run-node-auto -d rte

