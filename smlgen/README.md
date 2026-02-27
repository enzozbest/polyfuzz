# SMLGen

A Standard ML program generator written in Kotlin. SMLGen produces syntactically valid SML '97 programs at configurable
complexity levels, designed for building test corpora for compiler and interpreter testing, fuzz testing, and
property-based testing.

Part of the [PolyFuzz](https://github.com/enzozbest/polyfuzz) project.

## Overview

SMLGen uses a compositional generator framework where complex SML programs are constructed from smaller generator
combinators that mirror the SML grammar. Generators can be combined using sequencing (`F`), choice (`X`), weighted
selection, depth-controlled recursion, and other standard combinators. The framework maintains grammar constraints while
exploring the breadth of SML's syntax, covering expressions, declarations, types, patterns, functions, exceptions, and
edge cases.

Programs are generated at five complexity levels, each controlling recursion depth and repetition:

| Level     | Max Depth | Max Repeat | Description                                   |
|-----------|-----------|------------|-----------------------------------------------|
| `MINIMAL` | 2         | 1          | Single declarations, simple expressions       |
| `SIMPLE`  | 3         | 2          | Few declarations, basic expressions           |
| `MEDIUM`  | 5         | 3          | Moderate nesting, variety of constructs       |
| `COMPLEX` | 7         | 4          | Deep nesting, advanced features               |
| `EXTREME` | 10        | 5          | Maximum nesting, edge cases, obscure features |

A `MIXED` mode cycles through all five levels, useful for generating diverse test corpora.

## Requirements

- JDK 25+
- Gradle 8+ (wrapper included)

## Building

```bash
./gradlew build
```

To build a JAR:

```bash
./gradlew jar
# Output: build/libs/smlgen-1.0-SNAPSHOT.jar
```

## Usage

```
smlgen [-n <count>] [-seed <seed>] [-o <dir>] [-c <complexity>]

Options:
  -n <count>       Number of test files to generate (default: 10)
  -seed <seed>     Seed for reproducible generation (random if omitted)
  -o <dir>         Output directory (default: current directory)
  -c <complexity>  Complexity level (default: MIXED)
                   Values: MINIMAL, SIMPLE, MEDIUM, COMPLEX, EXTREME, MIXED
  -h, --help       Show this help message
```

### Examples

```bash
# Generate 10 programs at mixed complexity (default)
./gradlew run

# Generate 20 complex programs into a directory
./gradlew run --args="-n 20 -c COMPLEX -o ./test_corpus"

# Generate 50 programs with a fixed seed for reproducibility
./gradlew run --args="-n 50 -seed 12345 -o ./corpus"

# Generate 100 extreme programs with edge cases
./gradlew run --args="-n 100 -c EXTREME -o ./edge_cases"
```

Output files are named `Program_<index>_<length>_<seed>.sml`.

## Project Structure

```
src/main/kotlin/bestetti/enzo/smlgen/
├── gen/                        # Generic generator framework
│   ├── Generator.kt            # Core Generator interface and DSL
│   ├── GenerationContext.kt    # Generation state (depth, randomness, config)
│   ├── GeneratorCombinators.kt # seq, choice, weightedChoice, depthChoice, many, sepBy, ...
│   ├── GeneratorConveniences.kt# Helper functions and wrapping utilities
│   └── AtomicGenerators.kt    # Literal and string generators
├── sml/
│   ├── grammar/                # SML '97 grammar elements
│   │   ├── SmlLexical.kt      # Identifiers, constants, comments, reserved words
│   │   ├── SmlExpressions.kt  # Expressions (if, case, let, fn, raise, handle, ...)
│   │   ├── SmlTypes.kt        # Type expressions (variables, constructors, records, functions)
│   │   ├── SmlPatterns.kt     # Patterns (wildcards, constructors, records, as-patterns)
│   │   ├── SmlDeclarations.kt # Declarations (val, fun, type, datatype, exception, infix)
│   │   └── SmlEdgeCase.kt     # Obscure/unusual SML features
│   └── generator/              # High-level program generation
│       ├── SmlProgramGenerator.kt  # Main generator with length enforcement and fallback
│       ├── StructureGenerators.kt  # Program templates per complexity level
│       ├── ProgramComplexity.kt    # Complexity enum and config mapping
│       └── ProgramConfig.kt       # Generation configuration
├── Main.kt                     # CLI entry point
└── CliArgs.kt                  # Argument parsing
```

## Testing

```bash
./gradlew test
```

Test coverage reports (JaCoCo) are generated automatically after tests run:

```bash
# HTML report
open build/reports/jacoco/test/html/index.html
```

## Generator Framework

The generator framework is generic and reusable. At its core, a `Generator` is a function
`(GenerationContext) -> String` with combinator operators:

```kotlin
// Sequence: generate A followed by B
val ab = genA F genB

// Choice: randomly pick A or B
val aOrB = genA X genB

// Other combinators
weightedChoice(0.7 to genA, 0.3 to genB)  // Weighted random choice
depthChoice(terminal, recursive)            // Depth-aware recursion control
many(gen)                                   // Zero or more repetitions
many1(gen)                                  // One or more repetitions
optional(gen)                               // Zero or one occurrence
sepBy(gen, sep)                             // Separated list
lazy { recursiveGen }                       // Deferred evaluation for recursion
```

The `GenerationContext` tracks recursion depth and provides a seeded `Random` instance for reproducible output.

