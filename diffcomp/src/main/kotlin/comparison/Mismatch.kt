package comparison

enum class MismatchType {
    ORACLE_ONLY,
    POLYLEX_ONLY,
    TOKEN_TYPE_MISMATCH,
    TOKEN_TEXT_MISMATCH,
    WRONG_TOKEN_POSITION,
}

data class Mismatch(
    val type: MismatchType,
    val oracleIndex: Int,       //-1 if POLYLEX_ONLY
    val polylexIndex: Int,      //-1 if ORACLE_ONLY
    val oracleToken: String?,   //null if POLYLEX_ONLY
    val polylexToken: String?,  //null if ORACLE_ONLY
)
