package org.move.lang.core.resolve

import org.move.lang.core.psi.MvNamedElement

data class SimpleScopeEntry<T: MvNamedElement>(
    val name: String,
    val element: T
)

fun interface MatchingProcessor<T: MvNamedElement> {
    fun match(entry: SimpleScopeEntry<T>): Boolean

    fun match(itemVis: ItemVis, element: T): Boolean {
        if (!element.isVisibleInScopes(itemVis)) return false
        val name = element.name ?: return false
        val entry = SimpleScopeEntry<T>(name, element)
        return match(entry)
    }

    fun matchAll(itemVis: ItemVis, vararg collections: Iterable<T>): Boolean =
        listOf(*collections)
            .flatten()
            .any { match(itemVis, it) }

//    private fun match(element: MvNamedElement): Boolean {
//        val name = element.name ?: return false
//        val entry = SimpleScopeEntry(name, element)
//        return match(entry)
//    }
}
