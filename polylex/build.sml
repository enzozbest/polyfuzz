(* build.sml - Build script for the lexer harness *)
(* Run with: poly < build.sml *)
(* Or for export: poly --script build.sml *)

val () = print "=== Building PolyML Lexer Harness ===\n\n";

(* Load original PolyML files in dependency order *)
val () = print "Loading original/Misc.ML...\n";
use "original/Misc.ML";

val () = print "Loading original/HashTable.ML...\n";
use "original/HashTable.ML";

val () = print "Loading original/SymbolsSig.sml...\n";
use "original/SymbolsSig.sml";

val () = print "Loading original/Symbols.ML...\n";
use "original/Symbols.ML";

(* Load stub Pretty (instead of original which uses Address/RunCall) *)
val () = print "Loading stubs/PRETTY.sig...\n";
use "stubs/PRETTY.sig";

val () = print "Loading stubs/Pretty.sml...\n";
use "stubs/Pretty.sml";

(* Continue with original files *)
val () = print "Loading original/DEBUG.sig...\n";
use "original/DEBUG.sig";

val () = print "Loading original/Debug.ML...\n";
use "original/Debug.ML";

val () = print "Loading original/LEXSIG.sml...\n";
use "original/LEXSIG.sml";

val () = print "Loading original/LEX_.ML...\n";
use "original/LEX_.ML";

val () = print "Loading original/Lex.ML...\n";
use "original/Lex.ML";

val () = print "\n=== All modules loaded successfully! ===\n\n";

(* Load the harness *)
val () = print "Loading Main.sml...\n";
use "Main.sml";

(* Export to standalone executable *)
val () = print "Exporting executable as 'sml-lexer'...\n";
PolyML.export("polylex", Main.main);

val () = print "\nExport complete!\n";
val () = print "Link with: cc -o polylex polylex.o -L/usr/local/lib -Wl,-rpath,/usr/local/lib -lpolymain -lpolyml\n";