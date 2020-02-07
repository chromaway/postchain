#!/bin/bash

RANDOM=$$
CMD="postchain-node/postchain-client.sh -c zero-chain.properties post-tx nop"

$CMD $RANDOM &
$CMD $RANDOM &
$CMD $RANDOM &
$CMD $RANDOM &
$CMD $RANDOM &

