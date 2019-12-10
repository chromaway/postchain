package net.postchain.integrationtest.managedmode

import net.postchain.core.EContext
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvArray

internal fun dummyHandlerArray(target: Unit, eContext: EContext, args: Gtv): Gtv {
    return GtvArray(emptyArray())
}

internal fun dummyHandlerArray(target: ManagedTestModuleTwoPeersConnect.Companion.Nodes, eContext: EContext, args: Gtv): Gtv {
    return GtvArray(emptyArray())
}
