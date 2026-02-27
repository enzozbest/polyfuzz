# PolyLex - Instrumented for AFL Fuzzing

A standalone lexer using the **original PolyML lexer code**, designed for 
differential testing against a verified oracle. This repository provides the instrumented version of polylex for use with AFL 

## Quick Start
### 1. Install PolyML
The lexer wrapper is written in Standard ML and needs to be compiled with PolyML itself. That guarantees that the lexer files are compiled exactly as in the main compiler and increases confidence in any testing effort.

### 2. Making the binaries
You can compile everything and be ready to go with:

```bash
make
```

You can also compile individual components:
```bash
make lexer
```
will build polylex

```bash
make shim
```
will build the C shim used to instrument polylex

```bash
make link
```
will link the object files into the executable polylex_fuzz.

### 4. Using:
You can use polylex_fuzz directly as with polylex. For example, input a string and the output will go to stdout:

```bash
echo 'val x = 42' | ./polylex_fuzz
```

Alternatively, you can also use an input file (output goes to stdout):

```bash
./polylex_fuzz input.sml
```

Or, you can use both an input and output file (reads from input.sml and writes to output.txt):

```bash
./polylex_fuzz input.sml output.txt
```

The main use case expected, however, is to use this with AFL. For that, you can use the following command:

```bash
afl-fuzz -i <input_corpus> -o <output_corpus> -- ./polylex_fuzz @
```

## Directory Structure

```
polylex/
├── originals/          # Files from PolyML
│   ├── Misc.ML         # Exception definitions
│   ├── HashTable.ML    # Hash table for keyword lookup
│   ├── SymbolsSig.sml  # Token type signature
│   ├── Symbols.ML      # Token definitions (+ custom export format. only modification from the original compiler)
│   ├── DEBUG.sig       # Debug parameters signature
│   ├── Debug.ML        # Debug parameters
│   ├── LEXSIG.sml      # Lexer signature
│   ├── LEX_.ML         # THE ORIGINAL LEXER
│   └── Lex.ML          # Functor instantiation
├── stubs/              # Simplified replacements for PolyML internals
│   ├── PRETTY.sig      # Pretty printing signature
│   └── Pretty.sml      # Pretty printing (simplified)
├── Main.sml            # Wrapper to call the lexer on inputs
├── build.sml           # Build script
├── polylex_c_shim.c    # C shim for AFL fuzzing
├── LEX_.ML             # The instrumented lexer
├── Makefile            # For make
└── README.md

```

## Why Stubs?

The original `Pretty.sml` uses `Address` and `RunCall` which are low-level
PolyML primitives for inspecting runtime representations. These aren't needed
for lexing - Pretty is only used for error message formatting. The stub 
provides the same interface but with simple datatypes.


## License
- Original PolyML files: LGPL 2.1 (see PolyML repository)
