# PolyML Lexer (Lifted from the compiler)

A standalone lexer using the **original PolyML lexer code**, designed for 
differential testing against a verified oracle.

## Quick Start
### 1. Install PolyML
The lexer wrapper is written in Standard ML and needs to be compiled with PolyML itself. That guarantees that the lexer files are compiled exactly as in the main compiler and increases confidence in any testing effort.

### 2. Build lexer Executable

```bash
polyc build.sml
cc -o polylex polylex.o -lpolymain -lpolyml
```
In case the linker fails, it may be because the PolyML libraries are in some non-standard location. Find them with:

```bash
find /usr -name "libpolyml*" 2>/dev/null
```

and then link with the explicit path:
```bash
cc -o polylex polylex.o -L/<that path> -Wl,-rpath,<that path> -lpolymain -lpolyml
```

### 4. Using:
You can directly input a string and the output will go to stdout:

```bash
echo 'val x = 42' | ./polylex
```

Alternatively, you can also use an input file (output goes to stdout):

```bash
./polylex input.sml
```

Or, you can use both an input and output file (reads from input.sml and writes to output.txt):

```bash
./polylex input.sml output.txt
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
│   ├── LEX_.ML         # THE LEXER
│   └── Lex.ML          # Functor instantiation
├── stubs/              # Simplified replacements for PolyML internals
│   ├── PRETTY.sig      # Pretty printing signature
│   └── Pretty.sml      # Pretty printing (simplified)
├── Main.sml            # Wrapper to call the lexer on inputs
├── build.sml           # Build script
└── README.md
```

## Why Stubs?

The original `Pretty.sml` uses `Address` and `RunCall` which are low-level
PolyML primitives for inspecting runtime representations. These aren't needed
for lexing - Pretty is only used for error message formatting. The stub 
provides the same interface but with simple datatypes.


## License
- Original PolyML files: LGPL 2.1 (see PolyML repository)
