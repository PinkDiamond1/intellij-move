package org.move.lang.core.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.ext.ty
import org.move.lang.core.psi.returnTy
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.isCompatible
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

fun LookupElement.toMvLookupElement(properties: LookupElementProperties): MvLookupElement =
    MvLookupElement(this, properties)

class MvLookupElement(
    delegate: LookupElement,
    val props: LookupElementProperties
) : LookupElementDecorator<LookupElement>(delegate) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as MvLookupElement

        if (props != other.props) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + props.hashCode()
        return result
    }
}

data class LookupElementProperties(
    /**
     * `true` if after insertion of the lookup element it will form an expression with a type
     * that conforms to the expected type of that expression.
     *
     * ```
     * fn foo() -> String { ... } // isReturnTypeConformsToExpectedType = true
     * fn bar() -> i32 { ... }    // isReturnTypeConformsToExpectedType = false
     * fn main() {
     *     let a: String = // <-- complete here
     * }
     * ```
     */
    val isReturnTypeConformsToExpectedType: Boolean = false,

    val isCompatibleWithContext: Boolean = false,

    val typeHasAllRequiredAbilities: Boolean = false,
)

fun lookupProperties(element: MvNamedElement, context: CompletionContext): LookupElementProperties {
    val ctx = InferenceContext(context.itemVis.isMsl)
    var props = LookupElementProperties()
    if (context.expectedTy !is TyUnknown) {
        val ty = element.asTy(ctx)
        props = props.copy(isReturnTypeConformsToExpectedType = isCompatible(context.expectedTy, ty))
    }
    return props
}

private fun MvNamedElement.asTy(ctx: InferenceContext): Ty =
    when (this) {
//        is RsConstant -> typeReference?.type
//        is RsConstParameter -> typeReference?.type
//        is RsFieldDecl -> typeReference?.type
        is MvFunction -> this.returnTy
//        is RsStructItem -> declaredType
//        is RsEnumVariant -> parentEnum.declaredType
//        is MvBindingPat -> this.cachedTy(ctx)
        else -> TyUnknown
    }