#!/bin/sh
# Copyright (c) 2017 ChromaWay Inc. See README for license information.

set -eu

ls

pwd

# Seting env-s
export NODE=${ENV_NODE:-node1}

if [ $NODE = "node1" ]
then
  export NODE_HOST=127.0.0.1
  export NODE_PORT=9871
  export NODE_PUBKEY=0350fe40766bc0ce8d08b3f5b810e49a8352fdd458606bd5fafe5acdcdc8ff3f57
fi

if [ $NODE = "node2" ]
then
  export NODE_HOST=127.0.0.1
  export NODE_PORT=9872
  export NODE_PUBKEY=02B99A05912B01B7797D84D6660E9ED35FAEE078BD5BDF40026E0CC6E0CB2EF50C
fi

if [ $NODE = "node3" ]
then
  export NODE_HOST=127.0.0.1
  export NODE_PORT=9873
  export NODE_PUBKEY=02839DDE1D2121CE72794E54180F5F5C3AD23543D419CB4C3640A854ACB1ADA9E6
fi


# Deploying chain-zero dapp
sh ./deploy.sh

# Launching a node
sh ./run.sh WIPE_DB
