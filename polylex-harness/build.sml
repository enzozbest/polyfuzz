(* build.sml - Build script for the lexer harness *)
(* Run with: poly < build.sml *)
(* Or for export: poly --script build.sml *)

val () = print "=== Building PolyML Lexer Harness ===\n\n";

(* Load original PolyML files in dependency order *)
val () = print "Loading originals/Misc.ML...\n";
use "originals/Misc.ML";

val () = print "Loading originals/HashTable.ML...\n";
use "originals/HashTable.ML";

val () = print "Loading originals/SymbolsSig.sml...\n";
use "originals/SymbolsSig.sml";

val () = print "Loading originals/Symbols.ML...\n";
use "originals/Symbols.ML";

(* Load stub Pretty (instead of original which uses Address/RunCall) *)
val () = print "Loading stubs/PRETTY.sig...\n";
use "stubs/PRETTY.sig";

val () = print "Loading stubs/Pretty.sml...\n";
use "stubs/Pretty.sml";

(* Continue with original files *)
val () = print "Loading originals/DEBUG.sig...\n";
use "originals/DEBUG.sig";

val () = print "Loading originals/Debug.ML...\n";
use "originals/Debug.ML";

val () = print "Loading originals/LEXSIG.sml...\n";
use "originals/LEXSIG.sml";

val () = print "Loading originals/LEX_.ML...\n";
use "originals/LEX_.ML";

val () = print "Loading originals/Lex.ML...\n";
use "originals/Lex.ML";

val () = print "\n=== All modules loaded successfully! ===\n\n";

(* Load the harness *)
val () = print "Loading Main.sml...\n";
use "Main.sml";

(* Export to standalone executable *)
val () = print "Exporting executable as 'sml-lexer'...\n";
PolyML.export("polylex", Main.main);

val () = print "\nExport complete!\n";
val () = print "Link with: cc -o polylex polylex.o -L/usr/local/lib -Wl,-rpath,/usr/local/lib -lpolymain -lpolyml\n";