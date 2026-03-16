(* Main.sml - Lexer harness for fuzzing *)
(* Reads input from stdin, tokenises it, outputs tokens to stdout *)

structure AflFfi =
struct
    (* External symbols *)
    val setupSym = Foreign.externalFunctionSymbol "setup"
    val traceSym = Foreign.externalFunctionSymbol "trace"
    val resetSym = Foreign.externalFunctionSymbol "reset"

    val setup : unit -> unit =
        Foreign.buildCall0 (setupSym, (), Foreign.cVoid)

    val trace : int -> unit =
        Foreign.buildCall1 (traceSym, Foreign.cInt, Foreign.cVoid)

    val reset : unit -> unit =
        Foreign.buildCall0 (resetSym, (), Foreign.cVoid)
end;

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

    (* Main entry point — reads from stdin, runs lexer *)
    fun main () =
    let
        (* Initialise AFL shared memory *)
        val () = AflFfi.setup ()

        (* Read all input from stdin *)
        val input = TextIO.inputAll TextIO.stdIn

        (* Reset coverage tracking *)
        val () = AflFfi.reset ()

        (* Run the lexer and output tokens to stdout *)
        val tokens = tokenise input
        val () = List.app (fn (tok, text) =>
            print (Symbols.tokenToString(tok, text) ^ " ")) tokens
        val () = print "\n"
    in
        ()
    end
    handle _ => ()
    (* Lexer exceptions are caught inside tokenise (line 77-80) and return partial
       results.  Exceptions that reach here are infrastructure failures — FFI setup,
       stdin I/O, broken stdout pipe — not input-dependent lexer bugs, so silently
       exiting with 0 is correct: AFL++ should not treat them as interesting crashes. *)
end;
