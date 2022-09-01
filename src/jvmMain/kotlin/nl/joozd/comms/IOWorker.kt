package nl.joozd.comms

import nl.joozd.serializing.intFromBytes
import java.io.BufferedInputStream
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

/**
 * IOWorker will simplify reading/writing from/to a socket.
 * It should get an open socket upon construction which it will use
 * It should be closed after usage, which will close the socket.
 * @param socket: An open socket to be used for communications
 */
class IOWorker(
    private val inputStream: InputStream,
    private val outputStream: OutputStream,
    val otherAddress: String = "Unknown Host"
    ): Closeable {
    constructor(socket: Socket): this(
        outputStream = socket.getOutputStream(),
        inputStream = socket.getInputStream(),
        otherAddress = socket.inetAddress.hostName ?: "Unknown host"
    )

    /**
     * returns the next input from client
     * @param maxSize: Maximum size of a message to read
     */
    fun read(maxSize: Int = Int.MAX_VALUE): ByteArray = getInput(BufferedInputStream(inputStream), maxSize)


    /**
     * Writes a [Packet] to the connection
     * NOTE: This is a blocking IO function. It does not actually throw any IOExceptions but this way compiler will
     *        know that it is.
     * @return true if sent successfully or false if an error occurred.
     */
    @Throws(IOException::class)
    fun write(data: Packet): Boolean {
        try {
            outputStream.write(data.content)
            outputStream.flush()
        } catch (ie: IOException) {
            ie.printStackTrace()
            return false
        }
        return true
    }

    /**
     * Write a bytearray to the connection.
     * NOTE: This is a blocking IO function. It does not actually throw any IOExceptions but this way compiler will
     *        know that it is. (useful for e.g. Android)
     * @return true if sent successfully or false if an error occurred.
     */
    @Throws(IOException::class)
    fun write(data: ByteArray) = write(Packet(data))

    /**
     * Write a string to the connection.
     * NOTE: This is a blocking IO function. It does not actually throw any IOExceptions but this way compiler will
     *        know that it is. (useful for e.g. Android)
     * This will be received as a ByteArray.
     * @return true if sent successfully or false if an error occurred.
     */
    @Throws(IOException::class)
    fun write(data: String) = write(Packet(data))

    /**
     * Write a List of Bytes to the connection.
     * NOTE: This is a blocking IO function. It does not actually throw any IOExceptions but this way compiler will
     *        know that it is. (useful for e.g. Android)
     * This will be received as a ByteArray.
     * @return true if sent successfully or false if an error occurred.
     */
    @Throws(IOException::class)
    fun write(data: List<Byte>) = write(Packet(data))


    override fun close() {
        inputStream.close()
    }

    /**
     * Read input from socket, using the JoozdComms protocol.
     */
    @Throws(IOException::class)
    private fun getInput(inputStream: BufferedInputStream, maxSize: Int): ByteArray {
        val buffer = ByteArray(8192)
        val header = ByteArray(Packet.HEADER_LENGTH + 4)

        //Read the header as it comes in, or fail trying.
        repeat(header.size) {
            val r = inputStream.read()
            if (r < 0) throw IOException("Stream too short: ${it - 1} bytes")
            header[it] = r.toByte()
        }
        val expectedSize = intFromBytes(header.takeLast(4))
        if (expectedSize > maxSize) throw IOException("size bigger than $maxSize")
        val message = mutableListOf<Byte>()

        //read buffers until correct amount of bytes reached or fail trying
        while (message.size < expectedSize) {
            val b = inputStream.read(buffer)
            if (b < 0) throw IOException("Stream too short: expect $expectedSize, got ${message.size}")
            message.addAll(buffer.take(b))
        }
        return message.toByteArray()
    }
}