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


(* AFL coverage trace *)
val aflTrace : int -> unit =
    let val f = ref NONE
    in fn id =>
        case !f of
            SOME func => func id
        |   NONE =>
            let val func = Foreign.buildCall1
                    (Foreign.externalFunctionSymbol "trace",
                     Foreign.cInt, Foreign.cVoid)
            in f := SOME func; func id end
            handle _ => ()
    end;

val () = print "Loading LEX_.ML...\n";
use "LEX_.ML";

val () = print "Loading originals/Lex.ML...\n";
use "originals/Lex.ML";

val () = print "\n=== All modules loaded successfully! ===\n\n";

(* Load the harness *)
val () = print "Loading Main.sml...\n";
use "Main.sml";

(* Export *)
val () = print "Exporting executable as 'polylex_fuzz'...\n";
PolyML.export("polylex_fuzz", Main.main);

val () = print "\nExport complete!\n";
val () = print "Now compile and link:\n";
val () = print "  afl-clang-fast -c polylex_c_shim.c -o polylex_c_shim.o\n";
val () = print "  afl-clang-fast -o polylex_fuzz polylex_fuzz.o polylex_c_shim.o \\\n";
val () = print "      -L/usr/local/lib -Wl,-rpath,/usr/local/lib -lpolymain -lpolyml\n";
