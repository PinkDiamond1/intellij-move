package org.move.lang.core

import com.intellij.patterns.*
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.tree.TokenSet
import com.intellij.util.ProcessingContext
import org.move.lang.MoveElementTypes.*
import org.move.lang.MoveFile
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.leftLeaves

object MovePsiPatterns {
    private val STATEMENT_BOUNDARIES = TokenSet.create(SEMICOLON, L_BRACE, R_BRACE)

    val whitespace: PsiElementPattern.Capture<PsiElement> = PlatformPatterns.psiElement().whitespace()

    val onStatementBeginning: PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement().with(OnStatementBeginning())

    fun onStatementBeginning(vararg startWords: String): PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement().with(OnStatementBeginning(*startWords))

    fun toplevel(): PsiElementPattern.Capture<PsiElement> =
        psiElementWithParent<MoveFile>()

    fun addressBlock(): PsiElementPattern.Capture<PsiElement> =
        psiElementWithParent<MoveAddressBlock>()

    fun moduleBlock(): PsiElementPattern.Capture<PsiElement> =
        psiElementWithParent<MoveModuleBlock>()

    fun scriptBlock(): PsiElementPattern.Capture<PsiElement> =
        psiElementWithParent<MoveScriptBlock>()

    fun codeStatement(): PsiElementPattern.Capture<PsiElement> =
        psiElementInside<MoveCodeBlock>()

    fun typeRef(): PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement()
            .withSuperParent<MoveQualifiedPathType>(2)

    fun typeParamBound(): PsiElementPattern.Capture<PsiElement> =
        psiElementWithParent<MoveTypeParameterList>()
            .afterLeafSkipping(
                PlatformPatterns.psiElement().whitespaceCommentEmptyOrError(),
                PlatformPatterns.psiElement(COLON),
            )

    fun qualifiedPathIdentifier(): PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement()
            .withParent<MoveQualifiedPath>()
            .withCond("FirstChild") { e -> e.prevSibling == null }

    fun qualifiedPathTypeIdentifier(): PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement()
            .withSuperParent<MoveQualifiedPathType>(2)
            .withCond("FirstChild") { e -> e.prevSibling == null }

    fun specIdentifier(): PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement()
            .withSuperParent<MoveItemSpecDef>(2)

    private inline fun <reified I : PsiElement> psiElementWithParent() =
        PlatformPatterns.psiElement().withParent(
            StandardPatterns.or(
                psiElement<I>(),
                psiElement<PsiErrorElement>().withParent(psiElement<I>())
            )
        )

    private inline fun <reified I : PsiElement> psiElementInside() =
        PlatformPatterns.psiElement().inside(
            StandardPatterns.or(
                psiElement<I>(),
                psiElement<PsiErrorElement>().withParent(psiElement<I>())
            )
        )

    private class OnStatementBeginning(vararg startWords: String) :
        PatternCondition<PsiElement>("on statement beginning") {
        val myStartWords = startWords
        override fun accepts(t: PsiElement, context: ProcessingContext?): Boolean {
            val prev = t.prevVisibleOrNewLine
            return if (myStartWords.isEmpty())
                prev == null || prev is PsiWhiteSpace || prev.node.elementType in STATEMENT_BOUNDARIES
            else {
                prev != null && prev.node.text in myStartWords
            }
        }
    }
}

private val PsiElement.prevVisibleOrNewLine: PsiElement?
    get() {
        return leftLeaves
            .filterNot { it is PsiComment || it is PsiErrorElement }
            .filter { it !is PsiWhiteSpace || it.textContains('\n') }
            .firstOrNull()

    }

inline fun <reified I : PsiElement> psiElement(): PsiElementPattern.Capture<I> {
    return PlatformPatterns.psiElement(I::class.java)
}

inline fun <reified I : PsiElement> PsiElementPattern.Capture<PsiElement>.withParent(): PsiElementPattern.Capture<PsiElement> {
    return this.withSuperParent(1, I::class.java)
}

inline fun <reified I : PsiElement> PsiElementPattern.Capture<PsiElement>.withSuperParent(level: Int): PsiElementPattern.Capture<PsiElement> {
    return this.withSuperParent(level, I::class.java)
}

fun <T, Self : ObjectPattern<T, Self>> ObjectPattern<T, Self>.withCond(
    name: String,
    cond: (T) -> Boolean,
): Self =
    with(object : PatternCondition<T>(name) {
        override fun accepts(t: T, context: ProcessingContext?): Boolean = cond(t)
    })

//
//inline fun <reified I : PsiElement> psiElementOrError(): PsiElementPattern.Capture<I> {
//    return PlatformPatterns.psiElement(I::class.java)
//        .andOr(PlatformPatterns.psiElement().withParent(MovePatterns.error))
//}

