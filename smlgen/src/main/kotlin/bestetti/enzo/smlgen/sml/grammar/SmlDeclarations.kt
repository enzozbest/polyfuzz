package bestetti.enzo.smlgen.sml.grammar

import bestetti.enzo.smlgen.gen.AtomicGenerators.empty
import bestetti.enzo.smlgen.gen.AtomicGenerators.literal
import bestetti.enzo.smlgen.gen.Generator
import bestetti.enzo.smlgen.gen.GeneratorCombinators.depthChoice
import bestetti.enzo.smlgen.gen.GeneratorCombinators.lazy
import bestetti.enzo.smlgen.gen.GeneratorCombinators.optional
import bestetti.enzo.smlgen.gen.GeneratorCombinators.sepBy1
import bestetti.enzo.smlgen.gen.GeneratorCombinators.weightedChoice
import bestetti.enzo.smlgen.gen.GeneratorConveniences.F

/**
 * Declaration generators for Standard ML.
 * Based on the SML '97 Definition declaration grammar.
 */
object SmlDeclarations {

    /** Simple value binding: pat = exp */
    private val simpleValBind: Generator = lazy {
        SmlPatterns.pattern F SmlLexical.optWs F "=" F SmlLexical.optWs F SmlExpressions.simpleExpression
    }

    /** Value declaration: val pat = exp [and pat = exp ...] */
    val valDec: Generator = lazy {
        "val" F SmlLexical.reqWs F sepBy1(simpleValBind, SmlLexical.reqWs F "and" F SmlLexical.reqWs)
    }

    /** Recursive value declaration: val rec ... */
    val valRecDec: Generator = lazy {
        "val" F SmlLexical.reqWs F literal("rec") F SmlLexical.reqWs F
                sepBy1(simpleValBind, SmlLexical.reqWs F "and" F SmlLexical.reqWs)
    }

    /** Function clause: fname atpat ... = exp */
    private val funClause: Generator = lazy {
        SmlLexical.valueId F SmlLexical.reqWs F SmlPatterns.funArgPats F SmlLexical.optWs F "=" F SmlLexical.optWs F
                SmlExpressions.simpleExpression
    }

    /** Function binding with multiple clauses */
    private val funBind: Generator = lazy {
        sepBy1(funClause, SmlLexical.optWs F "|" F SmlLexical.optWs)
    }

    /** Function declaration: fun fname atpat ... = exp [| fname atpat ... = exp ...] [and ...] */
    val funDec: Generator = lazy {
        "fun" F SmlLexical.reqWs F sepBy1(funBind, SmlLexical.reqWs F "and" F SmlLexical.reqWs)
    }

    /** Type binding: tyvarseq tycon = ty */
    private val typeBind: Generator = lazy {
        SmlTypes.tyVarSeq F SmlLexical.tyconId F SmlLexical.optWs F "=" F SmlLexical.optWs F SmlTypes.type
    }

    /** Type declaration: type tyvarseq tycon = ty [and ...] */
    val typeDec: Generator = lazy {
        "type" F SmlLexical.reqWs F sepBy1(typeBind, SmlLexical.reqWs F "and" F SmlLexical.reqWs)
    }

    /** Constructor binding: Con [of ty] */
    private val conBind: Generator = lazy {
        weightedChoice(
            0.5 to SmlLexical.structId,
            0.5 to (SmlLexical.structId F SmlLexical.reqWs F "of" F SmlLexical.reqWs F SmlTypes.type)
        )
    }

    /** Datatype binding: tyvarseq tycon = Con [of ty] | ... */
    private val dataBind: Generator = lazy {
        SmlTypes.tyVarSeq F SmlLexical.tyconId F SmlLexical.optWs F "=" F SmlLexical.optWs F
                sepBy1(conBind, SmlLexical.optWs F "|" F SmlLexical.optWs)
    }

    /** Optional withtype clause */
    private val withType: Generator = lazy {
        optional(
            SmlLexical.reqWs F "withtype" F SmlLexical.reqWs F
                    sepBy1(typeBind, SmlLexical.reqWs F "and" F SmlLexical.reqWs)
        )
    }

    /** Datatype declaration */
    val datatypeDec: Generator = lazy {
        "datatype" F SmlLexical.reqWs F
                sepBy1(dataBind, SmlLexical.reqWs F "and" F SmlLexical.reqWs) F withType
    }

    /** Datatype replication: datatype tycon = datatype longtycon */
    val datatypeRepDec: Generator = lazy {
        "datatype" F SmlLexical.reqWs F SmlLexical.tyconId F SmlLexical.optWs F "=" F SmlLexical.optWs F
                "datatype" F SmlLexical.reqWs F SmlLexical.longTyconId
    }

    /** Exception binding: exn [of ty] or exn = longid */
    private val exnBind: Generator = lazy {
        weightedChoice(
            0.4 to SmlLexical.valueId,
            0.4 to (SmlLexical.valueId F SmlLexical.reqWs F "of" F SmlLexical.reqWs F SmlTypes.type),
            0.2 to (SmlLexical.valueId F SmlLexical.optWs F "=" F SmlLexical.optWs F SmlLexical.longValueId)
        )
    }

    /** Exception declaration */
    val exceptionDec: Generator = lazy {
        "exception" F SmlLexical.reqWs F sepBy1(exnBind, SmlLexical.reqWs F "and" F SmlLexical.reqWs)
    }

    /** Local declaration: local dec in dec end */
    val localDec: Generator = lazy {
        "local" F SmlLexical.reqWs F declarations F SmlLexical.reqWs F "in" F SmlLexical.reqWs F declarations F
                SmlLexical.reqWs F "end"
    }

    /** Infix declaration */
    val infixDec: Generator = lazy {
        "infix" F optional(SmlLexical.reqWs F { ctx -> ctx.random.nextInt(10).toString() }) F
                SmlLexical.reqWs F sepBy1(SmlLexical.valueId, SmlLexical.reqWs)
    }

    /** Infixr declaration */
    val infixrDec: Generator = lazy {
        "infixr" F optional(SmlLexical.reqWs F { ctx -> ctx.random.nextInt(10).toString() }) F
                SmlLexical.reqWs F sepBy1(SmlLexical.valueId, SmlLexical.reqWs)
    }

    /** Nonfix declaration */
    val nonfixDec: Generator = lazy {
        "nonfix" F SmlLexical.reqWs F sepBy1(SmlLexical.valueId, SmlLexical.reqWs)
    }

    /** Open declaration: open structid ... */
    val openDec: Generator = lazy {
        "open" F SmlLexical.reqWs F sepBy1(SmlLexical.structId, SmlLexical.reqWs)
    }

    //COMBINED DECLARATIONS
    /** Simple declaration (non-recursive, for depth control) */
    private val simpleDeclaration: Generator = weightedChoice(
        0.5 to valDec,
        0.3 to typeDec,
        0.1 to exceptionDec,
        0.05 to infixDec,
        0.05 to nonfixDec
    )

    /** Single declaration */
    val declaration: Generator = depthChoice(
        terminals = listOf(simpleDeclaration),
        recursives = listOf(
            weightedChoice(
                0.30 to valDec,
                0.25 to funDec,
                0.15 to datatypeDec,
                0.10 to typeDec,
                0.08 to exceptionDec,
                0.05 to localDec,
                0.03 to infixDec,
                0.02 to infixrDec,
                0.02 to nonfixDec
            )
        )
    )

    /** Sequence of declarations */
    val declarations: Generator = lazy {
        sepBy1(
            declaration,
            weightedChoice(
                0.5 to SmlLexical.reqWs,
                0.3 to (";" F SmlLexical.optWs),
                0.2 to (SmlLexical.newline F SmlLexical.optWs)
            )
        )
    }

    /** Empty declaration (allowed in SML) */
    val emptyDec: Generator = empty()
}
