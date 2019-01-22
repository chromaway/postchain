package net.postchain.devtools

import org.easymock.EasyMock

object EasyMockUtils {

    fun <T> anyOfType(type: Class<T>): T = EasyMock.anyObject(type)

}
