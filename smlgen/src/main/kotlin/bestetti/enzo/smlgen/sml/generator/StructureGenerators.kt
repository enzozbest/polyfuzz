package bestetti.enzo.smlgen.sml.generator

import bestetti.enzo.smlgen.gen.AtomicGenerators
import bestetti.enzo.smlgen.gen.Generator
import bestetti.enzo.smlgen.gen.GeneratorCombinators.lazy
import bestetti.enzo.smlgen.gen.GeneratorCombinators.many
import bestetti.enzo.smlgen.gen.GeneratorCombinators.optional
import bestetti.enzo.smlgen.gen.GeneratorCombinators.weightedChoice
import bestetti.enzo.smlgen.gen.GeneratorConveniences.F
import bestetti.enzo.smlgen.sml.grammar.SmlDeclarations
import bestetti.enzo.smlgen.sml.grammar.SmlEdgeCase
import bestetti.enzo.smlgen.sml.grammar.SmlExpressions
import bestetti.enzo.smlgen.sml.grammar.SmlLexical
import bestetti.enzo.smlgen.sml.grammar.SmlTypes

object StructureGenerators {
    /** Top-level program element */
    fun programElement(config: ProgramConfig): Generator = lazy {
        val base = weightedChoice(
            0.5 to SmlDeclarations.declaration,
            0.3 to SmlExpressions.expression,
            0.1 to SmlLexical.comment,
            0.1 to AtomicGenerators.empty()
        )

        if (config.includeObscureFeatures) {
            weightedChoice(
                0.7 to base,
                0.1 to SmlEdgeCase.obscureDeclaration,
                0.1 to SmlEdgeCase.obscureExpression,
                0.05 to SmlEdgeCase.obscurePattern,
                0.05 to weightedChoice(
                    0.4 to SmlEdgeCase.obscureNumeric,
                    0.3 to SmlEdgeCase.obscureString,
                    0.3 to SmlEdgeCase.obscureChar
                )
            )
        } else {
            base
        }
    }

    val minimalProgram: Generator = weightedChoice(
        0.4 to SmlDeclarations.valDec,
        0.3 to SmlExpressions.simpleExpression,
        0.2 to SmlLexical.constant,
        0.1 to AtomicGenerators.literal("()")
    )

    val simpleProgram: Generator = lazy {
        weightedChoice(
            0.5 to SmlDeclarations.valDec,
            0.3 to SmlDeclarations.funDec,
            0.1 to SmlDeclarations.typeDec,
            0.1 to (SmlDeclarations.valDec F ";" F SmlLexical.optWs F SmlDeclarations.valDec)
        )
    }

    fun mediumProgram(config: ProgramConfig): Generator = lazy {
        (if (config.includeComments) optional(SmlLexical.comment) else AtomicGenerators.empty()) F
                SmlLexical.optNewline F
                SmlDeclarations.declarations F
                SmlLexical.optNewline F
                optional(SmlLexical.optWs F ";" F SmlLexical.optWs F SmlExpressions.expression)
    }

    fun complexProgram(config: ProgramConfig): Generator = lazy {
        (if (config.includeComments) optional(SmlLexical.comment) else AtomicGenerators.empty()) F
                SmlLexical.optNewline F

                // Type and datatype declarations first
                optional(
                    weightedChoice(0.6 to SmlDeclarations.datatypeDec, 0.4 to SmlDeclarations.typeDec) F
                            SmlLexical.reqWs
                ) F
                // Main declarations
                SmlDeclarations.declarations F
                SmlLexical.optNewline F

                // Optional exception declaration
                optional(SmlLexical.reqWs F SmlDeclarations.exceptionDec) F
                SmlLexical.optNewline F

                // Optional expression
                optional(";" F SmlLexical.optWs F SmlExpressions.expression) F

                // Trailing comment
                if (config.includeComments) optional(SmlLexical.comment) else AtomicGenerators.empty()
    }

    fun extremeProgram(config: ProgramConfig): Generator = lazy {
        // Opening comment
        (if (config.includeComments) SmlLexical.comment else AtomicGenerators.empty()) F
                SmlLexical.optNewline F

                // Infix declarations
                optional(SmlDeclarations.infixDec F SmlLexical.reqWs) F

                // Datatype with all features
                optional(
                    ("datatype " F SmlTypes.tyVarSeq F SmlLexical.tyconId F " = " F SmlLexical.structId F optional(
                        " of " F SmlTypes.type
                    )) F
                            many(" | " F SmlLexical.structId F optional(" of " F SmlTypes.type)) F SmlLexical.reqWs
                ) F

                // Exception declarations
                optional(SmlDeclarations.exceptionDec F SmlLexical.reqWs) F

                // Local block with nested declarations
                optional(
                    "local" F SmlLexical.reqWs F SmlDeclarations.declaration F SmlLexical.reqWs F "in" F SmlLexical.reqWs F
                            SmlDeclarations.declaration F SmlLexical.reqWs F "end" F SmlLexical.reqWs
                ) F

                // Main declarations
                SmlDeclarations.declarations F
                SmlLexical.optNewline F

                // Complex expression
                optional(";" F SmlLexical.optWs F SmlExpressions.letExpr) F

                // Trailing comment
                if (config.includeComments) optional(SmlLexical.optWs F SmlLexical.comment) else AtomicGenerators.empty()
    }
}