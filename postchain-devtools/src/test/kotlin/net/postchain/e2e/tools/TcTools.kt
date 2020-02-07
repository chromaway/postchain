package net.postchain.e2e.tools

import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.GenericContainer
import java.io.File

class KGenericContainer(dockerImageName: String)
    : GenericContainer<KGenericContainer>(dockerImageName)

class KDockerComposeContainer(vararg composeFiles: File)
    : DockerComposeContainer<KDockerComposeContainer>(*composeFiles)

