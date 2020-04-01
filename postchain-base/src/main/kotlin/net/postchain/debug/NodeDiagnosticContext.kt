// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.debug

interface NodeDiagnosticContext {

    /**
     * TODO: [POS-97]
     */
    fun addProperty(property: DiagnosticProperty, value: Any?)

    /**
     * TODO: [POS-97]
     */
    fun addProperty(property: DiagnosticProperty, lazyValue: () -> Any?)

    /**
     * TODO: [POS-97]
     */
    fun getProperty(property: DiagnosticProperty): Any?

    /**
     * TODO: [POS-97]
     */
    fun getProperties(): Map<String, Any?>

    /**
     * TODO: [POS-97]
     */
    fun removeProperty(property: DiagnosticProperty)
}

