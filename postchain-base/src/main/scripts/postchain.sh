#!/bin/bash

scriptdir=`dirname $0`

java -cp $scriptdir/${project.artifactId}-${project.version}-${executable-classifier}.jar:$APPCP ${main-class} $@
