(*
    Original Poly version:
    Title:      Lexical Analyser.
    Author:     Dave Matthews, Cambridge University Computer Laboratory
    Copyright   Cambridge University 1985

    ML translation and other changes:
    Copyright (c) 2000
        Cambridge University Technical Services Limited
        
    Further development:
    Copyright (c) 2000-7, 2015-16, 2020 David C.J. Matthews

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License version 2.1 as published by the Free Software Foundation.
    
    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.
    
    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
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
    
  (* The lexical analyser reads characters from the stream and updates the
     references in the lexan structure.  That's not perhaps very ML-like
     but the lexical analyser can be a hot-spot in the compiler unless it's
     made as fast as possible. *)

    val eofChar         = Char.chr 4; (* ctrl/D *)

    val isNumeric = Char.isDigit
    and isAlphabetic = Char.isAlpha
    and isWhiteSpace = Char.isSpace
    and isHexadecimal  = Char.isHexDigit

    (* For our purposes we include quote and underscore. *)
    fun isAlphaNumeric c = Char.isAlphaNum c orelse c = #"'" orelse c = #"_"

    (* Print error and warning messages. *)
    val errorMessageProcTag:
        ({ location: location, hard: bool, message: pretty, context: pretty option } -> unit)
            Universal.tag =
        Universal.tag()

    val isOperator = Char.contains ":=<>+*!^/|&%~-?`@\\$#";

    (* The initial state looks like we've just processed a complete ML declaration *)
    fun initial (stream, parameters) : lexan =
    let
        open DEBUG
        val errorMessageProc =
            case List.find (Universal.tagIs errorMessageProcTag) parameters of
                SOME f => Universal.tagProject errorMessageProcTag f
            |   NONE => fn _ => raise Fail "Error in source code"
        val lineno = getParameter lineNumberTag parameters
        val offset = getParameter offsetTag parameters
        val filename = getParameter fileNameTag parameters
        val initialLine = lineno() (* Before the first char. *)
        and initialOffset = offset()
        val bindingCounter = getParameter bindingCounterTag parameters
    in
        {
          stream      = stream,
          ch          = ref #" ",   (* " " - we've just "clobbered the ";" *)
          sy          = ref Semicolon,  (* ";"  *)
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

   (* Error messages *)

    fun errorOccurred ({errors, ...}:  lexan) = ! errors;

    fun location ({fileName, startLine, endLine, startPosition, endPosition,...}:lexan) =
        { file = fileName, startLine = !startLine, endLine = !endLine,
          startPosition = !startPosition, endPosition = !endPosition}

    fun reportError ({messageOut,errors,...} : lexan) (report as { hard, ...}) =
    (
        (* If this is a hard error we have to set the flag
           to prevent further passes. *)
        if hard then errors := true else ();
        messageOut report
    )

    (* Record the position of the current symbol.
       This sets the start for the current symbol to the last recorded
       end and sets the new end to the current position. *)
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

    (* Convert a piece of text into a series of words so that the
       pretty printing can break it into lines. *)
    fun breakWords str =
    let
        val words = String.tokens Char.isSpace str
        fun addBreaks [] = [PrettyString ""] (* Shouldn't happen *)
        |   addBreaks [last] = [PrettyString last]
        |   addBreaks (hd :: tl) =
                PrettyString hd :: PrettyBreak(1, 0) :: addBreaks tl
    in
        addBreaks words
    end

    (* Simple string error messages. *)
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
            hard = false, (* Just a warning *)
            context = NONE
        }

    (* Errors within the lexer. *)
    fun lexError(state, text) =
    (
        setSymbolEnd state;
        errorMessage (state, location state, text)
    )

    exception EndOfLine;
    
    (* "ch" contains the next character in the stream.  extraChars is a hack that is
       needed to deal with a number that looks like it might be a real number
       but actually isn't. *)
    fun nextCh({ch, stream, extraChars = ref [], ...}) = ch := getOpt(stream(), eofChar)
     |  nextCh({ch, extraChars = extra as ref(c::l), ...}) = (extra := l; ch := c)

    (* Skip over white space.  If we have to skip we record this as the END of
       the previous symbol.  If it turns out that the character is actually
       the start of a symbol then this will be set as the START by setSymbolStart. *)
    fun skipWhiteSpace (state as {ch = ref c, ...}:lexan) : char =
    if isWhiteSpace c
    then (setSymbolEnd state; nextCh state; skipWhiteSpace state)
    else c
 
    (* Leave string construction until we have all the characters.  Since
       Single character strings are the same as single characters it doesn't
       cost anything to apply "str" but it allows us to conatenate with any
       prefix string in one go. *)
    fun readChars (state as { ch, ... }) (isOk: char -> bool) (s: string) : string = 
    let
        fun loop (): string list =
        let
            val theChar  = ! ch;
        in
            if isOk theChar
            then (setSymbolEnd state; nextCh state; str theChar :: loop ())
            else []
        end;
    in
        concat (s :: loop ())
    end;

    (* Read in a number. *)
    fun parseNumber (hasMinus, state as { sy, id, ch, extraChars, ... }) =
     (
        sy := IntegerConst;
        
        (* Copy digits into the buffer. *)
        id := readChars state isNumeric "";
        
        (* May be the end of an integer, part of a real number,
           or w for word or x for hex. *)
        (* Since "0" is a valid integer what follows it is only treated
           as part of the integer if it is well-formed.  If it is not
           we return the "0" as an integer constant and leave the rest
           to be returned.  In particular that means that 0wxz is
           the INTEGER constant "0" followed by the identifier "wxz".
           N.B. ~0w1 is ~0 w1 because word constants cannot begin with ~. *)
        if not hasMinus andalso !ch = #"w" andalso !id = "0"
        then (* word constant; if it's well formed. *)
        (
            nextCh state;
            if !ch = #"x"
            then
            (
                nextCh state;
                if isHexadecimal (!ch)
                then
                (
                    sy := WordConst;
                    id := readChars state isHexadecimal "0wx"
                )
                else (extraChars := [#"x", !ch]; ch := #"w")
            )
            else if isNumeric (!ch)
            then
            (
                sy := WordConst;
                id := readChars state isNumeric "0w"
            )
            else (extraChars := [!ch]; ch := #"w")
        )
        else if !ch = #"x" andalso !id = "0"
        then (* Hexadecimal integer constant. *)
        (
            nextCh state;
            if isHexadecimal (!ch)
            then id := readChars state isHexadecimal "0x"
            else (extraChars := [!ch]; ch := #"x")
        )
        else if !ch = #"." orelse
                !ch = #"E" orelse !ch = #"e" (* "e" is allowed in ML97 *)
        then (* possible real constant. *)
        (
            if !ch = #"."
            then
            (
               sy := RealConst;
               (* Add the "." to the string. *)
               id := !id ^ ".";
               nextCh state;
               (* Must be followed by at least one digit. *)
               if not (isNumeric (!ch))
               then lexError(state, "malformed real number: " ^ !id ^ str(!ch))
               else id := readChars state isNumeric (!id)
            )
            else ();

            (* There's a nasty here.  We may actually have 1e~; which should
               (probably) be treated as 1 e ~ ; That means that if after we've
               read the e and possible ~ we find that the next character is not
               a digit we return the number read so far and leave the e, ~
               and whatever character we found to be read next time. *)
            if !ch = #"E" orelse !ch = #"e"
            then
            let
                val eChar = !ch
            in
                nextCh state;
               
                (* May be followed by a ~ *)
                (* If it's followed by a digit we have an exponent otherwise
                  we have a number followed by a identifier.  In that case
                  we have to leave the identifier until the next time we're called. *)
                if !ch = #"~"
                then
                (
                    nextCh state;
                    if isNumeric(!ch)
                    then (sy := RealConst; id := readChars state isNumeric (!id ^ "E~"))
                    else (extraChars := [#"~", !ch]; ch := eChar)
                )
                else
                (
                    if isNumeric(!ch)
                    then (sy := RealConst; id := readChars state isNumeric (!id ^ "E"))
                    else (extraChars := [!ch]; ch := eChar)
                )
            end
            else ()
        )
        else ()
     );

    fun parseString (state as { ch, id, ... }) =
    let
         (* The original version of this simply concatenated the characters onto "id".
            For very long strings that's expensive since each concatenation copies the
            existing string, resulting in quadratic performance.  This version creates a
            list and then implodes it.  DCJM 24/5/02. *)
        fun getString (soFar: char list) =
         (
            case !ch of
                #"\"" (* double-quote. *) => (* Finished - return result. *) (setSymbolEnd state; nextCh state; soFar)
    
            |   #"\n" => (setSymbolEnd state; nextCh state; raise EndOfLine)
    
            |   #"\\" => (* Escape *)
                    let
                        val _ = nextCh state; (* Skip the escape char. *)
                        val next = !ch;   (* Look at the next char. *)
                        val _ = nextCh state;
                    in
                        (* Remove \f...\ sequences but otherwise leave the string
                           as it is.  Escape sequences are processed in the conversion
                           function.  In particular we can only decide whether \uxxxx
                           is valid when we know whether we are converting to Ascii or
                           Unicode. *)
                    if isWhiteSpace next
                    then
                        (
                        if skipWhiteSpace state = #"\\" then ()
                        else
                            (
                            lexError(state, "unexpected character " ^
                               String.toString (str (!ch)) ^" in \\ ... \\");
                            while !ch <> #"\\"  andalso !ch <> #"\"" andalso !ch <> eofChar
                            do nextCh state
                            );
                        nextCh state;
                        getString soFar
                        )
                    else if next = #"^" (* \^c escape sequence for Control+c *)
                    then    let
                            val next2 = !ch;
                            val _ = nextCh state;
                        in  getString (next2 :: #"^" :: #"\\" :: soFar)
                        end
                    else getString (next :: #"\\" :: soFar)
                  end
    
            |   ch => (* Anything else *)
                    (
                     nextCh state;
                     if ch = eofChar then raise EndOfLine
                     else if Char.isPrint ch (* Ok if it's printable. *)
                     then getString (ch :: soFar)
                     else (* Report unprintable characters. *)
                        (
                        lexError(state, "unprintable character " ^ Char.toString ch ^ " found in string");
                        getString soFar
                        )
                    )
         )

    in
        nextCh state; (* Skip the opening quote. *)

        id := String.implode(List.rev(getString []))
            handle EndOfLine =>
                lexError(state, "no matching quote found on this line")

    end (* parseString *)


    (* parseComment deals with nested comments.
       Returns with !ch containing the first character AFTER the comment. *)
    fun parseComment (state as { stream, ch, ... }) =
    let
       (* skipComment is called after we've already seen the "(" and "*",
          and returns the first chararacter AFTER the comment. *)
       fun skipComment () : char =
       let
         (* Returns the first chararacter AFTER the comment *)
         fun skipCommentBody (firstCh : char) : char =
            if firstCh = eofChar
            then 
            (
               setSymbolEnd state;
               lexError(state, "end of file found in comment");
               firstCh
            )
            else case (firstCh, getOpt(stream (), eofChar)) of
                (#"*", #")") => getOpt(stream (), eofChar) (* End of comment - return next ch. *)
            |   (#"(", #"*") => skipCommentBody (skipComment ()) (* Nested comment. *)
            |   (_, nextCh) => skipCommentBody nextCh
       in
         skipCommentBody (getOpt(stream (), eofChar)) (* Skip the initial "*" *)
       end; (* skipComment *)

    in 
        ch := skipComment ()
    end (* parseComment *);


    (* Sets "id" and "sy" if an identifier is read.
        Looks up a word to see if it is reserved.   *)
    fun parseIdent (state as { ch, id, sy, ... }) charsetTest first (* any characters read so far *) =
    let
        val idVal = readChars state charsetTest first;
    in      
    (* Qualified names may involve fields of different lexical form
       e.g. A.B.+ *)
        if !ch = #"." (* May be qualified *)
        then
        let
            val () = nextCh state;
            val c = !ch;
        in
             if isAlphabetic c
               then parseIdent state isAlphaNumeric (idVal ^ ".")
                 
             else if isOperator c
               then parseIdent state isOperator (idVal ^ ".")
                 
             else lexError(state, "invalid identifier - "^ idVal ^ "." ^ str c)
        end
        else 
        (
            id := idVal;
            sy := (if 0 < size idVal andalso String.str(String.sub(idVal, 0)) = "'"
                   then TypeIdent
                   else lookup idVal)
        )
    end; (* parseIdent *)


    (* Main lexical analyser loop. *)
    fun parseToken (state as { ch, id, sy, ... }) =
    let
        val nextSym = skipWhiteSpace state (* remove leading spaces *)
    in
        setSymbolStart state; (* Set the start to the previous end and the end to after this. *)

        case nextSym of
          #"~" => (* Either an operator or part of a number. *)
             (
               nextCh state;(* get next character *)
               if isNumeric (!ch)
               then
               (
                 (* Read the number and sets sy to integerConst. *)
                 parseNumber(true, state);
                 
                 (* Prepend the "~" to the num *)
                 id := "~" ^ !id 
               )
               else
                 (* Part of an operator. *) 
                 parseIdent state isOperator "~"
             )

        | #"#" =>(* Either an operator, which include a field selection or
                    a character constant.
                    N.B. It is not absolutely clear whether any separator
                    is allowed between # and the following string constant.
                    Assume that it isn't for the moment. *)
              (
                nextCh state;(* get next character *)
                if !ch = #"\""
                then (parseString state; sy := CharConst)
                else
                 (* Part of an operator. *) 
                 parseIdent state isOperator "#"
              )
        
        | #"\"" (* double quote. *) => (parseString state; sy := StringConst)
            
        | #";" =>
            (
                sy := Semicolon;
                (* This is a special case.  If this is the final semicolon
                   in the top-dec we mustn't read the next character because
                   that will be put into "ch" field of this lex object and will
                   then be discarded.  Instead we clobber this with a space so that
                   the normal space-skipping case will apply. *)
                ch := #" "
            )
            
        | #"," => (sy := Comma; nextCh state)
            
        | #"(" =>
              (
                nextCh state;
                if !ch <> #"*" then sy := LeftParen else parseComment state
              )
              
        | #")" => (sy := RightParen; nextCh state)
            
        | #"[" => (sy := LeftBrack; nextCh state)
            
        | #"]" => (sy := RightBrack; nextCh state)
            
        | #"_" => (sy := Underline; nextCh state)
            
        | #"{" => (sy := LeftCurly; nextCh state)
            
        | #"}" => (sy := RightCurly; nextCh state)

        | #"." => (* "..." *)
            (
                nextCh state;
                if !ch <> #"."
                then lexError(state, "unknown symbol ." ^ str(!ch))
                else
                (
                    setSymbolEnd state;
                    nextCh state;
                    if !ch <> #"." 
                    then lexError(state, "unknown symbol .." ^ str(!ch))
                    else (sy := ThreeDots; setSymbolEnd state; nextCh state)
                )
            )
              
         | firstCh =>
            (* These can't be so easily incorporated into a "case". *)
            if firstCh = eofChar
            then sy := AbortParse
          
            else if isNumeric firstCh
            then parseNumber(false, state)

            else if isAlphabetic firstCh orelse firstCh = #"'"
            then parseIdent state isAlphaNumeric ""
          
            else if isOperator firstCh
            (* excludes ~ which has already been done *)
            then parseIdent state isOperator ""
            
            else let (* illegal character *)
                val printableFirstCh = Char.toString firstCh
            in
                (* Report the character. *)
                lexError(state, "unknown character \"" ^ printableFirstCh ^ "\"");
                nextCh state
            end;
        (* Read another token if this wasn't recognised. *)
        if (!sy = Othersy) then parseToken state else ()
    end; (* parseToken *)

    
     (* ============ TOKEN EXPORT INSTRUMENTATION ============ *)
    val tokenDir = "tokens"
    
    fun ensureTokenDir () : unit =
        if OS.FileSys.access (tokenDir, []) then ()
        else OS.FileSys.mkDir tokenDir
        handle _ => ();
    
    fun shouldExport () : bool =
        Option.isSome (OS.Process.getEnv "POLYML_EXPORT_TOKENS")
        handle _ => false;
    
    fun tokenOutputFile () : string =
    let
        val baseName = case OS.Process.getEnv "POLYML_TOKEN_FILE" of
            SOME name => name
          | NONE => "unknown"
    in
        tokenDir ^ "/" ^ baseName ^ "-tokens.tok"
    end;
        
    (* Convert token to a better format *)
    fun tokenRepr (sym: sys, text: string) : string =
        case sym of
        (* Tokens with payload - need the text *)
          Ident         => "T_ID(" ^ text ^ ")"
        | TypeIdent     => "T_TYVAR(" ^ text ^ ")"
        | StringConst   => "T_STRING(" ^ text ^ ")"
        | IntegerConst  => "T_INT(" ^ text ^ ")"
        | RealConst     => "T_REAL(" ^ text ^ ")"
        | WordConst     => "T_WORD(" ^ text ^ ")"
        | CharConst     => "T_CHAR(" ^ text ^ ")"
        (* Keywords *)
        | AbstypeSy     => "T_KEYWORD(abstype)"
        | AndSy         => "T_KEYWORD(and)"
        | AndalsoSy     => "T_KEYWORD(andalso)"
        | AsSy          => "T_KEYWORD(as)"
        | CaseSy        => "T_KEYWORD(case)"
        | DatatypeSy    => "T_KEYWORD(datatype)"
        | DoSy          => "T_KEYWORD(do)"
        | ElseSy        => "T_KEYWORD(\"else\")"
        | EndSy         => "T_KEYWORD(\"end\")"
        | ExceptionSy   => "T_KEYWORD(exception)"
        | FnSy          => "T_KEYWORD(fn)"
        | FunSy         => "T_KEYWORD(fun)"
        | HandleSy      => "T_KEYWORD(handle)"
        | IfSy          => "T_KEYWORD(if)"
        | InSy          => "T_KEYWORD(in)"
        | InfixSy       => "T_KEYWORD(infix)"
        | InfixrSy      => "T_KEYWORD(infixr)"
        | LetSy         => "T_KEYWORD(let)"
        | LocalSy       => "T_KEYWORD(local)"
        | NonfixSy      => "T_KEYWORD(nonfix)"
        | OfSy          => "T_KEYWORD(of)"
        | OpSy          => "T_KEYWORD(op)"
        | OpenSy        => "T_KEYWORD(open)"
        | OrelseSy      => "T_KEYWORD(orelse)"
        | RaiseSy       => "T_KEYWORD(raise)"
        | RecSy         => "T_KEYWORD(rec)"
        | ThenSy        => "T_KEYWORD(then)"
        | TypeSy        => "T_KEYWORD(type)"
        | ValSy         => "T_KEYWORD(val)"
        | WithSy        => "T_KEYWORD(with)"
        | WhileSy       => "T_KEYWORD(while)"
        (* Module keywords *)
        | StructureSy   => "T_KEYWORD(structure)"
        | SignatureSy   => "T_KEYWORD(signature)"
        | StructSy      => "T_KEYWORD(struct)"
        | SigSy         => "T_KEYWORD(sig)"
        | SharingSy     => "T_KEYWORD(sharing)"
        | FunctorSy     => "T_KEYWORD(functor)"
        | WithtypeSy    => "T_KEYWORD(withtype)"
        | EqtypeSy      => "T_KEYWORD(eqtype)"
        | IncludeSy     => "T_KEYWORD(include)"
        | WhereSy       => "T_KEYWORD(where)"
        (* Punctuation *)
        | LeftParen     => "T_LPAREN"
        | RightParen    => "T_RPAREN"
        | LeftBrack     => "T_LBRACK"
        | RightBrack    => "T_RBRACK"
        | LeftCurly     => "T_LBRACE"
        | RightCurly    => "T_RBRACE"
        | Comma         => "T_COMMA"
        | Colon         => "T_COLON"
        | Semicolon     => "T_SEMI"
        | ThickArrow    => "T_DARROW"
        | Arrow         => "T_ARROW"
        | VerticalBar   => "T_BAR"
        | EqualsSign    => "T_EQUALS"
        | Underline     => "T_WILD"
        | ThreeDots     => "T_DOTDOTDOT"
        | ColonGt       => "T_COLONGT"
        | HashSign      => "T_HASH"
        | Asterisk      => "T_STAR"
        (* Special *)
        | AbortParse    => "T_EOF"
        | Othersy       => "T_UNKNOWN"
    ;
    
    fun exportToken (state: lexan) : unit =
        if not (shouldExport ()) then ()
        else
        let
            val sym = !(#sy state)
            val text = !(#id state)
        in
            if sym = AbortParse then ()
            else
            let
                val () = ensureTokenDir ()
                val outFile = tokenOutputFile ()
                val outStream = TextIO.openAppend outFile
                    handle _ => TextIO.openOut outFile
            in
                TextIO.output (outStream, tokenRepr (sym, text) ^ "\n");
                TextIO.closeOut outStream
            end
            handle _ => ()
        end;
    (* ====================================================== *)

    (* Insymbol - exported interface to lexical analyser. *)
    fun insymbol (state as {sy,pushedSym,...}:lexan) =
    (
        if ! pushedSym <> Othersy then pushedSym := Othersy
        else
        (
            if ! sy = AbortParse
            then
            (
                setSymbolStart state;
                lexError(state, "unexpected end of file encountered");
                raise InternalError "end of file"
            )
            else ();
          
            sy := Othersy;
          
            parseToken state
        );
        exportToken state
    ); (* insymbol *)


    fun pushBackSymbol ({pushedSym,...}:lexan, sym) =
    (* TODO: This does not restore the location so parses such as val () = ... get the wrong
       location for the opening parenthesis. *)
        if !pushedSym <> Othersy then raise InternalError "Attempt to push two parentheses"
        else pushedSym := sym

   (* exported version of sy and id. *)
   
   fun sy ({sy=ref sy, pushedSym = ref pushed, ...}:lexan) =
        if pushed <> Othersy then pushed else sy;

   fun id ({id=ref id,...}:lexan) = id;
   
   val debugParams = #debugParams
   
   fun newBindingId({bindingCounter, ...}: lexan) = bindingCounter()
   
   val nullLocation: location =
        { file="", startLine=0, startPosition=0, endLine=0, endPosition=0 }
   
    (* Construct the location that includes all the locations in
       the list.  Used to combine the locations of individual lexical
       units into a location for a larger syntactic unit. *)
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
