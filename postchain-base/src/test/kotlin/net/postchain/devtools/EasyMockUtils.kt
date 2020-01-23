// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.devtools

import org.easymock.EasyMock

object EasyMockUtils {

    fun <T> anyOfType(type: Class<T>): T = EasyMock.anyObject(type)

}
