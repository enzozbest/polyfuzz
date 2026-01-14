(*
    Copyright (c) 2000
        Cambridge University Technical Services Limited

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.
    
    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.
    
    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*)


(*
    Title:      Lexical symbols for ML compiler.
    Author:     Dave Matthews, Cambridge University Computer Laboratory
    Copyright   Cambridge University 1985
*)

structure Symbols : SymbolsSig =

struct
    datatype sys =
        AbortParse
    |   Ident
    |   AbstypeSy
    |   AndSy 
    |   AndalsoSy 
    |   AsSy 
    |   CaseSy
    |   DatatypeSy 
    |   DoSy 
    |   ElseSy 
    |   EndSy 
    |   ExceptionSy 
    |   FnSy 
    |   FunSy
    |   HandleSy 
    |   IfSy 
    |   InSy 
    |   InfixSy 
    |   InfixrSy 
    |   LetSy 
    |   LocalSy
    |   NonfixSy 
    |   OfSy 
    |   OpSy 
    |   OpenSy 
    |   OrelseSy 
    |   RaiseSy 
    |   RecSy 
    |   ThenSy
    |   TypeSy 
    |   ValSy 
    |   WithSy 
    |   WhileSy 
    |   LeftParen 
    |   RightParen
    |   LeftBrack
    |   RightBrack
    |   Comma 
    |   Colon 
    |   Semicolon 
    |   ThickArrow
    |   VerticalBar
    |   EqualsSign
    |   Underline 
    |   TypeIdent
    |   StringConst
    |   IntegerConst
    |   Asterisk 
    |   Arrow 
    |   RealConst 
    |   LeftCurly
    |   RightCurly
    |   ThreeDots
    |   ColonGt
    |   HashSign 
    |   StructureSy 
    |   SignatureSy
    |   StructSy 
    |   SigSy 
    |   SharingSy 
    |   FunctorSy 
    |   WithtypeSy
    |   EqtypeSy
    |   IncludeSy
    |   WhereSy
    |   WordConst
    |   CharConst
    |   Othersy

    fun repr AbortParse     = "\^G"
    |   repr Ident          = "<identifier>"
    |   repr AbstypeSy      = "abstype"
    |   repr AndSy          = "and"
    |   repr AndalsoSy      = "andalso"
    |   repr AsSy           = "as"
    |   repr CaseSy         = "case"
    |   repr DatatypeSy     = "datatype"
    |   repr DoSy           = "do"
    |   repr ElseSy         = "else"
    |   repr EndSy          = "end"
    |   repr ExceptionSy    = "exception"
    |   repr FnSy           = "fn"
    |   repr FunSy          = "fun"
    |   repr HandleSy       = "handle"
    |   repr IfSy           = "if"
    |   repr InSy           = "in"
    |   repr InfixSy        = "infix"
    |   repr InfixrSy       = "infixr"
    |   repr LetSy          = "let"
    |   repr LocalSy        = "local"
    |   repr NonfixSy       = "nonfix"
    |   repr OfSy           = "of"
    |   repr OpSy           = "op"
    |   repr OpenSy         = "open"
    |   repr OrelseSy       = "orelse"
    |   repr RaiseSy        = "raise"
    |   repr RecSy          = "rec"
    |   repr ThenSy         = "then"
    |   repr TypeSy         = "type"
    |   repr ValSy          = "val"
    |   repr WithSy         = "with"
    |   repr WhileSy        = "while"
    |   repr LeftParen      = "("
    |   repr RightParen     = ")"
    |   repr LeftBrack      = "["
    |   repr RightBrack     = "]"
    |   repr Comma          = ","
    |   repr Colon          = ":"
    |   repr Semicolon      = ";"
    |   repr ThickArrow     = "=>"
    |   repr VerticalBar    = "|"
    |   repr EqualsSign     = "="
    |   repr Underline      = "_"
    |   repr TypeIdent      = "<type identifier>"
    |   repr StringConst    = "<string constant>"
    |   repr IntegerConst   = "<integer constant>"
    |   repr Asterisk       = "*"
    |   repr Arrow          = "->"
    |   repr RealConst      = "<real constant>"
    |   repr LeftCurly      = "{"
    |   repr RightCurly     = "}"
    |   repr ThreeDots      = "..."
    |   repr ColonGt        = ":>"
    |   repr HashSign       = "#"
    |   repr StructureSy    = "structure"
    |   repr SignatureSy    = "signature"
    |   repr StructSy       = "struct"
    |   repr SigSy          = "sig"
    |   repr SharingSy      = "sharing"
    |   repr FunctorSy      = "functor"
    |   repr WithtypeSy     = "withtype"
    |   repr EqtypeSy       = "eqtype"
    |   repr IncludeSy      = "include"
    |   repr WhereSy        = "where"
    |   repr WordConst      = "<word constant>"
    |   repr CharConst      = "<char constant>"
    |   repr Othersy        = "unknown symbol"

    local
        open HashTable;

        val reservedWords =
        [
            AbstypeSy,
            AndSy,
            AndalsoSy,
            AsSy,
            CaseSy,
            DatatypeSy, 
            DoSy,
            ElseSy, 
            EndSy,
            ExceptionSy,
            FnSy,
            FunSy,
            HandleSy,
            IfSy,
            InSy,
            InfixSy,
            InfixrSy,
            LetSy,
            LocalSy,
            NonfixSy,
            OfSy,
            OpSy,
            OpenSy,
            OrelseSy,
            RaiseSy,
            RecSy,
            ThenSy,
            TypeSy,
            ValSy,
            WithSy,
            WhileSy,
            StructureSy,
            SignatureSy,
            StructSy,
            SigSy,
            SharingSy,
            FunctorSy,
            WithtypeSy,
            EqtypeSy,
            IncludeSy,
            WhereSy,
            (* Reserved or special symbols *)
            Arrow,
            EqualsSign,
            ThickArrow,
            Asterisk,
            ColonGt,
            Underline,
            Colon,
            VerticalBar,
            HashSign
        ]

        local
            (* Make table sparse to minimise collisions. *)
            val table = hashMake (4 * List.length reservedWords);

            (* Enter each reserved word in the hash table *)
            val () = List.app (fn n => hashSet (table, repr n, n)) reservedWords
        in
            val table = hashFreeze table
        end
    in
        (* Returns the symbol if it is in the table else ident *)
        fun lookup str = getOpt(hashSub (table, str), Ident);
    end;

        (* Custom token representation for testing with the oracle, to align representations *)
        fun tokenToString (sym: sys, text: string) : string =
        case sym of
            (* Tokens with payload *)
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


end (* Symbols *);
