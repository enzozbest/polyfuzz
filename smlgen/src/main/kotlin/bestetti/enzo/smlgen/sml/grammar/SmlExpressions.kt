package bestetti.enzo.smlgen.sml.grammar

import bestetti.enzo.smlgen.gen.AtomicGenerators.literal
import bestetti.enzo.smlgen.gen.Generator
import bestetti.enzo.smlgen.gen.GeneratorCombinators.depthChoice
import bestetti.enzo.smlgen.gen.GeneratorCombinators.lazy
import bestetti.enzo.smlgen.gen.GeneratorCombinators.many1
import bestetti.enzo.smlgen.gen.GeneratorCombinators.sepBy
import bestetti.enzo.smlgen.gen.GeneratorCombinators.sepBy1
import bestetti.enzo.smlgen.gen.GeneratorCombinators.weightedChoice
import bestetti.enzo.smlgen.gen.GeneratorCombinators.wrapBraces
import bestetti.enzo.smlgen.gen.GeneratorCombinators.wrapBrackets
import bestetti.enzo.smlgen.gen.GeneratorCombinators.wrapParens
import bestetti.enzo.smlgen.gen.GeneratorConveniences.F
import bestetti.enzo.smlgen.gen.GeneratorConveniences.toGenerator

/**
 * Expression generators for Standard ML.
 * Based on the SML '97 Definition expression grammar.
 */
object SmlExpressions {
    /** Special constant expression */
    val constExpr: Generator = SmlLexical.constant

    /** Variable expression (possibly long identifier) */
    val varExpr: Generator = SmlLexical.longValueId

    /** op prefixed identifier (for using infixes as values) */
    val opExpr: Generator = "op" F SmlLexical.reqWs F SmlLexical.infixOp

    /** Unit expression () */
    val unitExpr: Generator = "()".toGenerator()

    /** Simple atomic expression (non-recursive) */
    private val simpleAtomicExpr: Generator = weightedChoice(
        0.35 to constExpr,
        0.35 to varExpr,
        0.15 to SmlLexical.builtinValueId,
        0.1 to unitExpr,
        0.05 to opExpr
    )

    //COMPOUND EXPRESSIONS
    /** Record expression row: label = exp */
    private val expRow: Generator = lazy {
        SmlLexical.label F SmlLexical.optWs F "=" F SmlLexical.optWs F expression
    }

    /** Record expression { lab1 = exp1, lab2 = exp2, ... } */
    private val recordExpr: Generator = lazy {
        (SmlLexical.optWs F sepBy1(expRow, "," F SmlLexical.optWs) F SmlLexical.optWs).wrapBraces()
    }

    /** Record selector #label */
    val selectorExpr: Generator = "#" F SmlLexical.label

    /** Tuple expression (exp1, exp2, ...) - at least 2 elements */
    private val tupleExpr: Generator = lazy {
        (SmlLexical.optWs F expression F "," F SmlLexical.optWs F
                sepBy1(expression, "," F SmlLexical.optWs) F SmlLexical.optWs).wrapParens()
    }

    /** List expression [exp1, exp2, ...] */
    private val listExpr: Generator = lazy {
        (SmlLexical.optWs F sepBy(expression, "," F SmlLexical.optWs) F SmlLexical.optWs).wrapBrackets()
    }

    /** Sequence expression (exp1; exp2; ...; expn) */
    private val seqExpr: Generator = lazy {
        (SmlLexical.optWs F expression F ";" F SmlLexical.optWs F
                sepBy1(expression, ";" F SmlLexical.optWs) F SmlLexical.optWs).wrapParens()
    }

    /** Bracketed expression */
    private val parenExpr: Generator = lazy {
        (SmlLexical.optWs F expression F SmlLexical.optWs).wrapParens()
    }

    //CONTROL FLOW EXPRESSIONS
    /** If-then-else expression */
    private val ifExpr: Generator = lazy {
        "if" F SmlLexical.reqWs F expression F SmlLexical.reqWs F "then" F SmlLexical.reqWs F expression F
                SmlLexical.reqWs F "else" F SmlLexical.reqWs F expression
    }

    /** While loop expression */
    private val whileExpr: Generator = lazy {
        "while" F SmlLexical.reqWs F expression F SmlLexical.reqWs F "do" F SmlLexical.reqWs F expression
    }

    /** Case expression match rule: pat => exp */
    private val matchRule: Generator = lazy {
        SmlPatterns.pattern F SmlLexical.optWs F "=>" F SmlLexical.optWs F expression
    }

    /** Case expression */
    private val caseExpr: Generator = lazy {
        "case" F SmlLexical.reqWs F expression F SmlLexical.reqWs F "of" F SmlLexical.reqWs F
                sepBy1(matchRule, SmlLexical.optWs F literal("|") F SmlLexical.optWs)
    }

    /** Anonymous function (fn) expression */
    private val fnExpr: Generator = lazy {
        "fn" F SmlLexical.reqWs F sepBy1(matchRule, SmlLexical.optWs F literal("|") F SmlLexical.optWs)
    }

    //OPERATOR EXPRESSIONS
    /** Infix operator expression */
    private val infixExpr: Generator = lazy {
        atomicExpression F SmlLexical.reqWs F SmlLexical.infixOp F SmlLexical.reqWs F expression
    }

    /** Cons expression exp1 :: exp2 */
    private val consExpr: Generator = lazy {
        atomicExpression F SmlLexical.optWs F "::" F SmlLexical.optWs F expression
    }

    /** Andalso expression */
    private val andalsoExpr: Generator = lazy {
        atomicExpression F SmlLexical.reqWs F "andalso" F SmlLexical.reqWs F expression
    }

    /** Orelse expression */
    private val orelseExpr: Generator = lazy {
        atomicExpression F SmlLexical.reqWs F "orelse" F SmlLexical.reqWs F expression
    }

    //APPLICATION EXPRESSIONS
    /** Function application (juxtaposition) */
    private val appExpr: Generator = lazy {
        atomicExpression F SmlLexical.reqWs F atomicExpression
    }

    /** Multiple argument application */
    private val multiAppExpr: Generator = lazy {
        atomicExpression F many1(SmlLexical.reqWs F atomicExpression)
    }

    //TYPE AND EXCEPTION EXPRESSIONS
    /** Typed expression: exp : ty */
    private val typedExpr: Generator = lazy {
        atomicExpression F SmlLexical.optWs F ":" F SmlLexical.optWs F SmlTypes.type
    }

    /** Raise expression */
    private val raiseExpr: Generator = lazy {
        "raise" F SmlLexical.reqWs F expression
    }

    /** Handle expression */
    private val handleExpr: Generator = lazy {
        atomicExpression F SmlLexical.reqWs F "handle" F SmlLexical.reqWs F
                sepBy1(matchRule, SmlLexical.optWs F "|" F SmlLexical.optWs)
    }

    //LET EXPRESSION
    /** Let expression (uses declarations from SmlDeclarations) */
    val letExpr: Generator = lazy {
        "let" F SmlLexical.reqWs F SmlDeclarations.declarations F SmlLexical.reqWs F "in" F SmlLexical.reqWs F
                sepBy1(expression, SmlLexical.optWs F ";" F SmlLexical.optWs) F SmlLexical.reqWs F "end"
    }

    //COMBINED EXPRESSIONS
    /** Atomic expression (for use in applications and operators) */
    val atomicExpression: Generator = depthChoice(
        terminals = listOf(simpleAtomicExpr),
        recursives = listOf(
            weightedChoice(
                0.4 to simpleAtomicExpr,
                0.15 to recordExpr,
                0.15 to tupleExpr,
                0.15 to listExpr,
                0.1 to parenExpr,
                0.05 to selectorExpr
            )
        )
    )

    /** Full expression */
    val expression: Generator = depthChoice(
        terminals = listOf(simpleAtomicExpr),
        recursives = listOf(
            weightedChoice(
                0.18 to simpleAtomicExpr,
                0.11 to appExpr,
                0.10 to infixExpr,
                0.10 to ifExpr,
                0.08 to fnExpr,
                0.08 to letExpr,
                0.07 to caseExpr,
                0.05 to tupleExpr,
                0.05 to listExpr,
                0.04 to consExpr,
                0.03 to andalsoExpr,
                0.03 to orelseExpr,
                0.02 to typedExpr,
                0.02 to handleExpr,
                0.02 to raiseExpr,
                0.02 to whileExpr
            )
        )
    )

    /** Simple expression */
    val simpleExpression: Generator = depthChoice(
        terminals = listOf(simpleAtomicExpr),
        recursives = listOf(
            weightedChoice(
                0.4 to simpleAtomicExpr,
                0.2 to appExpr,
                0.15 to infixExpr,
                0.1 to tupleExpr,
                0.1 to listExpr,
                0.05 to fnExpr
            )
        )
    )
}
