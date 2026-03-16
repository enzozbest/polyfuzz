(* LEX_.ML instrumented for fuzzing
    This file is a copy of LEX_.ML from the original PolyML repository with tracing code added for AFL++ fuzzing.
    Please refer to LEX__ORIGINAL.ML to see the original implementation and accreditations.

    AFL++ Trace IDs:
    Each aflTrace call uses a random 16-bit ID (0-65535).
    No two IDs share the same (id >> 1) value, which prevents prev_loc collisions
    in AFL++'s edge coverage bitmap (bitmap[cur ^ prev_loc]++; prev_loc = cur >> 1).
    This mirrors how afl-clang-fast assigns instrumentation IDs and maximises
    the distinguishability of edges in the coverage map.
    IDs were generated with seed=42 via Python random.randint
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

    fun logicalLen s =
        let fun count i =
            if i >= size s then 0
            else if String.sub(s, i) = #"\\"
            then
                let val next = i + 1 in
                    if next < size s andalso String.sub(s, next) = #"^"
                    then 1 + count (i + 3)
                    else if next < size s andalso String.sub(s, next) = #"u"
                    then 1 + count (i + 6)
                    else if next < size s andalso Char.isDigit(String.sub(s, next))
                    then 1 + count (i + 4)
                    else 1 + count (i + 2)
                end
            else 1 + count (i + 1)
        in count 0
    end

    fun isAlphaNumeric c =
        if Char.isAlphaNum c then (aflTrace 41906; true)
        else if c = #"'" then (aflTrace 7297; true)
        else if c = #"_" then (aflTrace 1640; true)
        else (aflTrace 48599; false)

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
                SOME f => (aflTrace 18025; Universal.tagProject errorMessageProcTag f)
            |   NONE => (aflTrace 16050; fn _ => raise Fail "Error in source code")
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
        if hard then (aflTrace 14629; errors := true) else (aflTrace 9145; ());
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
        fun addBreaks [] = (aflTrace 48266; [PrettyString ""])
        |   addBreaks [last] = (aflTrace 6718; [PrettyString last])
        |   addBreaks (hd :: tl) =
                (aflTrace 44349; PrettyString hd :: PrettyBreak(1, 0) :: addBreaks tl)
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

    fun nextCh({ch, stream, extraChars = ref [], ...}) = (aflTrace 48541; ch := getOpt(stream(), eofChar))
     |  nextCh({ch, extraChars = extra as ref(c::l), ...}) = (aflTrace 58470; extra := l; ch := c)

    fun skipWhiteSpace (state as {ch = ref c, ...}:lexan) : char =
    if isWhiteSpace c
    then (aflTrace 35742; setSymbolEnd state; nextCh state; skipWhiteSpace state)
    else (aflTrace 5698; c)

    fun readChars (state as { ch, ... }) (isOk: char -> bool) (s: string) : string =
    let
        fun loop (): string list =
        let
            val theChar  = ! ch;
        in
            if isOk theChar
            then (aflTrace 38699; setSymbolEnd state; nextCh state; str theChar :: loop ())
            else (aflTrace 27652; [])
        end;
    in
        concat (s :: loop ())
    end;

    (* ================================================================ *)
    (* parseNumber                                                      *)
    (* ================================================================ *)
    fun parseNumber (hasMinus, state as { sy, id, ch, extraChars, ... }) =
     (
        sy := IntegerConst;
        id := readChars state isNumeric "";

        if not hasMinus andalso !ch = #"w" andalso !id = "0"
        then (* word constant *)
        (
            aflTrace 2083;
            nextCh state;
            if !ch = #"x"
            then
            (
                aflTrace 1953;
                nextCh state;
                if isHexadecimal (!ch)
                then
                (
                    aflTrace 6141;
                    sy := WordConst;
                    id := readChars state isHexadecimal "0wx"
                )
                else (aflTrace 14329; extraChars := [#"x", !ch]; ch := #"w")
            )
            else if isNumeric (!ch)
            then
            (
                aflTrace 15248;
                sy := WordConst;
                id := readChars state isNumeric "0w"
            )
            else (aflTrace 33119; extraChars := [!ch]; ch := #"w")
        )
        else if !ch = #"x" andalso !id = "0"
        then (* Hexadecimal integer constant. *)
        (
            aflTrace 39454;
            nextCh state;
            if isHexadecimal (!ch)
            then (aflTrace 1740; id := readChars state isHexadecimal "0x")
            else (aflTrace 36782; extraChars := [!ch]; ch := #"x")
        )
        else if !ch = #"." orelse
                !ch = #"E" orelse !ch = #"e"
        then (* possible real constant. *)
        (
            aflTrace 13032;
            if !ch = #"."
            then
            (
                aflTrace 46926;
                nextCh state;

                if isNumeric(!ch)
                then
                (
                  aflTrace 42591;
                  sy := RealConst;
                  id := !id ^ ".";
                  id := readChars state isNumeric (!id)
                )
                else (aflTrace 45963; extraChars := [!ch]; ch := #".")
            )
            else (aflTrace 35714; ());

            if !ch = #"E" orelse !ch = #"e"
            then
            let
                val eChar = !ch
            in
                aflTrace 27494;
                nextCh state;

                if !ch = #"~"
                then
                (
                    aflTrace 14447;
                    nextCh state;
                    if isNumeric(!ch)
                    then (aflTrace 29440; sy := RealConst; id := readChars state isNumeric (!id ^ "E~"))
                    else (aflTrace 38619; extraChars := [#"~", !ch]; ch := eChar)
                )
                else
                (
                    aflTrace 18232;
                    if isNumeric(!ch)
                    then (aflTrace 53047; sy := RealConst; id := readChars state isNumeric (!id ^ "E"))
                    else (aflTrace 56971; extraChars := [!ch]; ch := eChar)
                )
            end
            else (aflTrace 426; ())
        )
        else (aflTrace 49730; ())
     );

    (* ================================================================ *)
    (* parseString                                                      *)
    (* ================================================================ *)
    fun parseString (state as { ch, id, ... }) =
    let
        fun getString (soFar: char list) =
         (
            case !ch of
                #"\"" => (aflTrace 52811; setSymbolEnd state; nextCh state; soFar)

            |   #"\n" => (aflTrace 10464; setSymbolEnd state; nextCh state; raise EndOfLine)

            |   #"\\" =>
                    let
                        val _ = (aflTrace 47952; nextCh state);
                        val next = !ch;
                        val _ = nextCh state;
                    in
                    if isWhiteSpace next
                    then
                        (
                        aflTrace 45754;
                        if skipWhiteSpace state = #"\\" then (aflTrace 27697; ())
                        else
                            (
                            aflTrace 22299;
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
                            val _ = aflTrace 18211
                            val next2 = !ch;
                            val _ = nextCh state;
                        in  getString (next2 :: #"^" :: #"\\" :: soFar)
                        end
                    else (aflTrace 10190; getString (next :: #"\\" :: soFar))
                  end

            |   ch =>
                    (
                     aflTrace 14111;
                     nextCh state;
                     if ch = eofChar then (aflTrace 62732; raise EndOfLine)
                     else if Char.isPrint ch
                     then (aflTrace 50037; getString (ch :: soFar))
                     else
                        (
                        aflTrace 22060;
                        lexError(state, "unprintable character " ^ Char.toString ch ^ " found in string");
                        getString soFar
                        )
                    )
         )

    in
        nextCh state;

        id := String.implode(List.rev(getString []))
            handle EndOfLine =>
                (aflTrace 6699; lexError(state, "no matching quote found on this line"))

    end;


    (* ================================================================ *)
    (* parseComment                                                     *)
    (* ================================================================ *)
    fun parseComment (state as { stream, ch, ... }) =
    let
       fun skipComment () : char =
       let
         fun skipCommentBody (firstCh : char) : char =
            if firstCh = eofChar
            then
            (
               aflTrace 6079;
               setSymbolEnd state;
               lexError(state, "end of file found in comment");
               firstCh
            )
            else (aflTrace 21716; case (firstCh, getOpt(stream (), eofChar)) of
                (#"*", #")") => (aflTrace 24899; getOpt(stream (), eofChar))
            |   (#"(", #"*") => (aflTrace 6339; skipCommentBody (skipComment ()))
            |   (_, nextCh) => (aflTrace 23527; skipCommentBody nextCh))
       in
         skipCommentBody (getOpt(stream (), eofChar))
       end;

    in
        ch := skipComment ()
    end;


    (* ================================================================ *)
    (* parseIdent                                                       *)
    (* ================================================================ *)
    fun parseIdent (state as { ch, id, sy, ... }) charsetTest first =
    let
        val idVal = readChars state charsetTest first;
    in
        if !ch = #"."
        then
        let
            val () = aflTrace 55542
            val () = nextCh state;
            val c = !ch;
        in
             if isAlphabetic c
               then (aflTrace 22542; parseIdent state isAlphaNumeric (idVal ^ "."))

             else if isOperator c
               then (aflTrace 39566; parseIdent state isOperator (idVal ^ "."))

             else (aflTrace 17336; lexError(state, "invalid identifier - "^ idVal ^ "." ^ str c))
        end
        else
        (
            aflTrace 52896;
            id := idVal;
            sy := (if 0 < size idVal andalso String.str(String.sub(idVal, 0)) = "'"
                   then (aflTrace 2848; TypeIdent)
                   else (aflTrace 47824; lookup idVal))
        )
    end;


    (* ================================================================ *)
    (* parseToken                                                       *)
    (* ================================================================ *)
    fun parseToken (state as { ch, id, sy, ... }) =
    let
        val nextSym = skipWhiteSpace state
    in
        setSymbolStart state;

        case nextSym of
          #"~" =>
             (
               aflTrace 30109;
               nextCh state;
               if isNumeric (!ch)
               then
               (
                 aflTrace 35143;
                 parseNumber(true, state);
                 id := "~" ^ !id
               )
               else
                 (aflTrace 8181; parseIdent state isOperator "~")
             )

        | #"#" =>
              (
                aflTrace 63775;
                nextCh state;
                if !ch = #"\""
                then
                (
                    aflTrace 60444;
                    parseString state;
                    sy := CharConst;
                    if logicalLen (!id) <> 1
                    then lexError(state, "character constant must contain exactly one character")
                    else (aflTrace 64569; ())
                )
                else (aflTrace 24808; parseIdent state isOperator "#")
              )

        | #"\"" => (aflTrace 5165; parseString state; sy := StringConst)

        | #";" =>
            (
                aflTrace 36179;
                sy := Semicolon;
                ch := #" "
            )

        | #"," => (aflTrace 19214; sy := Comma; nextCh state)

        | #"(" =>
              (
                aflTrace 54356;
                nextCh state;
                if !ch <> #"*" then (aflTrace 41199; sy := LeftParen) else (aflTrace 40536; parseComment state)
              )

        | #")" => (aflTrace 58029; sy := RightParen; nextCh state)

        | #"[" => (aflTrace 56473; sy := LeftBrack; nextCh state)

        | #"]" => (aflTrace 23701; sy := RightBrack; nextCh state)

        | #"_" => (aflTrace 37838; sy := Underline; nextCh state)

        | #"{" => (aflTrace 12602; sy := LeftCurly; nextCh state)

        | #"}" => (aflTrace 46175; sy := RightCurly; nextCh state)

        | #"." =>
            (
                aflTrace 4559;
                nextCh state;
                if !ch <> #"."
                then (aflTrace 3004; lexError(state, "unknown symbol ." ^ str(!ch)))
                else
                (
                    aflTrace 43337;
                    setSymbolEnd state;
                    nextCh state;
                    if !ch <> #"."
                    then (aflTrace 14936; lexError(state, "unknown symbol .." ^ str(!ch)))
                    else (aflTrace 50664; sy := ThreeDots; setSymbolEnd state; nextCh state)
                )
            )

         | firstCh =>
            (
            aflTrace 18966;
            if firstCh = eofChar
            then (aflTrace 64568; sy := AbortParse)

            else if isNumeric firstCh
            then (aflTrace 5230; parseNumber(false, state))

            else if isAlphabetic firstCh orelse firstCh = #"'"
            then (aflTrace 56055; parseIdent state isAlphaNumeric "")

            else if isOperator firstCh
            then (aflTrace 15257; parseIdent state isOperator "")

            else let
                val _ = aflTrace 56786
                val printableFirstCh = Char.toString firstCh
            in
                lexError(state, "unknown character \"" ^ printableFirstCh ^ "\"");
                nextCh state
            end
            );
        if (!sy = Othersy) then (aflTrace 6620; parseToken state) else (aflTrace 24912; ())
    end;

    (* ================================================================ *)
    (* insymbol                                                         *)
    (* ================================================================ *)
    fun insymbol (state as {sy,pushedSym,...}:lexan) =
    if ! pushedSym <> Othersy then (aflTrace 18218; pushedSym := Othersy)
    else
    (
        aflTrace 29715;
        if ! sy = AbortParse
        then
        (
            aflTrace 41661;
            setSymbolStart state;
            lexError(state, "unexpected end of file encountered");
            raise InternalError "end of file"
        )
        else (aflTrace 54665; ());

        sy := Othersy;

        parseToken state
    );

    fun pushBackSymbol ({pushedSym,...}:lexan, sym) =
        if !pushedSym <> Othersy then (aflTrace 23910; raise InternalError "Attempt to push two parentheses")
        else (aflTrace 10660; pushedSym := sym)

   fun sy ({sy=ref sy, pushedSym = ref pushed, ...}:lexan) =
        if pushed <> Othersy then (aflTrace 24261; pushed) else (aflTrace 23284; sy);

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
