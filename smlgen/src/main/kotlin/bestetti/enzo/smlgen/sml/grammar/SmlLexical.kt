package bestetti.enzo.smlgen.sml.grammar

import bestetti.enzo.smlgen.gen.AtomicGenerators.empty
import bestetti.enzo.smlgen.gen.AtomicGenerators.literal
import bestetti.enzo.smlgen.gen.AtomicGenerators.oneOf
import bestetti.enzo.smlgen.gen.AtomicGenerators.ws
import bestetti.enzo.smlgen.gen.Generator
import bestetti.enzo.smlgen.gen.GeneratorCombinators.between
import bestetti.enzo.smlgen.gen.GeneratorCombinators.optional
import bestetti.enzo.smlgen.gen.GeneratorCombinators.weightedChoice
import bestetti.enzo.smlgen.gen.GeneratorConveniences.X
import bestetti.enzo.smlgen.gen.GeneratorConveniences.toGenerator

/**
 * Lexical element generators for Standard ML.
 * Based on the SML '97 Definition lexical specification.
 */
object SmlLexical {
    val alphabet = 0..255

    /** Single space */
    val space: Generator = ws()

    /** Optional whitespace */
    val optWs: Generator = optional { ctx -> space.generate(ctx) }

    /** Required whitespace */
    val reqWs: Generator = space F optWs

    /** Newline */
    val newline: Generator = "\n".toGenerator() X "\r\n".toGenerator()

    /** Optional newlines and indentation */
    val optNewline: Generator = weightedChoice(
        0.7 to empty(),
        0.2 to newline,
        0.1 to (newline F ("  " X "\t"))
    )

    /** Simple comment content - avoids nested comment markers and unbalanced delimiters */
    private val commentChar: Generator = Generator { ctx ->
        val commentAlphabet = alphabet.map { it.toChar() }
        commentAlphabet[ctx.random.nextInt(commentAlphabet.size)].toString()
    }

    /** Comment content - ensures no nested comment markers, including at boundaries */
    private val commentContent: Generator = Generator { ctx ->
        val length = ctx.random.nextInt(20)
        var content = buildString {
            repeat(length) {
                val c = commentChar(ctx)
                // Avoid creating (* or *) within content
                when {
                    c == "*" && isNotEmpty() && last() == '(' -> {
                        // Skip - would form (*
                    }
                    c == ")" && isNotEmpty() && last() == '*' -> {
                        // Skip - would form *)
                    }
                    else -> append(c)
                }
            }
        }
        // Remove leading * (would combine with opener "(" to form "(*")
        while (content.startsWith("*")) {
            content = content.drop(1)
        }
        // Remove trailing ( (would combine with closer "*" to form "(*")
        while (content.endsWith("(")) {
            content = content.dropLast(1)
        }
        // Remove leading ) (would combine with opener "*" to form "*)") - opener is "(*, so last char is *"
        while (content.startsWith(")")) {
            content = content.drop(1)
        }
        // Remove trailing * (would combine with closer ")" to form "*)")
        while (content.endsWith("*")) {
            content = content.dropLast(1)
        }
        content
    }

    /** Non-nested comment */
    val comment: Generator = commentContent.between("(*".toGenerator(), "*)".toGenerator())

    /** Optional comment */
    val optComment: Generator = weightedChoice(
        0.9 to empty(),
        0.1 to comment
    )

    /** Lowercase letter */
    private val lowerLetter: Generator = Generator { ctx ->
        ('a' + ctx.random.nextInt(26)).toString()
    }

    /** Uppercase letter */
    private val upperLetter: Generator = Generator { ctx ->
        ('A' + ctx.random.nextInt(26)).toString()
    }

    /** Any letter */
    private val letter: Generator = lowerLetter X upperLetter

    /** Digit */
    private val digit: Generator = Generator { ctx ->
        ('0' + ctx.random.nextInt(10)).toString()
    }

    /** Hex digit */
    private val hexDigit: Generator = Generator { ctx ->
        "0123456789abcdefABCDEF"[ctx.random.nextInt(22)].toString()
    }

    /** Prime (tick mark for type variables) */
    private val prime: Generator = "'".toGenerator()

    /** Underscore */
    private val underscore: Generator = "_".toGenerator()

    /** Alphanumeric identifier character (letter, digit, underscore, prime) */
    private val alphanumChar: Generator = weightedChoice(
        0.6 to letter,
        0.2 to digit,
        0.1 to underscore,
        0.1 to prime
    )

    /** Alphanumeric identifier (starts with letter) */
    val alphanumId: Generator = Generator { ctx ->
        val first = letter(ctx)
        val restLen = ctx.random.nextInt(11) // 0-10 more chars
        first + buildString {
            repeat(restLen) { append(alphanumChar(ctx)) }
        }
    }

    /** Short alphanumeric identifier */
    val shortAlphanumId: Generator = Generator { ctx ->
        val first = lowerLetter(ctx)
        val restLen = ctx.random.nextInt(3) // 0-2 more chars
        first + buildString {
            repeat(restLen) {
                append(alphanumChar(ctx))
            }
        }
    }

    /** Symbolic characters allowed in symbolic identifiers */
    private val symbolicChar: Generator = oneOf(
        "!", "%", "&", "$", "#", "+", "-", "/", ":", "<", "=", ">", "?", "@", "\\", "~", "`", "^", "|", "*"
    )

    /** Symbolic identifier - must avoid single # or | which are reserved */
    val symbolicId: Generator = Generator { ctx ->
        val reserved = setOf("#", "|")
        var id: String
        do {
            val len = 1 + ctx.random.nextInt(3) // 1-3 symbolic chars
            id = buildString {
                repeat(len) { append(symbolicChar(ctx)) }
            }
        } while (id in reserved)
        id
    }

    /** Type variable (starts with prime) */
    val typeVar: Generator = prime F alphanumId

    /** Equality type variable (starts with two primes) */
    val eqTypeVar: Generator = prime F prime F alphanumId

    /** Any type variable */
    val anyTypeVar: Generator = weightedChoice(
        0.8 to typeVar,
        0.2 to eqTypeVar
    )

    /** Value identifier - avoiding reserved words */
    val valueId: Generator = Generator { ctx ->
        val reserved = setOf(
            "abstype", "and", "andalso", "as", "case", "datatype", "do", "else",
            "end", "exception", "fn", "fun", "handle", "if", "in", "infix",
            "infixr", "let", "local", "nonfix", "of", "op", "open", "orelse",
            "raise", "rec", "then", "type", "val", "while", "with", "withtype"
        )
        var id: String
        do {
            id = shortAlphanumId(ctx)
        } while (id in reserved)
        id
    }

    /** Type constructor identifier (typically lowercase) */
    val tyconId: Generator = valueId

    /** Structure identifier (typically capitalized) */
    val structId: Generator = Generator { ctx ->
        val first = upperLetter(ctx)
        val restLen = ctx.random.nextInt(5)
        first + buildString {
            repeat(restLen) { append(alphanumChar(ctx)) }
        }
    }

    /** Long value identifier (qualified with structure names) */
    val longValueId: Generator = weightedChoice(
        0.8 to valueId,
        0.2 to (structId F ".".toGenerator() F valueId)
    )

    /** Long type constructor identifier */
    val longTyconId: Generator = weightedChoice(
        0.8 to tyconId,
        0.2 to (structId F ".".toGenerator() F tyconId)
    )

    /** Label for record fields (alphanumeric or numeric) */
    val label: Generator = weightedChoice(
        0.8 to valueId,
        0.2 to Generator { ctx -> (1 + ctx.random.nextInt(10)).toString() }
    )

    /** Decimal integer constant */
    val decimalInt: Generator = Generator { ctx ->
        val negative = ctx.random.nextInt(10) == 0
        val numDigits = 1 + ctx.random.nextInt(5)
        val num = buildString {
            // First digit: 1-9 (or 0 if single digit)
            if (numDigits == 1) {
                append(ctx.random.nextInt(10))
            } else {
                append(1 + ctx.random.nextInt(9))
                repeat(numDigits - 1) { append(ctx.random.nextInt(10)) }
            }
        }
        if (negative) "~$num" else num
    }

    /** Hexadecimal integer constant */
    val hexInt: Generator = Generator { ctx ->
        val negative = ctx.random.nextInt(10) == 0
        val numDigits = 1 + ctx.random.nextInt(4)
        val hex = buildString {
            repeat(numDigits) { append(hexDigit(ctx)) }
        }
        val prefix = if (negative) "~0x" else "0x"
        prefix + hex
    }

    /** Word constant (unsigned) */
    val wordConst: Generator = Generator { ctx ->
        val isHex = ctx.random.nextBoolean()
        val numDigits = 1 + ctx.random.nextInt(4)
        val prefix = if (isHex) "0wx" else "0w"
        val digits = buildString {
            repeat(numDigits) { append(if (isHex) hexDigit(ctx) else ctx.random.nextInt(10)) }
        }
        prefix + digits
    }

    /** Integer constant */
    val intConst: Generator = weightedChoice(
        0.8 to decimalInt,
        0.2 to hexInt
    )

    /** Real constant */
    val realConst: Generator = Generator { ctx ->
        val negative = ctx.random.nextInt(10) == 0
        val intPart = buildString {
            val digits = 1 + ctx.random.nextInt(4)
            if (digits == 1) {
                append(ctx.random.nextInt(10))
            } else {
                append(1 + ctx.random.nextInt(9))
                repeat(digits - 1) { append(ctx.random.nextInt(10)) }
            }
        }
        val hasFrac = ctx.random.nextBoolean()
        val hasExp = ctx.random.nextBoolean()

        buildString {
            if (negative) append("~")
            append(intPart)
            if (hasFrac) {
                append(".")
                repeat(1 + ctx.random.nextInt(3)) { append(ctx.random.nextInt(10)) }
            }
            if (hasExp) {
                append(if (ctx.random.nextBoolean()) "e" else "E")
                if (ctx.random.nextBoolean()) append("~")
                repeat(1 + ctx.random.nextInt(2)) { append(ctx.random.nextInt(10)) }
            }
            // Must have fraction or exponent for real
            if (!hasFrac && !hasExp) {
                append(".0")
            }
        }
    }

    /** String escape sequence */
    private val stringEscape: Generator = weightedChoice(
        0.15 to literal("\\n"),
        0.15 to literal("\\t"),
        0.1 to literal("\\\\"),
        0.1 to literal("\\\""),
        0.1 to literal("\\a"),
        0.1 to literal("\\b"),
        0.1 to literal("\\v"),
        0.1 to literal("\\f"),
        0.05 to literal("\\r"),
        // \ddd decimal escape
        0.03 to Generator { ctx ->
            "\\${ctx.random.nextInt(256).toString().padStart(3, '0')}"
        },
        // \uxxxx Unicode escape
        0.02 to Generator { ctx ->
            "\\u${buildString { repeat(4) { append(hexDigit(ctx)) } }}"
        }
    )

    /** Printable string character (excluding " and \) */
    private val stringChar: Generator = Generator { ctx ->
        // Printable ASCII excluding " and \
        val printable = " !#\$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_`abcdefghijklmnopqrstuvwxyz{|}~"
        printable[ctx.random.nextInt(printable.length)].toString()
    }

    /** String content element */
    private val stringElement: Generator = weightedChoice(
        0.85 to stringChar,
        0.15 to stringEscape
    )

    /** String constant */
    val stringConst: Generator = Generator { ctx ->
        val len = ctx.random.nextInt(15)
        "\"" + buildString {
            repeat(len) { append(stringElement(ctx)) }
        } + "\""
    }

    /** Character constant */
    val charConst: Generator = Generator { ctx ->
        val useEscape = ctx.random.nextInt(5) == 0
        val content = if (useEscape) stringEscape(ctx) else stringChar(ctx)
        "#\"${content}\""
    }

    /** Any constant */
    val constant: Generator = weightedChoice(
        0.35 to intConst,
        0.15 to wordConst,
        0.15 to realConst,
        0.25 to stringConst,
        0.10 to charConst
    )

    /** Constants valid in patterns (no reals - SML doesn't allow real constants in patterns) */
    val patternConstant: Generator = weightedChoice(
        0.40 to intConst,
        0.20 to wordConst,
        0.25 to stringConst,
        0.15 to charConst
    )

    val builtinValueId: Generator = oneOf(
        "nil", "true", "false", "NONE", "SOME", "ref"
    )

    val builtinTypeId: Generator = oneOf(
        "int", "real", "bool", "string", "char", "unit", "list", "option", "ref", "word", "exn"
    )

    val infixOp: Generator = oneOf(
        "+", "-", "*", "/", "div", "mod", "<", ">", "<=", ">=", "=", "<>",
        "::", "@", "^", "o", "before", ":="
    )

    val prefixOp: Generator = oneOf(
        "!", "not", "~", "abs", "floor", "ceil", "trunc", "round"
    )
}
