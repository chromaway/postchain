package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

@Parameters(commandDescription = "Adds configuration")
class CommandAddConfiguration : Command {

    @Parameter(names = ["-a", "--archetype"], description = "Archetype of blockchain. Not currently used.")
    private var archetype = "base"

    @Parameter(names = ["-id"], description = "Id of blockchain", required = true)
    private var id = 0

    @Parameter(names = ["-h", "--height"], description = "Height of configuration", required = true)
    private var height = 0

    @Parameter(names = ["-c", "--config"], description = "Configuration file of blockchain (gtxml or binary)", required = true)
    private var configFile = ""

    override fun key(): String = "add-configuration"

    override fun execute() {
        println("add-configuration will be executed with options: " +
                "${ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE)}")
    }
}