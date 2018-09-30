#!/bin/sh

postchain=/opt/chromaway/postchain-2.3.5-SNAPSHOT.jar

sleep 10

java -jar $postchain add-blockchain \
	-nc config/config.0.properties \
	-cid 1 \
	-rid 78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a3 \
	-bc ./opt/chromaway/configuration_78967baa.xml

java -jar $postchain add-configuration \
	-nc config/config.0.properties \
	-cid 1 \
	-h 0 \
	-bc ./opt/chromaway/configuration_78967baa.xml

java -jar $postchain run-node -c 1
