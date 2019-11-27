package net.postchain.debug

import java.util.Comparator.comparingInt

class DefaultNodeDiagnosticContext : NodeDiagnosticContext {

    private val properties: MutableMap<DiagnosticProperty, () -> Any?> = mutableMapOf()

    override fun addProperty(property: DiagnosticProperty, value: Any?) {
        properties[property] = { value }
    }

    override fun addProperty(property: DiagnosticProperty, lazyValue: () -> Any?) {
        properties[property] = lazyValue
    }

    override fun getProperty(property: DiagnosticProperty): Any? {
        return properties[property]
    }

    override fun getProperties(): Map<String, Any?> {
        return properties
                .toSortedMap(comparingInt(DiagnosticProperty::ordinal))
                .mapKeys { (k, _) -> k.prettyName }
                .mapValues { (_, v) -> v() }
    }

    override fun removeProperty(property: DiagnosticProperty) {
        properties.remove(property)
    }
}