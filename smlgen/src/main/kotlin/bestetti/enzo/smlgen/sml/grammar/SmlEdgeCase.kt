package bestetti.enzo.smlgen.sml.grammar

import bestetti.enzo.smlgen.gen.AtomicGenerators.empty
import bestetti.enzo.smlgen.gen.Generator
import bestetti.enzo.smlgen.gen.GeneratorCombinators
import bestetti.enzo.smlgen.gen.GeneratorCombinators.weightedChoice
import bestetti.enzo.smlgen.gen.GeneratorCombinators.wrapBrackets
import bestetti.enzo.smlgen.gen.GeneratorCombinators.wrapParens
import bestetti.enzo.smlgen.gen.GeneratorConveniences.F
import bestetti.enzo.smlgen.gen.GeneratorConveniences.toGenerator

object SmlEdgeCase {
    /** Symbolic identifier used as value */
    val obscureSymbolicId: Generator = Generator { ctx ->
        // Generate unusual but valid symbolic identifiers
        val symbols = listOf(
            "!!!", "%%%", "&&&", "???", "<<<", ">>>", "~~~",
            "+-+", "<=>", "|>", "<|", ">>>=", "<<<<=",
            "\\\\\\", "^^^", "$$$", "@@@", "***"
        )
        symbols[ctx.random.nextInt(symbols.size)]
    }

    /** Edge-case numeric constants */
    val obscureNumeric: Generator = Generator { ctx ->
        val cases = listOf(
            "0", "~0", "0x0", "~0x0", "0w0", "0wx0", "0.0", "~0.0", "0E0", "0.0e~0", "1E~10",
            "9999999999", "0xFFFFFFFF", "0wFFFFFFFF", "123456789.987654321", "1.0e10", "~1.0E~10"
        )
        cases[ctx.random.nextInt(cases.size)]
    }

    /** Edge-case string constants with special escapes */
    val obscureString: Generator = Generator { ctx ->
        val cases = listOf(
            "\"\"",                         // Empty string
            "\"\\n\\t\\r\\f\\v\\b\\a\"",    // All special escapes
            "\"\\\\\\\"\"",                 // Backslash and quote
            "\"\\000\\001\\127\\255\"",     // Decimal escapes
            "\"\\u0000\\u00FF\\uFFFF\"",    // Unicode escapes
            "\"a\\^@b\\^Ac\\^_d\"",         // Control character escapes (if supported)
            "\"\\ \\\"",                    // Gap (whitespace between backslashes)
            "\"\\\n\\\"",                   // Gap with newline
            "\"\\\t\n \\\"",                // Gap with mixed whitespace
            "\"αβγδ\"",                     // Unicode characters (if supported)
            "\"日本語\"",                    // CJK characters (if supported)
            "\"🎉\"",                       // Emoji (if supported)
            "\"\\u03B1\\u03B2\\u03B3\""     // Unicode escapes for Greek
        )
        cases[ctx.random.nextInt(cases.size)]
    }

    /** Edge-case character constants */
    val obscureChar: Generator = Generator { ctx ->
        val cases = listOf(
            "#\"\\n\"",      // Newline char
            "#\"\\t\"",      // Tab char
            "#\"\\\\\"",     // Backslash char
            "#\"\\\"\"",     // Quote char
            "#\"\\000\"",    // Null char
            "#\"\\255\"",    // Max byte char
            "#\" \"",        // Space char
            "#\"\\u0000\"",  // Unicode null
            "#\"\\u00FF\""   // Unicode 255
        )
        cases[ctx.random.nextInt(cases.size)]
    }

    /** Obscure but valid patterns */
    val obscurePattern: Generator = GeneratorCombinators.lazy {
        weightedChoice(
            // Multiple wildcards in tuple
            0.2 to "(_, _, _)".toGenerator(),
            // Deeply nested pattern
            0.2 to "((x, y), (z, w))".toGenerator(),
            // Pattern with type constraint
            0.2 to (SmlLexical.valueId F " : int"),
            // As pattern (layered)
            0.2 to (SmlLexical.valueId F " as " F SmlLexical.valueId),
            // Record with ellipsis
            0.2 to "{...}".toGenerator()
        )
    }

    /** Obscure expressions testing edge cases */
    val obscureExpression: Generator = GeneratorCombinators.lazy {
        weightedChoice(
            // Chained comparisons (requires parentheses)
            0.15 to "(1 < 2) andalso (2 < 3)".toGenerator(),
            // Op prefix usage
            0.15 to ("op" F SmlLexical.reqWs F SmlLexical.infixOp),
            // Record field selection
            0.15 to ("#1" F SmlLexical.reqWs F "(1, 2)"),
            // Raise and handle
            0.15 to "(raise Fail \"x\") handle Fail _ => 0".toGenerator(),
            // Nested fn
            0.1 to "fn x => fn y => x + y".toGenerator(),
            // Empty list
            0.1 to empty().wrapBrackets(),
            // Unit
            0.1 to empty().wrapParens(),
            // Sequence expression
            0.1 to "print \"a\"; print \"b\"; ()".toGenerator().wrapParens()
        )
    }

    /** Obscure declarations testing edge cases */
    val obscureDeclaration: Generator = GeneratorCombinators.lazy {
        weightedChoice(
            // Mutual recursion with and
            0.2 to "fun f x = g x and g x = f x".toGenerator(),
            // Datatype with multiple constructors
            0.2 to "datatype t = A | B of int | C of int * int".toGenerator(),
            // Type abbreviation with type variable
            0.15 to "type 'a pair = 'a * 'a".toGenerator(),
            // Infix declaration with precedence
            0.15 to "infix 5 +++".toGenerator(),
            // Exception with payload
            0.15 to "exception MyExn of string".toGenerator(),
            // Local declaration
            0.15 to "local val x = 1 in val y = x end".toGenerator()
        )
    }
}