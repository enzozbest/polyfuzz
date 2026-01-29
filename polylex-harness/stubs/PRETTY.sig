(* PRETTY.sig - Stub signature for pretty printing *)
(* This is a simplified version that avoids PolyML-specific internals *)

signature PRETTY =
sig
    type context
    type pretty
    
    val ContextLocation:
        { file: string, startLine: FixedInt.int, startPosition: FixedInt.int, 
          endLine: FixedInt.int, endPosition: FixedInt.int } -> context
    val ContextProperty: string * string -> context

    val PrettyBlock: FixedInt.int * bool * context list * pretty list -> pretty
    val PrettyBreak: FixedInt.int * FixedInt.int -> pretty
    val PrettyString: string -> pretty
    
    val isPrettyBlock: pretty -> bool
    val isPrettyBreak: pretty -> bool
    val isPrettyString: pretty -> bool
    
    val projPrettyBlock: pretty -> int * bool * context list * pretty list
    val projPrettyBreak: pretty -> int * int
    val projPrettyString: pretty -> string

    val uglyPrint: pretty -> string
    
    val prettyPrint: (string -> unit) * int -> pretty -> unit

    val printOutputTag : (pretty -> unit) Universal.tag
    val compilerOutputTag: (pretty -> unit) Universal.tag
    
    val getPrintOutput : Universal.universal list -> pretty -> unit
    val getCompilerOutput : Universal.universal list -> pretty -> unit
    val getSimplePrinter: Universal.universal list * int list -> string -> unit

    val tagPrettyBlock: word
    val tagPrettyBreak: word
    val tagPrettyString: word
    val maxPrettyTag: word

    structure Sharing:
    sig
        type pretty = pretty
        type context = context
    end
end;
