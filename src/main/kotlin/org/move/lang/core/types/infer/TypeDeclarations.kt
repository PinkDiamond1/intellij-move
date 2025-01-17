package org.move.lang.core.types.infer

import org.move.ide.annotator.INTEGER_TYPE_IDENTIFIERS
import org.move.ide.annotator.SPEC_INTEGER_TYPE_IDENTIFIERS
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.mutable
import org.move.lang.core.psi.ext.typeArguments
import org.move.lang.core.types.ty.*

fun inferBuiltinTypeTy(moveType: MvPathType, inferenceCtx: InferenceContext): Ty {
    val refName = moveType.path.referenceName ?: return TyUnknown
    if (inferenceCtx.msl && refName in SPEC_INTEGER_TYPE_IDENTIFIERS) return TyInteger.fromName("num")
    return when (refName) {
        in INTEGER_TYPE_IDENTIFIERS -> TyInteger.fromName(refName)
        "bool" -> TyBool
        "address" -> TyAddress
        "signer" -> TySigner
        "vector" -> {
            val itemTy = moveType.path.typeArguments
                .firstOrNull()
                ?.type
                ?.let { inferTypeTy(it, inferenceCtx) } ?: TyUnknown
            return TyVector(itemTy)
        }
        else -> TyUnknown
    }
}

fun inferTypeTy(moveType: MvType, inferenceCtx: InferenceContext): Ty {
    val existingTy = inferenceCtx.typeTypes[moveType]
    if (existingTy != null) {
        return existingTy
    }
    val ty = when (moveType) {
        is MvPathType -> run {
            val struct =
                moveType.path.reference?.resolve() ?: return@run inferBuiltinTypeTy(moveType, inferenceCtx)
            when (struct) {
                is MvTypeParameter -> TyTypeParameter(struct)
                is MvStruct -> {
                    val typeArgs = moveType.path.typeArguments.map { inferTypeTy(it.type, inferenceCtx) }
                    val structTy = instantiateItemTy(struct, inferenceCtx) as? TyStruct ?: return TyUnknown

                    val ctx = InferenceContext(inferenceCtx.msl)
                    for ((tyVar, tyArg) in structTy.typeVars.zip(typeArgs)) {
                        ctx.addConstraint(tyVar, tyArg)
                    }
                    ctx.processConstraints()
                    ctx.resolveTy(structTy)
                }
                else -> TyUnknown
            }
        }
        is MvRefType -> run {
            val mutabilities = RefPermissions.valueOf(moveType.mutable)
            val innerTypeRef = moveType.type ?: return@run TyReference(TyUnknown, mutabilities, inferenceCtx.msl)
            val innerTy = inferTypeTy(innerTypeRef, inferenceCtx)
            TyReference(innerTy, mutabilities, inferenceCtx.msl)
        }
        is MvTupleType -> {
            val innerTypes = moveType.typeList.map { inferTypeTy(it, inferenceCtx) }
            TyTuple(innerTypes)
        }
        is MvUnitType -> TyUnit
        else -> TyUnknown
    }
    inferenceCtx.cacheTypeTy(moveType, ty)
    return ty
}
