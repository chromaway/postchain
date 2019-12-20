#!/bin/bash

set -e

# ./deploy.sh

if [[ $1 == "WIPE_DB" ]]; then 
	echo "Deleting the database..."
	postchain-node/postchain.sh wipe-db -nc rte/node-config.properties
 
  echo "Adding my peer-info..."
  postchain-node/postchain.sh peerinfo-add -nc rte/node-config.properties -h 127.0.0.1 -p 9871 -pk 0350fe40766bc0ce8d08b3f5b810e49a8352fdd458606bd5fafe5acdcdc8ff3f57

fi


exec postchain-node/postchain.sh run-node-auto -d rte

