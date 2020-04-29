#!/bin/bash

set -eu

scriptdir=`dirname ${BASH_SOURCE[0]}`

${RELL_JAVA:-java} -Dlog4j.configurationFile=./postchain-node/lib/log4j2.yml -cp "$scriptdir/lib/*" net.postchain.AppKt $@

