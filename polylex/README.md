# PolyML Lexer Harness for Fuzzing

A standalone lexer harness using the **original PolyML lexer code**, designed for 
differential testing against your Sulzmann-Lu derivative oracle.

## Quick Start

### 1. Copy Original PolyML Files

First, copy the required files from the PolyML repository into `original/`:

```bash
# Clone PolyML if you haven't
git clone https://github.com/polyml/polyml.git

# Copy required files
mkdir -p original
cp polyml/mlsource/MLCompiler/Misc.ML           original/
cp polyml/mlsource/MLCompiler/HashTable.ML      original/
cp polyml/mlsource/MLCompiler/SymbolsSig.sml    original/
cp polyml/mlsource/MLCompiler/Symbols.ML        original/
cp polyml/mlsource/MLCompiler/DEBUG.sig         original/
cp polyml/mlsource/MLCompiler/Debug.ML          original/
cp polyml/mlsource/MLCompiler/LEXSIG.sml        original/
cp polyml/mlsource/MLCompiler/LEX_.ML           original/
cp polyml/mlsource/MLCompiler/Lex.ML            original/
```

### 2. Test Interactively

```bash
poly < test.sml
```

### 3. Build Standalone Executable

```bash
poly < build.sml
cc -o sml-lexer sml-lexer.o -lpolymain -lpolyml
```

### 4. Use

```bash
echo 'val x = 42' | ./sml-lexer
```

## Directory Structure

```
polyml-lexer-harness/
├── original/           # Untouched files from PolyML (you copy these)
│   ├── Misc.ML         # Exception definitions
│   ├── HashTable.ML    # Hash table for keyword lookup
│   ├── SymbolsSig.sml  # Token type signature
│   ├── Symbols.ML      # Token definitions
│   ├── DEBUG.sig       # Debug parameters signature
│   ├── Debug.ML        # Debug parameters
│   ├── LEXSIG.sml      # Lexer signature
│   ├── LEX_.ML         # THE LEXER (this is what you're testing!)
│   └── Lex.ML          # Functor instantiation
├── stubs/              # Simplified replacements for PolyML internals
│   ├── PRETTY.sig      # Pretty printing signature
│   └── Pretty.sml      # Pretty printing (simplified)
├── Main.sml            # Harness: stdin → tokens → stdout
├── build.sml           # Build script
├── test.sml            # Interactive test script
└── README.md
```

## Why Stubs?

The original `Pretty.sml` uses `Address` and `RunCall` which are low-level
PolyML primitives for inspecting runtime representations. These aren't needed
for lexing - Pretty is only used for error message formatting. The stub 
provides the same interface but with simple datatypes.

## Output Format

Tokens are output one per line:

```
IDENT("x")           # Identifier
TYVAR("'a")          # Type variable  
STRING("hello")      # String constant
INT(42)              # Integer
REAL(3.14)           # Real
WORD(0wx1F)          # Word constant
CHAR("c")            # Character
val                  # Keywords
fun
let
...
(                    # Punctuation
)
[
]
=>
->
...
```

## Fuzzing with AFL++

### Option A: AFL++ with QEMU mode (no recompilation)

```bash
# Build normally
poly < build.sml
cc -o sml-lexer sml-lexer.o -lpolymain -lpolyml

# Fuzz with QEMU instrumentation
mkdir -p corpus findings
echo 'val x = 1' > corpus/seed.sml
afl-fuzz -Q -i corpus -o findings -- ./sml-lexer
```

### Option B: Instrument PolyML itself (more complex)

This requires recompiling PolyML with AFL++ instrumentation, which is beyond
the scope of this harness.

## Differential Testing Workflow

```bash
# For each input:
INPUT="val x = 42"

# Get PolyML tokens
echo "$INPUT" | ./sml-lexer > poly_tokens.txt

# Get your oracle tokens
echo "$INPUT" | ./your-verilex-oracle > oracle_tokens.txt

# Compare
diff poly_tokens.txt oracle_tokens.txt && echo "MATCH" || echo "DIVERGENCE"
```

## Troubleshooting

### "Cannot find Misc" or similar
Make sure you copied all files to `original/` directory.

### "Undefined structure Universal"
`Universal` is part of PolyML's basis library. It should be available 
automatically when running under PolyML.

### Link errors
You may need to specify the PolyML library path:
```bash
cc -o sml-lexer sml-lexer.o -L/usr/local/lib -lpolymain -lpolyml
```

## License

- Original PolyML files: LGPL 2.1 (see PolyML repository)
- Stubs and harness: Provided for your project use
