Verilex — a tiny Kotlin lexer built with regex derivatives
==========================================================

Updated on: January 17, 2026

Overview
--------
Verilex is an experimental, minimal lexer written in Kotlin. It builds on the
idea of regular-expression derivatives to scan an input string and return a
list of tagged tokens. This algorithm was first introduced by Sulzmann and Lu (2014), 
and later formally proved correct by Urban (2016). That means the algorithm always produces 
the correct sequence of tokens for a given input stream and regular expressions.

"Correct" in this context means POSIX disambiguation: the lexer always prefers the longest match (maximal munch), 
and, if there are multiple equal-length matches, it chooses the rule declared first (rule priority).

Instead of writing a traditional hand‑rolled lexer, you compose regular expressions 
with a small DSL and “tag” the parts you want to capture. The engine walks the input one character at a time, 
computing derivatives and carrying a structured value that is finally turned into a list of `(tag, lexeme)` pairs.

Key ideas
---------
- Regular expressions are expressed as Kotlin data types (see `rexp` package).
- A light DSL makes composition ergonomic (`F` for sequencing read as "followed by", `X` for choice read as "or",
  `S()` for `*`, `P()` for `+`, and `T` for labelling parts of the regex).
- The lexer core (`Verilex.lex`) computes derivatives and injects characters into a structured value, 
  which is flattened into a token stream at the end.

Quick example
-------------
The snippet below shows how to tokenize a toy language consisting of identifiers
and a few operators. Tags become token names, and the corresponding lexemes are
returned as pairs.

```kotlin

fun main() {
    // Operators: + - * /
    val op = "op" T ("+" X "-" X "*" X "/")

    // Identifier: one or more lowercase letters
    val letters = RANGE(('a'..'z').toSet())
    val id = "id" T (letters F letters.S())

    // A token is either an operator or an identifier; allow any number of them in a row
    val lexerRegex = (op X id).S().toCharFunctionFormat() // compiles chars/classes to internal predicates

    val input = "sum+a-b/c"
    val tokens: List<Pair<String, String>> = Verilex.lex(lexerRegex, input)
    println(tokens)
    // Example output (format may vary): [(id, sum), (op, +), (id, a), (op, -), (id, b), (op, /), (id, c)]
}
```

How it works
----------------------------
- `RegularExpression` is a sealed hierarchy with constructors like `ALT`,
  `SEQ`, `STAR`, `PLUS`, and character matchers. The helper functions in
  `RegexConveniences.kt` provide a tiny DSL:
  - `a F b` — sequence (concatenation)
  - `a X b` — choice (`|`)
  - `r.S()` — Kleene star (`r*`)
  - `r.P()` — one or more (`r+`)
  - `"name" T r` — record/tag the sub‑expression so its matched text is
    visible in the final token list
- `toCharFunctionFormat()` rewrites character classes into a predicate form
  (`CFUN`) so derivatives can be computed per input character more easily.
- `Verilex.lex(regex, input)` walks the input, computing derivatives and
  using `Injector` to “inject” each consumed character back into a value
  tree. At the end it calls `env()` on that value to produce
  `List<Pair<String,String>>` where the first element is the tag (token kind)
  and the second is the lexeme.

Notes and limitations
---------------------
- Error handling is minimal. If the provided regex cannot match the full input,
  an exception may be thrown deep in the derivative/injection pipeline.