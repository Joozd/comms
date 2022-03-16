package nl.joozd.comms

object CommsKeywords {
    const val HEADER = "JOOZD"
    const val OK = "OK"
    const val SERVER_ERROR = "SERVER_ERROR "
    const val BAD_DATA_RECEIVED = "BAD_DATA_RECEIVED"
    const val NEXT_IS_COMPRESSED = "NEXT_IS_COMPRESSED"
    const val HELLO = "HELLO" // If protocol changes, we can add HELLO_V2 here to keep backwards compatability
    const val END_OF_SESSION = "END_OF_SESSION"
}