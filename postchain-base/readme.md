 In order to deploy an image to the docker repository execute next steps:
 1. Substitute those params in pom.xml: 
  configuration.to.image
  configuration.to.auth.username
  configuration.to.auth.password
 2. Execute maven goals: mvn compile jib:build