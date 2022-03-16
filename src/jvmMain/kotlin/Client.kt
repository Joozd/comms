/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020 Joost Welle
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Affero General Public License as
 *      published by the Free Software Foundation, either version 3 of the
 *      License, or (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Affero General Public License for more details.
 *
 *      You should have received a copy of the GNU Affero General Public License
 *      along with this program.  If not, see https://www.gnu.org/licenses
 *
 */

package nl.joozd.comms

import kotlinx.coroutines.*
import nl.joozd.serializing.intFromBytes
import nl.joozd.serializing.toByteArray
import nl.joozd.serializing.wrap
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLSocketFactory
import kotlin.coroutines.CoroutineContext

class Client private constructor(): Closeable, CoroutineScope {
    private val socket = SSLSocketFactory.getDefault().createSocket(
        SERVER_URL,
        SERVER_PORT
    )
    /**
     * The context of this scope.
     * Context is encapsulated by the scope and used for implementation of coroutine builders that are extensions on the scope.
     * Accessing this property in general code is not recommended for any purposes except accessing the [Job] instance for advanced usages.
     *
     * By convention, should contain an instance of a [Job] to enforce structured concurrency.
     */
    override val coroutineContext: CoroutineContext = Dispatchers.IO + Job()

    /**
     * If socket died, this is set to false.
     * Set to true on creation of socket.
     * Set to false on receiving a socket exception
     */
    var alive = false
        private set

    private suspend fun initialize(): Client{
        socket?.let{
            alive = sendToServer(Packet(wrap(CommsKeywords.HELLO))) == CommsResult.OK
        }
        return this
    }

    /**
     * Send data to server. On success, returns the amount of bytes sent, on failure returns a negative Integer
     * @return [CommsResult]:
     *  [CommsResult.OK] if all OK
     *  [CommsResult.SOCKET_IS_NULL] if socket is not created
     *  [CommsResult.UNKNOWN_HOST] if host was not found
     *  [CommsResult.IO_ERROR] if a generic IO error occurred
     *  [CommsResult.CONNECTION_REFUSED] if connection could not be established by remote host
     *  [CommsResult.SOCKET_ERROR] if there is an error creating or accessing a Socket
     */
    private suspend fun sendToServer(packet: Packet): CommsResult {
        try {
            socket?.let {
                val output = BufferedOutputStream(it.getOutputStream())
                withContext(Dispatchers.IO) {
                    output.write(packet.content)
                    output.flush()
                }
                return CommsResult.OK
            }
            return CommsResult.SOCKET_IS_NULL.also{
                alive = false
            }
        } catch (he: UnknownHostException) {
            return CommsResult.UNKNOWN_HOST.also{
                alive = false
            }
        } catch (ioe: IOException) {
            return CommsResult.IO_ERROR.also{
                alive = false
            }
        } catch (ce: ConnectException) {
            return CommsResult.CONNECTION_REFUSED.also{
                alive = false
            }
        } catch (se: SocketException) {
            return CommsResult.SOCKET_ERROR.also{
                alive = false
            }
        }
    }

    /**
     * Reads data from server
     * @param f: listener with a 0-100 percentage completed value
     * @return received data, or null if an error occured
     */
    suspend fun readFromServer(f: (Int) -> Unit = {}): ByteArray? {
        if (alive) {
            socket?.let {
                return try {
                    withContext(Dispatchers.IO) { getInput(BufferedInputStream(it.getInputStream()), f) }
                } catch (e: IOException) {
                    null
                }
            }
        }
        return null
    }

    /**
     * Gets input from a BufferedInputStream
     * @param inputStream: The InputStream. This must be a complete [Packet]
     * @param f: listener with a 0-100 percentage completed value
     */
    private fun getInput(inputStream: BufferedInputStream, f: (Int) -> Unit = {}): ByteArray {
        val buffer = ByteArray(8192)
        val header = ByteArray(Packet.HEADER_LENGTH + 4)

        //Read the header as it comes in, or fail trying.
        repeat (header.size){
            val r = inputStream.read()
            if (r<0) throw IOException("Stream too short: ${it-1} bytes")
            header[it] = r.toByte()
        }
        val expectedSize =
            intFromBytes(header.takeLast(4))
        if (expectedSize > MAX_MESSAGE_SIZE) throw IOException("size bigger than $MAX_MESSAGE_SIZE")
        val message = mutableListOf<Byte>()

        //read buffers until correct amount of bytes reached or fail trying
        while (message.size < expectedSize){
            f(100*message.size/expectedSize)
            val b = inputStream.read(buffer)
            if (b<0) throw IOException("Stream too short: expect $expectedSize, got ${message.size}")
            message.addAll(buffer.take(b))
        }
        f(100)
        return message.toByteArray()
    }

    /**
     * Tries to send an END_OF_SESSION message, then tries to close the socket no matter what.
     */


    /**
     * Send a request to server
     * @param request: A string as defined in nl.joozd.joozdlogcommon.comms.JoozdlogCommsKeywords
     * @param extraData: A bytearray with extra data to be sent as part of this request
     * @return [CommsResult]:
     *  [CommsResult.OK] if all OK
     *  [CommsResult.CLIENT_NOT_ALIVE] if Client died
     *  Or any of the results that [sendToServer] can return:
     *  [CommsResult.SOCKET_IS_NULL] if socket is not created
     *  [CommsResult.UNKNOWN_HOST] if host was not found
     *  [CommsResult.IO_ERROR] if a generic IO error occurred
     *  [CommsResult.CONNECTION_REFUSED] if connection could not be established by remote host
     *  [CommsResult.SOCKET_ERROR] if there is an error creating or accessing a Socket
     */
    suspend fun sendRequest(request: String, extraData: ByteArray? = null, initializing: Boolean = false): CommsResult = if (alive)
        sendToServer(Packet(wrap(request) + (extraData ?: ByteArray(0))))
    else CommsResult.CLIENT_NOT_ALIVE

    suspend fun sendMessage(message: Message): CommsResult = if (alive)
        sendToServer(Packet(message.bytes))
    else CommsResult.CLIENT_NOT_ALIVE

    /**
     * Will send END_OF_SESSION and close socket
     * Will set lifeCycle to DESTROYED
     * Will cancel all running jobs
     */
    override fun close() {
        launch {
            try {
                socket.use {
                    sendRequest(CommsKeywords.END_OF_SESSION)
                }
            } finally {
                coroutineContext.cancel()
            }
        }
    }


    companion object{
        const val SERVER_URL = "joozd.nl"
        const val SERVER_PORT = 1337
        const val MAX_MESSAGE_SIZE = Int.MAX_VALUE-1

        /**
         * Returns an open instance if it is available
         * Client will be locked until starting timeOut()
         */
        suspend fun getInstance(): Client = withContext(Dispatchers.IO) { Client().initialize() }
    }


}