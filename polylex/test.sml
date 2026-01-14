(* test.sml - Quick test of the lexer (runs interactively) *)
(* Run with: poly < test.sml *)

val () = print "=== Loading Lexer ===\n";

use "original/Misc.ML";
use "original/HashTable.ML";
use "original/SymbolsSig.sml";
use "original/Symbols.ML";
use "stubs/PRETTY.sig";
use "stubs/Pretty.sml";
use "original/DEBUG.sig";
use "original/Debug.ML";
use "original/LEXSIG.sml";
use "original/LEX_.ML";
use "original/Lex.ML";

val () = print "=== Lexer Loaded ===\n\n";

(* Simple tokenise function for testing *)
fun tokenise (input: string) : (Symbols.sys * string) list =
let
    val pos = ref 0
    val len = String.size input
    
    fun nextChar () : char option =
        if !pos >= len then NONE
        else let val c = String.sub(input, !pos)
             in pos := !pos + 1; SOME c end
    
    fun errorHandler {location, hard, message, context} =
        print ("Error: " ^ Pretty.uglyPrint message ^ "\n")
    
    val params = [
        Universal.tagInject Lex.errorMessageProcTag errorHandler,
        Universal.tagInject Debug.lineNumberTag (fn () => 1),
        Universal.tagInject Debug.offsetTag (fn () => FixedInt.fromInt (!pos)),
        Universal.tagInject Debug.fileNameTag "test",
        Universal.tagInject Debug.bindingCounterTag (fn () => 0)
    ]
    
    val lex = Lex.initial(nextChar, params)
    
    fun collect acc =
        let
            val () = Lex.insymbol lex
            val tok = Lex.sy lex
            val text = Lex.id lex
        in
            if tok = Symbols.AbortParse then List.rev acc
            else collect ((tok, text) :: acc)
        end
        handle _ => List.rev acc
in
    collect []
end;

(* Pretty print tokens *)
fun showTokens input =
let
    val tokens = tokenise input
    fun show (sym, text) = 
        print ("  " ^ Symbols.repr sym ^ 
               (if text = "" then "" else " = \"" ^ String.toString text ^ "\"") ^ 
               "\n")
in
    print ("Input: \"" ^ String.toString input ^ "\"\n");
    print "Tokens:\n";
    List.app show tokens;
    print "\n"
end;

(* Run test cases *)
val () = print "=== Running Tests ===\n\n";

val () = showTokens "val x = 42";
val () = showTokens "fun f x = x + 1";
val () = showTokens "\"hello world\"";
val () = showTokens "0wx1F";
val () = showTokens "(* comment *) val y = 3.14";
val () = showTokens "structure Foo = struct end";
val () = showTokens "'a -> 'b";
val () = showTokens "#\"c\"";

val () = print "=== Tests Complete ===\n";
