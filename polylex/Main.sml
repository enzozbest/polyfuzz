(* Main.sml - Lexer harness for fuzzing *)
(* Reads input from stdin, tokenises it, outputs tokens to stdout or file*)

structure Main =
struct
    (* Create lexer parameters with a simple error handler *)
    fun makeParams (posRef: int ref, filename: string) : Universal.universal list =
    let
        (* Error handler - output to stderr *)
        fun errorHandler {location: Lex.location, hard, message, context} =
            TextIO.output(TextIO.stdErr, 
                "Lex " ^ (if hard then "error" else "warning") ^ 
                " at " ^ #file location ^ ":" ^ 
                FixedInt.toString (#startLine location) ^ ":" ^
                FixedInt.toString (#startPosition location) ^ ": " ^
                Pretty.uglyPrint message ^ "\n")
        
        val lineNo = ref (1: FixedInt.int)
        val bindingCount = ref (0: FixedInt.int)
    in
        [
            Universal.tagInject Lex.errorMessageProcTag errorHandler,
            Universal.tagInject Debug.lineNumberTag (fn () => !lineNo),
            Universal.tagInject Debug.offsetTag (fn () => FixedInt.fromInt (!posRef)),
            Universal.tagInject Debug.fileNameTag filename,
            Universal.tagInject Debug.bindingCounterTag 
                (fn () => (bindingCount := !bindingCount + 1; !bindingCount))
        ]
    end

    (* Tokenise a string and return list of (token, text) pairs *)
    fun tokenise (input: string) : (Symbols.sys * string) list =
    let
        val pos = ref 0
        val len = String.size input
        
        (* Character stream function *)
        fun nextChar () : char option =
            if !pos >= len then NONE
            else 
                let val c = String.sub(input, !pos)
                in pos := !pos + 1; SOME c
                end
        
        val params = makeParams(pos, "<stdin>")
        val lex = Lex.initial(nextChar, params)
        
        (* Collect all tokens until EOF *)
        fun collect (acc: (Symbols.sys * string) list) : (Symbols.sys * string) list =
            let
                val () = Lex.insymbol lex
                val tok = Lex.sy lex
                val text = Lex.id lex
            in
                if tok = Symbols.AbortParse then
                    List.rev acc
                else
                    collect ((tok, text) :: acc)
            end
            handle Misc.InternalError msg => 
                    (TextIO.output(TextIO.stdErr, "Internal error: " ^ msg ^ "\n");
                     List.rev acc)
                 | _ => List.rev acc
    in
        collect []
    end

    (* Main entry point *)
    fun main () =
    let
        (* Read all input from stdin *)
        val input = TextIO.inputAll TextIO.stdIn
        
        (* Tokenise *)
        val tokens = tokenise input
        
        (* Output each token on its own line *)
        fun outputToken (sym, text) =
            print (Symbols.tokenToString(sym, text) ^ "\n")
    in
        List.app outputToken tokens;
        OS.Process.exit OS.Process.success
    end
    handle e => (
        TextIO.output(TextIO.stdErr, "Fatal: " ^ exnMessage e ^ "\n");
        OS.Process.exit OS.Process.failure
    )
end;
(* Entry point - called by exported executable or manually *)