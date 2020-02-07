#!/bin/bash

RANDOM=$$
CMD="postchain-node/postchain-client.sh -c zero-chain.properties post-tx nop"

# $CMD $RANDOM

curl $POSTCHAIN_CLIENT_API_URL/blocks/6357B76B43F8905A2BC35CE40906ACD8DA80DD129C469D93F723B94964DDA9E2
