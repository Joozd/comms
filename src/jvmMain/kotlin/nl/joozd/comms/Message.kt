package nl.joozd.comms

import nl.joozd.serializing.*

/**
 * A Request has a request string and possible extra data to go with that request
 * @param message: A string that identifies this message
 * @param extraData: Extra data to go with this message
 */
class Message(val message: String, val extraData: ByteArray = ByteArray(0)){
    val bytes: ByteArray
        get() = wrap(message) + extraData

    companion object{
        /**
         * Construct a Message from bytes.
         * @param bytes: A message in the form of [Message.bytes]
         * @return the original [Message]
         */
        fun fromBytes(bytes: ByteArray): Message{
            require (nextType(bytes) == STRING) { "Bad data received in Message.fromBytes()" }
            val message = unwrapString(nextWrap(bytes))
            val extraData = bytes.sliceArray(wrap(message).size until bytes.size) // drop() would return a list

            return Message(message, extraData)
        }
    }
}