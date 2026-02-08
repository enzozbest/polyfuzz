(* LEX_.ML instrumented for fuzzing
    This file is a copy of LEX_.ML from the original PolyML repository with tracing code added for AFL++ fuzzing.
    Please refer to LEX__ORIGINAL.ML to see the original implementation and accreditations.
*)

functor LEX_ (
structure PRETTY: PRETTY
structure SYMBOLS : SymbolsSig
structure DEBUG: DEBUG

) : LEXSIG =

(*****************************************************************************)
(*                  LEX functor body                                         *)
(*****************************************************************************)
struct

  open Misc;
  open PRETTY;
  open SYMBOLS;              infix 8 eq neq;

  type location = { file: string, startLine: FixedInt.int, startPosition: FixedInt.int, endLine: FixedInt.int, endPosition: FixedInt.int }

  type lexan =
    {
      stream:      unit -> char option,
      ch:          char ref,
      sy:          sys ref,
      id:          string ref,
      messageOut:
        { location: location, hard: bool, message: pretty, context: pretty option } -> unit,
      errors:      bool ref,
      pushedSym:   sys ref,
      extraChars:  char list ref,
      debugParams: Universal.universal list,
      (* Location information. *)
      getLineNo:   unit -> FixedInt.int,
      getOffset:   unit -> FixedInt.int,
      fileName:    string,
      startLine:   FixedInt.int ref,
      endLine:     FixedInt.int ref,
      startPosition: FixedInt.int ref,
      endPosition:   FixedInt.int ref,
      bindingCounter: unit -> FixedInt.int
    };

    val eofChar         = Char.chr 4; (* ctrl/D *)

    val isNumeric = Char.isDigit
    and isAlphabetic = Char.isAlpha
    and isWhiteSpace = Char.isSpace
    and isHexadecimal  = Char.isHexDigit

    fun isAlphaNumeric c =
        if Char.isAlphaNum c then (aflTrace 1; true)
        else if c = #"'" then (aflTrace 2; true)
        else if c = #"_" then (aflTrace 3; true)
        else (aflTrace 4; false)

    val errorMessageProcTag:
        ({ location: location, hard: bool, message: pretty, context: pretty option } -> unit)
            Universal.tag =
        Universal.tag()

    val isOperator = Char.contains ":=<>+*!^/|&%~-?`@\\$#";

    fun initial (stream, parameters) : lexan =
    let
        open DEBUG
        val errorMessageProc =
            case List.find (Universal.tagIs errorMessageProcTag) parameters of
                SOME f => (aflTrace 5; Universal.tagProject errorMessageProcTag f)
            |   NONE => (aflTrace 6; fn _ => raise Fail "Error in source code")
        val lineno = getParameter lineNumberTag parameters
        val offset = getParameter offsetTag parameters
        val filename = getParameter fileNameTag parameters
        val initialLine = lineno()
        and initialOffset = offset()
        val bindingCounter = getParameter bindingCounterTag parameters
    in
        {
          stream      = stream,
          ch          = ref #" ",
          sy          = ref Semicolon,
          id          = ref "",
          messageOut  = errorMessageProc,
          errors      = ref false,
          pushedSym   = ref Othersy,
          extraChars  = ref [],
          debugParams = parameters,
          getLineNo   = lineno,
          getOffset   = offset,
          fileName    = filename,
          startLine   = ref initialLine,
          endLine     = ref initialLine,
          startPosition = ref initialOffset,
          endPosition   = ref initialOffset,
          bindingCounter = bindingCounter
        }
    end

   val nullLex = initial (fn () => NONE, []);

    fun errorOccurred ({errors, ...}:  lexan) = ! errors;

    fun location ({fileName, startLine, endLine, startPosition, endPosition,...}:lexan) =
        { file = fileName, startLine = !startLine, endLine = !endLine,
          startPosition = !startPosition, endPosition = !endPosition}

    fun reportError ({messageOut,errors,...} : lexan) (report as { hard, ...}) =
    (
        if hard then (aflTrace 10; errors := true) else (aflTrace 11; ());
        messageOut report
    )

    fun setSymbolStart {getLineNo, getOffset, startLine, endLine, startPosition, endPosition, ...} =
    let
        val line = getLineNo() and offset = getOffset()
    in
        startLine := ! endLine; endLine := line;
        startPosition := ! endPosition; endPosition := offset
    end

    fun setSymbolEnd {getLineNo, getOffset, endLine, endPosition, ...} =
    let
        val line = getLineNo() and offset = getOffset()
    in
        endLine := line;
        endPosition := offset
    end

    fun breakWords str =
    let
        val words = String.tokens Char.isSpace str
        fun addBreaks [] = (aflTrace 15; [PrettyString ""])
        |   addBreaks [last] = (aflTrace 16; [PrettyString last])
        |   addBreaks (hd :: tl) =
                (aflTrace 17; PrettyString hd :: PrettyBreak(1, 0) :: addBreaks tl)
    in
        addBreaks words
    end

    fun errorMessage (lexan, location, message) =
        reportError lexan
        {
            location = location,
            message = PrettyBlock(3, false, [], breakWords message),
            hard = true,
            context = NONE
        }
    and warningMessage (lexan, location, message) =
        reportError lexan
        {
            location = location,
            message = PrettyBlock(3, false, [], breakWords message),
            hard = false,
            context = NONE
        }

    fun lexError(state, text) =
    (
        setSymbolEnd state;
        errorMessage (state, location state, text)
    )

    exception EndOfLine;

    fun nextCh({ch, stream, extraChars = ref [], ...}) = (aflTrace 20; ch := getOpt(stream(), eofChar))
     |  nextCh({ch, extraChars = extra as ref(c::l), ...}) = (aflTrace 21; extra := l; ch := c)

    fun skipWhiteSpace (state as {ch = ref c, ...}:lexan) : char =
    if isWhiteSpace c
    then (aflTrace 22; setSymbolEnd state; nextCh state; skipWhiteSpace state)
    else (aflTrace 23; c)

    fun readChars (state as { ch, ... }) (isOk: char -> bool) (s: string) : string =
    let
        fun loop (): string list =
        let
            val theChar  = ! ch;
        in
            if isOk theChar
            then (aflTrace 24; setSymbolEnd state; nextCh state; str theChar :: loop ())
            else (aflTrace 25; [])
        end;
    in
        concat (s :: loop ())
    end;

    (* ================================================================ *)
    (* parseNumber — IDs 100-199                                        *)
    (* ================================================================ *)
    fun parseNumber (hasMinus, state as { sy, id, ch, extraChars, ... }) =
     (
        sy := IntegerConst;
        id := readChars state isNumeric "";

        if not hasMinus andalso !ch = #"w" andalso !id = "0"
        then (* word constant *)
        (
            aflTrace 100;
            nextCh state;
            if !ch = #"x"
            then
            (
                aflTrace 101;
                nextCh state;
                if isHexadecimal (!ch)
                then
                (
                    aflTrace 102;
                    sy := WordConst;
                    id := readChars state isHexadecimal "0wx"
                )
                else (aflTrace 103; extraChars := [#"x", !ch]; ch := #"w")
            )
            else if isNumeric (!ch)
            then
            (
                aflTrace 104;
                sy := WordConst;
                id := readChars state isNumeric "0w"
            )
            else (aflTrace 105; extraChars := [!ch]; ch := #"w")
        )
        else if !ch = #"x" andalso !id = "0"
        then (* Hexadecimal integer constant. *)
        (
            aflTrace 110;
            nextCh state;
            if isHexadecimal (!ch)
            then (aflTrace 111; id := readChars state isHexadecimal "0x")
            else (aflTrace 112; extraChars := [!ch]; ch := #"x")
        )
        else if !ch = #"." orelse
                !ch = #"E" orelse !ch = #"e"
        then (* possible real constant. *)
        (
            aflTrace 120;
            if !ch = #"."
            then
            (
               aflTrace 121;
               sy := RealConst;
               id := !id ^ ".";
               nextCh state;
               if not (isNumeric (!ch))
               then (aflTrace 122; lexError(state, "malformed real number: " ^ !id ^ str(!ch)))
               else (aflTrace 123; id := readChars state isNumeric (!id))
            )
            else (aflTrace 124; ());

            if !ch = #"E" orelse !ch = #"e"
            then
            let
                val eChar = !ch
            in
                aflTrace 130;
                nextCh state;

                if !ch = #"~"
                then
                (
                    aflTrace 131;
                    nextCh state;
                    if isNumeric(!ch)
                    then (aflTrace 132; sy := RealConst; id := readChars state isNumeric (!id ^ "E~"))
                    else (aflTrace 133; extraChars := [#"~", !ch]; ch := eChar)
                )
                else
                (
                    aflTrace 134;
                    if isNumeric(!ch)
                    then (aflTrace 135; sy := RealConst; id := readChars state isNumeric (!id ^ "E"))
                    else (aflTrace 136; extraChars := [!ch]; ch := eChar)
                )
            end
            else (aflTrace 137; ())
        )
        else (aflTrace 140; ())
     );

    (* ================================================================ *)
    (* parseString — IDs 200-299                                        *)
    (* ================================================================ *)
    fun parseString (state as { ch, id, ... }) =
    let
        fun getString (soFar: char list) =
         (
            case !ch of
                #"\"" => (aflTrace 200; setSymbolEnd state; nextCh state; soFar)

            |   #"\n" => (aflTrace 201; setSymbolEnd state; nextCh state; raise EndOfLine)

            |   #"\\" =>
                    let
                        val _ = nextCh state;
                        val next = !ch;
                        val _ = nextCh state;
                    in
                    if isWhiteSpace next
                    then
                        (
                        aflTrace 210;
                        if skipWhiteSpace state = #"\\" then (aflTrace 211; ())
                        else
                            (
                            aflTrace 212;
                            lexError(state, "unexpected character " ^
                               String.toString (str (!ch)) ^" in \\ ... \\");
                            while !ch <> #"\\"  andalso !ch <> #"\"" andalso !ch <> eofChar
                            do nextCh state
                            );
                        nextCh state;
                        getString soFar
                        )
                    else if next = #"^"
                    then    let
                            val _ = aflTrace 220
                            val next2 = !ch;
                            val _ = nextCh state;
                        in  getString (next2 :: #"^" :: #"\\" :: soFar)
                        end
                    else (aflTrace 221; getString (next :: #"\\" :: soFar))
                  end

            |   ch =>
                    (
                     aflTrace 230;
                     nextCh state;
                     if ch = eofChar then (aflTrace 231; raise EndOfLine)
                     else if Char.isPrint ch
                     then (aflTrace 232; getString (ch :: soFar))
                     else
                        (
                        aflTrace 233;
                        lexError(state, "unprintable character " ^ Char.toString ch ^ " found in string");
                        getString soFar
                        )
                    )
         )

    in
        nextCh state;

        id := String.implode(List.rev(getString []))
            handle EndOfLine =>
                (aflTrace 240; lexError(state, "no matching quote found on this line"))

    end;


    (* ================================================================ *)
    (* parseComment — IDs 300-399                                       *)
    (* ================================================================ *)
    fun parseComment (state as { stream, ch, ... }) =
    let
       fun skipComment () : char =
       let
         fun skipCommentBody (firstCh : char) : char =
            if firstCh = eofChar
            then
            (
               aflTrace 300;
               setSymbolEnd state;
               lexError(state, "end of file found in comment");
               firstCh
            )
            else case (firstCh, getOpt(stream (), eofChar)) of
                (#"*", #")") => (aflTrace 301; getOpt(stream (), eofChar))
            |   (#"(", #"*") => (aflTrace 302; skipCommentBody (skipComment ()))
            |   (_, nextCh) => (aflTrace 303; skipCommentBody nextCh)
       in
         skipCommentBody (getOpt(stream (), eofChar))
       end;

    in
        ch := skipComment ()
    end;


    (* ================================================================ *)
    (* parseIdent — IDs 400-499                                         *)
    (* ================================================================ *)
    fun parseIdent (state as { ch, id, sy, ... }) charsetTest first =
    let
        val idVal = readChars state charsetTest first;
    in
        if !ch = #"."
        then
        let
            val () = aflTrace 400
            val () = nextCh state;
            val c = !ch;
        in
             if isAlphabetic c
               then (aflTrace 401; parseIdent state isAlphaNumeric (idVal ^ "."))

             else if isOperator c
               then (aflTrace 402; parseIdent state isOperator (idVal ^ "."))

             else (aflTrace 403; lexError(state, "invalid identifier - "^ idVal ^ "." ^ str c))
        end
        else
        (
            aflTrace 410;
            id := idVal;
            sy := (if 0 < size idVal andalso String.str(String.sub(idVal, 0)) = "'"
                   then (aflTrace 411; TypeIdent)
                   else (aflTrace 412; lookup idVal))
        )
    end;


    (* ================================================================ *)
    (* parseToken — IDs 500-599                                         *)
    (* ================================================================ *)
    fun parseToken (state as { ch, id, sy, ... }) =
    let
        val nextSym = skipWhiteSpace state
    in
        setSymbolStart state;

        case nextSym of
          #"~" =>
             (
               aflTrace 500;
               nextCh state;
               if isNumeric (!ch)
               then
               (
                 aflTrace 501;
                 parseNumber(true, state);
                 id := "~" ^ !id
               )
               else
                 (aflTrace 502; parseIdent state isOperator "~")
             )

        | #"#" =>
              (
                aflTrace 510;
                nextCh state;
                if !ch = #"\""
                then (aflTrace 511; parseString state; sy := CharConst)
                else (aflTrace 512; parseIdent state isOperator "#")
              )

        | #"\"" => (aflTrace 520; parseString state; sy := StringConst)

        | #";" =>
            (
                aflTrace 521;
                sy := Semicolon;
                ch := #" "
            )

        | #"," => (aflTrace 522; sy := Comma; nextCh state)

        | #"(" =>
              (
                aflTrace 530;
                nextCh state;
                if !ch <> #"*" then (aflTrace 531; sy := LeftParen) else (aflTrace 532; parseComment state)
              )

        | #")" => (aflTrace 533; sy := RightParen; nextCh state)

        | #"[" => (aflTrace 534; sy := LeftBrack; nextCh state)

        | #"]" => (aflTrace 535; sy := RightBrack; nextCh state)

        | #"_" => (aflTrace 536; sy := Underline; nextCh state)

        | #"{" => (aflTrace 537; sy := LeftCurly; nextCh state)

        | #"}" => (aflTrace 538; sy := RightCurly; nextCh state)

        | #"." =>
            (
                aflTrace 540;
                nextCh state;
                if !ch <> #"."
                then (aflTrace 541; lexError(state, "unknown symbol ." ^ str(!ch)))
                else
                (
                    aflTrace 542;
                    setSymbolEnd state;
                    nextCh state;
                    if !ch <> #"."
                    then (aflTrace 543; lexError(state, "unknown symbol .." ^ str(!ch)))
                    else (aflTrace 544; sy := ThreeDots; setSymbolEnd state; nextCh state)
                )
            )

         | firstCh =>
            (
            aflTrace 550;
            if firstCh = eofChar
            then (aflTrace 551; sy := AbortParse)

            else if isNumeric firstCh
            then (aflTrace 552; parseNumber(false, state))

            else if isAlphabetic firstCh orelse firstCh = #"'"
            then (aflTrace 553; parseIdent state isAlphaNumeric "")

            else if isOperator firstCh
            then (aflTrace 554; parseIdent state isOperator "")

            else let
                val _ = aflTrace 555
                val printableFirstCh = Char.toString firstCh
            in
                lexError(state, "unknown character \"" ^ printableFirstCh ^ "\"");
                nextCh state
            end
            );
        if (!sy = Othersy) then (aflTrace 560; parseToken state) else (aflTrace 561; ())
    end;

    (* ================================================================ *)
    (* insymbol — IDs 600-699                                           *)
    (* ================================================================ *)
    fun insymbol (state as {sy,pushedSym,...}:lexan) =
    if ! pushedSym <> Othersy then (aflTrace 600; pushedSym := Othersy)
    else
    (
        aflTrace 601;
        if ! sy = AbortParse
        then
        (
            aflTrace 602;
            setSymbolStart state;
            lexError(state, "unexpected end of file encountered");
            raise InternalError "end of file"
        )
        else (aflTrace 603; ());

        sy := Othersy;

        parseToken state
    );

    fun pushBackSymbol ({pushedSym,...}:lexan, sym) =
        if !pushedSym <> Othersy then (aflTrace 610; raise InternalError "Attempt to push two parentheses")
        else (aflTrace 611; pushedSym := sym)

   fun sy ({sy=ref sy, pushedSym = ref pushed, ...}:lexan) =
        if pushed <> Othersy then (aflTrace 620; pushed) else (aflTrace 621; sy);

   fun id ({id=ref id,...}:lexan) = id;

   val debugParams = #debugParams

   fun newBindingId({bindingCounter, ...}: lexan) = bindingCounter()

   val nullLocation: location =
        { file="", startLine=0, startPosition=0, endLine=0, endPosition=0 }

    fun locSpan ({ file, startLine, startPosition, ... }: location,
                 { endLine, endPosition, ... }: location) =
    {
        file=file, startLine=startLine, startPosition=startPosition,
        endLine=endLine, endPosition=endPosition
    }

    fun errorDepth{debugParams, ...} = DEBUG.getParameter DEBUG.errorDepthTag debugParams

    structure Sharing =
    struct
        type pretty     = pretty
        and  lexan      = lexan
        and  sys        = sys
    end

end (* LEX functor body *);
