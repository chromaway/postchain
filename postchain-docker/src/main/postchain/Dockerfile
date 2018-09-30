# Postchain
FROM openjdk:8-jdk

ADD ./postchain-base/target/postchain-base-2.3.5-SNAPSHOT-jar-with-dependencies.jar /opt/chromaway/postchain-2.3.5-SNAPSHOT.jar
ADD ./configuration_78967baa.xml /opt/chromaway/configuration_78967baa.xml
ADD ./entrypoint.sh /opt/chromaway/entrypoint.sh
# ADD ./wait-for-it.sh /opt/chromaway/wait-for-it.sh

ENTRYPOINT ["bash", "/opt/chromaway/entrypoint.sh"]
