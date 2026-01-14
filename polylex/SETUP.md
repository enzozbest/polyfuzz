# PolyML Lexer Harness - Setup Guide

## Files to Copy from PolyML Source (UNTOUCHED)

Copy these files from the PolyML repository (https://github.com/polyml/polyml)
into the `original/` directory:

```bash
# From mlsource/MLCompiler/
cp mlsource/MLCompiler/Misc.ML           original/
cp mlsource/MLCompiler/HashTable.ML      original/
cp mlsource/MLCompiler/SymbolsSig.sml    original/
cp mlsource/MLCompiler/Symbols.ML        original/
cp mlsource/MLCompiler/DEBUG.sig         original/
cp mlsource/MLCompiler/Debug.ML          original/
cp mlsource/MLCompiler/LEXSIG.sml        original/
cp mlsource/MLCompiler/LEX_.ML           original/
cp mlsource/MLCompiler/Lex.ML            original/
```

## Files That Need Stubs (provided in stubs/)

The following files use PolyML-specific internals (Address, RunCall) that 
won't work standalone. We provide simplified stubs:

- `PRETTY.sig` - signature (stub version)
- `Pretty.sml` - implementation (stub version)

## Directory Structure

```
polyml-lexer-harness/
├── original/           # Untouched files from PolyML
│   ├── Misc.ML
│   ├── HashTable.ML
│   ├── SymbolsSig.sml
│   ├── Symbols.ML
│   ├── DEBUG.sig
│   ├── Debug.ML
│   ├── LEXSIG.sml
│   ├── LEX_.ML
│   └── Lex.ML
├── stubs/              # Simplified stubs for PolyML-specific code
│   ├── PRETTY.sig
│   └── Pretty.sml
├── Main.sml            # Harness (stdin → tokens → stdout)
├── build.sml           # Build script
└── README.md
```

## Build Order

Files must be loaded in this order:

1. original/Misc.ML
2. original/HashTable.ML
3. original/SymbolsSig.sml
4. original/Symbols.ML
5. stubs/PRETTY.sig
6. stubs/Pretty.sml
7. original/DEBUG.sig
8. original/Debug.ML
9. original/LEXSIG.sml
10. original/LEX_.ML
11. original/Lex.ML
12. Main.sml
