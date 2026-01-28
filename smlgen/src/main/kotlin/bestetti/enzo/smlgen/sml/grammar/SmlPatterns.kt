package bestetti.enzo.smlgen.sml.grammar

import bestetti.enzo.smlgen.gen.Generator
import bestetti.enzo.smlgen.gen.GeneratorCombinators.depthChoice
import bestetti.enzo.smlgen.gen.GeneratorCombinators.lazy
import bestetti.enzo.smlgen.gen.GeneratorCombinators.optional
import bestetti.enzo.smlgen.gen.GeneratorCombinators.sepBy
import bestetti.enzo.smlgen.gen.GeneratorCombinators.sepBy1
import bestetti.enzo.smlgen.gen.GeneratorCombinators.weightedChoice
import bestetti.enzo.smlgen.gen.GeneratorCombinators.wrapBraces
import bestetti.enzo.smlgen.gen.GeneratorCombinators.wrapBrackets
import bestetti.enzo.smlgen.gen.GeneratorCombinators.wrapParens
import bestetti.enzo.smlgen.gen.GeneratorConveniences.F
import bestetti.enzo.smlgen.gen.GeneratorConveniences.toGenerator

/**
 * Pattern generators for Standard ML.
 * Based on the SML '97 Definition pattern grammar.
 */
object SmlPatterns {

    //ATOMIC PATTERNS
    /** Wildcard pattern _ */
    val wildcard: Generator = "_".toGenerator()

    /** Variable pattern */
    val varPat: Generator = SmlLexical.valueId

    /** Constant pattern (no reals - SML doesn't allow real constants in patterns) */
    val constPat: Generator = SmlLexical.patternConstant

    /** Unit pattern () */
    val unitPat: Generator = "()".toGenerator()

    /** Nil pattern [] */
    val nilPat: Generator = "[]".toGenerator()

    /** Simple atomic pattern (non-recursive) */
    private val simpleAtomicPat: Generator = weightedChoice(
        0.35 to varPat,
        0.25 to wildcard,
        0.25 to constPat,
        0.1 to unitPat,
        0.05 to nilPat
    )

    //CONSTRUCTED PATTERNS
    /** Record pattern row: label = pat */
    private val patRow: Generator = lazy {
        weightedChoice(
            // Full form: label = pattern
            0.7 to (SmlLexical.label F SmlLexical.optWs F "=" F SmlLexical.optWs F pattern),
            // Punning: just label (means label = label)
            0.2 to SmlLexical.valueId,
            // Wildcard row: ...
            0.1 to "...".toGenerator()
        )
    }

    /** Record pattern { lab1 = pat1, lab2 = pat2, ... } */
    private val recordPat: Generator = lazy {
        (SmlLexical.optWs F
                sepBy1(patRow, "," F SmlLexical.optWs) F
                SmlLexical.optWs).wrapBraces()
    }

    /** Tuple pattern (pat1, pat2, ...) */
    private val tuplePat: Generator = lazy {
        (SmlLexical.optWs F
                pattern F "," F
                SmlLexical.optWs F
                sepBy1(pattern, "," F SmlLexical.optWs) F
                SmlLexical.optWs).wrapParens()
    }

    /** List pattern [pat1, pat2, ...] */
    private val listPat: Generator = lazy {
        (SmlLexical.optWs F
                sepBy(pattern, "," F SmlLexical.optWs) F
                SmlLexical.optWs).wrapBrackets()
    }

    /** Cons pattern pat1 :: pat2 */
    private val consPat: Generator = lazy {
        atomicPattern F SmlLexical.optWs F "::" F SmlLexical.optWs F pattern
    }

    /** Constructor pattern: Con pat or Con */
    private val constructorPat: Generator = lazy {
        weightedChoice(
            // Nullary constructors
            0.4 to SmlLexical.builtinValueId,
            // SOME pat
            0.3 to ("SOME" F SmlLexical.reqWs F atomicPattern),
            // Custom constructor with argument
            0.3 to (SmlLexical.structId F SmlLexical.reqWs F atomicPattern)
        )
    }

    /** Typed pattern: pat : ty */
    private val typedPat: Generator = lazy {
        atomicPattern F SmlLexical.optWs F ":" F SmlLexical.optWs F SmlTypes.type
    }

    /** Layered pattern: id as pat */
    private val layeredPat: Generator = lazy {
        SmlLexical.valueId F SmlLexical.reqWs F "as" F SmlLexical.reqWs F pattern
    }

    /** Parenthesized pattern */
    private val parenPat: Generator = lazy {
        (SmlLexical.optWs F pattern F SmlLexical.optWs).wrapParens()
    }

    //COMBINED PATTERNS
    /** Atomic pattern (for use in constructed patterns) */
    val atomicPattern: Generator = depthChoice(
        terminals = listOf(simpleAtomicPat),
        recursives = listOf(
            weightedChoice(
                0.4 to simpleAtomicPat,
                0.2 to recordPat,
                0.15 to tuplePat,
                0.15 to listPat,
                0.1 to parenPat
            )
        )
    )

    /** Full pattern */
    val pattern: Generator = depthChoice(
        terminals = listOf(simpleAtomicPat),
        recursives = listOf(
            weightedChoice(
                0.35 to simpleAtomicPat,
                0.15 to consPat,
                0.1 to constructorPat,
                0.1 to typedPat,
                0.1 to tuplePat,
                0.08 to listPat,
                0.07 to recordPat,
                0.05 to layeredPat
            )
        )
    )

    //PATTERN SEQUENCES
    /** Pattern for function arguments (multiple atomic patterns) */
    val funArgPats: Generator = lazy {
        sepBy1(atomicPattern, SmlLexical.reqWs)
    }

    /** Optional type annotation for patterns */
    val optTypeAnnotation: Generator = optional(
        SmlLexical.optWs F ":" F SmlLexical.optWs F SmlTypes.type
    )
}
