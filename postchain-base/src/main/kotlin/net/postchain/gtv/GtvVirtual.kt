package net.postchain.gtv


/**
 * A Virtual GTV pretends to be the orignal GTV structure, but really only holds very few values.
 *
 * If the user of the [GtvVirtual] tries to find a value that this structure does not have, an exception will be thrown.
 */
abstract class GtvVirtual() : GtvCollection()




