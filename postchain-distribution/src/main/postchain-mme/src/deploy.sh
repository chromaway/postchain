#!/bin/bash

# This script produces blockchain configurations which
# include Rell source code

set -eu

NODE=${ENV_NODE:-node1}
POSTCHAIN_DB_URL=${ENV_POSTCHAIN_DB_URL:-jdbc:postgresql://localhost:5432/postchain}

if [ $NODE == "node1" ]
then
  NODE_HOST=127.0.0.1
  NODE_PORT=9871
  NODE_PUBKEY=0350fe40766bc0ce8d08b3f5b810e49a8352fdd458606bd5fafe5acdcdc8ff3f57
fi

if [ $NODE == "node2" ]
then
  NODE_HOST=127.0.0.1
  NODE_PORT=9872
  NODE_PUBKEY=02B99A05912B01B7797D84D6660E9ED35FAEE078BD5BDF40026E0CC6E0CB2EF50C
fi

if [ $NODE == "node3" ]
then
  NODE_HOST=127.0.0.1
  NODE_PORT=9873
  NODE_PUBKEY=02839DDE1D2121CE72794E54180F5F5C3AD23543D419CB4C3640A854ACB1ADA9E6
fi


rm -rf rte
mkdir rte

bash ./postchain-node/multigen.sh -d config/$NODE/chain_zero -o rte config/$NODE/chain_zero/manifest.xml
