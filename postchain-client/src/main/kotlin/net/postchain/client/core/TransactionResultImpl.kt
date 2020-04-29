// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.client.core

import net.postchain.client.core.TransactionResult
import net.postchain.core.TransactionStatus

class TransactionResultImpl(override val status: TransactionStatus) : TransactionResult
