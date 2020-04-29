#!/bin/bash

set -eu

scriptdir=`dirname ${BASH_SOURCE[0]}`
DEFAULT_LOG4J_CONF="$scriptdir/log4j2.yml"
LOG4J_PARAM="-Dlog4j.configurationFile=${POSTCHAIN_LOG4J:-$DEFAULT_LOG4J_CONF}"
${RELL_JAVA:-java} --illegal-access=permit $LOG4J_PARAM -cp "$scriptdir/lib/*" net.postchain.AppKt $@
