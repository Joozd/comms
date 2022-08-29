package nl.joozd.comms

import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.Closeable
import java.net.Socket
import kotlin.coroutines.CoroutineContext

@Suppress("unused")
class P2PSession: CoroutineScope, Closeable {
    override val coroutineContext: CoroutineContext = Dispatchers.IO
    private val peers = ArrayList<Socket>(2)
    private val forwardingJobs = ArrayList<Job>()

    private var onClosedListener: OnClosedListener? = null

    private var closed = false // closed means this nl.joozd.comms.P2PSession has been used and must be thrown out.

    private val full get() = peersCount == 2

    val peersCount get() = peers.size

    val connected get() =
        peers.size == 2 && peers.all { it.isConnected }

    /**
     * Connect a peer to this P2P session.
     * If this is the second peer, send [CommsKeywords.P2P_CONNECTED] to both peers to signal they can start doing what they want to do.
     */
    fun addPeer(peer: Socket): Boolean{
        if (peers.size == 2 || closed) return false
            peers.add(peer)
        if(peers.size == 2)
            connectPeers()
        return true
    }

    fun setOnClosedListener(onClosedListener: OnClosedListener){
        this.onClosedListener = onClosedListener
    }

    private fun connectPeers(){
        require(connected) { "Cannot connect peers ${peers.getOrNull(0)?.inetAddress} and ${peers.getOrNull(1)?.inetAddress}if they are not both connected to server" }
        forward(peers[0], peers[1])
        forward(peers[1], peers[0])
        peers.forEach{ s ->
            launch {
                try {
                    with(s.getOutputStream()) {
                        write(CommsKeywords.P2P_CONNECTED.toByteArray(Charsets.UTF_8))
                        flush()
                    }
                } catch (e: Exception) {
                    close()
                    throw(e)
                }
            }
        }
    }

    private fun forward(from: Socket, to: Socket){
        forwardingJobs.add(launch{
            while(isActive){
                try{
                    val buffer = ByteArray(BUFFER_SIZE)
                    val inputStream = BufferedInputStream(from.getInputStream())
                    val outputStream = to.getOutputStream()

                    while(true){
                        val readBytes = inputStream.read(buffer)
                        if (readBytes == -1) break
                        outputStream.write(buffer, 0, readBytes)
                    }
                }
                catch (e: Exception){
                    //On any error, close this session and pass on the error.
                    close()
                    throw(e)
                }
            }
        })
    }

    fun interface OnClosedListener{
        fun onClosed()
    }

    companion object{
        private const val BUFFER_SIZE = Client.BUFFER_SIZE
    }

    /**
     * Cancels forwarding jobs,
     * tries to send a [CommsKeywords.P2P_CONNECTION_CLOSED] message to all connections, then closes them
     */
    override fun close() {
        closed = true
        forwardingJobs.forEach {
            it.cancel()
        }
        peers.forEach { socket ->
            socket.use { s ->
                with(s.getOutputStream()) {
                    write(CommsKeywords.P2P_CONNECTION_CLOSED.toByteArray(Charsets.UTF_8))
                    flush()
                }
            }
        }
        onClosedListener?.onClosed()
    }
}