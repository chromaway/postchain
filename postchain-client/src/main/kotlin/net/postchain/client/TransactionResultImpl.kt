// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.client

import net.postchain.core.TransactionStatus

class TransactionResultImpl(override val status: TransactionStatus) : TransactionResult
