package bestetti.enzo.smlgen.sml.grammar

import bestetti.enzo.smlgen.gen.AtomicGenerators.literal
import bestetti.enzo.smlgen.gen.Generator
import bestetti.enzo.smlgen.gen.GeneratorCombinators.depthChoice
import bestetti.enzo.smlgen.gen.GeneratorCombinators.lazy
import bestetti.enzo.smlgen.gen.GeneratorCombinators.sepBy1
import bestetti.enzo.smlgen.gen.GeneratorCombinators.weightedChoice
import bestetti.enzo.smlgen.gen.GeneratorCombinators.wrapBraces
import bestetti.enzo.smlgen.gen.GeneratorCombinators.wrapParens
import bestetti.enzo.smlgen.gen.GeneratorConveniences.F

/**
 * Type expression generators for Standard ML.
 * Based on the SML '97 Definition type grammar.
 */
object SmlTypes {

    //TYPE EXPRESSIONS
    /** Type variable ('a, ''b, etc.) */
    val tyVar: Generator = SmlLexical.anyTypeVar

    /** Type constant (int, bool, etc.) */
    private val tyConst: Generator = weightedChoice(
        0.7 to SmlLexical.builtinTypeId,
        0.3 to SmlLexical.longTyconId
    )

    /** Simple type (non-recursive) */
    private val simpleType: Generator = weightedChoice(
        0.4 to tyVar,
        0.6 to tyConst
    )

    /** Record type row: label : ty */
    private val tyRow: Generator = lazy {
        SmlLexical.label F SmlLexical.optWs F ":" F SmlLexical.optWs F type
    }

    /** Record type { lab1 : ty1, lab2 : ty2, ... } */
    private val recordType: Generator = lazy {
        (SmlLexical.optWs F sepBy1(tyRow, "," F SmlLexical.optWs) F SmlLexical.optWs).wrapBraces()
    }

    /** Tuple type: ty1 * ty2 * ... */
    private val tupleType: Generator = lazy {
        atomicType F SmlLexical.optWs F "*" F SmlLexical.optWs F
                sepBy1(atomicType, SmlLexical.optWs F "*" F SmlLexical.optWs)
    }

    /** Function type: ty1 -> ty2 */
    private val functionType: Generator = lazy {
        atomicType F SmlLexical.optWs F "->" F SmlLexical.optWs F type
    }

    /** Type constructor application: ty tycon or (ty1, ty2) tycon */
    private val tyConApp: Generator = lazy {
        weightedChoice(
            0.6 to (atomicType F SmlLexical.reqWs F tyConst),
            0.4 to (sepBy1(type, ("," F SmlLexical.optWs)).wrapParens() F SmlLexical.reqWs F tyConst)
        )
    }

    /** Parenthesised type */
    private val parenType: Generator = lazy {
        (SmlLexical.optWs F type F SmlLexical.optWs).wrapParens()
    }

    /** Atomic type (for use in tuples and function types) */
    val atomicType: Generator = depthChoice(
        terminals = listOf(simpleType),
        recursives = listOf(
            weightedChoice(
                0.5 to simpleType,
                0.2 to recordType,
                0.15 to parenType,
                0.15 to tyConApp
            )
        )
    )

    /** Full type expression */
    val type: Generator = depthChoice(
        terminals = listOf(simpleType),
        recursives = listOf(
            weightedChoice(
                0.35 to simpleType,
                0.25 to functionType,
                0.15 to tupleType,
                0.1 to recordType,
                0.1 to tyConApp,
                0.05 to parenType
            )
        )
    )

    /** Type variable sequence for datatype/type declarations */
    val tyVarSeq: Generator = weightedChoice(
        0.5 to literal(""),
        0.3 to (tyVar F SmlLexical.reqWs),
        0.2 to (sepBy1(tyVar, "," F SmlLexical.optWs).wrapParens() F SmlLexical.reqWs)
    )

    //COMMON TYPE PATTERNS
    /** List type: ty list */
    val listType: Generator = atomicType F SmlLexical.reqWs F "list"

    /** Option type: ty option */
    val optionType: Generator = atomicType F SmlLexical.reqWs F "option"

    /** Ref type: ty ref */
    val refType: Generator = atomicType F SmlLexical.reqWs F "ref"

    /** Common concrete type for simple programs */
    val commonType: Generator = SmlLexical.builtinTypeId X listType X optionType
}
