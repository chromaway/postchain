package net.postchain.e2e.tools

import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.FixedHostPortGenericContainer
import org.testcontainers.containers.GenericContainer
import java.io.File

class KGenericContainer(dockerImageName: String)
    : GenericContainer<KGenericContainer>(dockerImageName)

class KGenericContainerFixedPort(dockerImageName: String)
    : FixedHostPortGenericContainer<KGenericContainerFixedPort>(dockerImageName)

class KDockerComposeContainer(vararg composeFiles: File)
    : DockerComposeContainer<KDockerComposeContainer>(*composeFiles)


fun KGenericContainer.startContainer(): Void = this.dockerClient.startContainerCmd(this.containerId).exec()

fun KGenericContainer.stopContainer(): Void = this.dockerClient.stopContainerCmd(this.containerId).exec()

