package net.postchain.devtools.utils.configuration

import org.apache.commons.configuration2.io.ClasspathLocationStrategy
import org.apache.commons.configuration2.io.FileLocationStrategy
import org.apache.commons.configuration2.io.FileLocator
import org.apache.commons.configuration2.io.FileSystem
import java.io.File
import java.net.URL

class UniversalFileLocationStrategy : FileLocationStrategy {

    private val classpathLocationStrategy = ClasspathLocationStrategy()

    override fun locate(fileSystem: FileSystem?, locator: FileLocator?): URL {
        /*
        * FYI: We use Spring convention here when files under resources are labeled with prefix 'classpath:'.
        * */
        val resourcePrefix = "classpath:"

        return when {
            locator!!.basePath?.contains(resourcePrefix) ?: false -> {
                val path = locator.basePath.substringAfter(resourcePrefix) + File.separator + locator.fileName
                val resource = path.replace("\\", "/")
                javaClass.getResource(resource)
            }

            else -> classpathLocationStrategy.locate(fileSystem, locator)
        }
    }
}