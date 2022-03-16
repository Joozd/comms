package nl.joozd.comms

import java.net.ServerSocket
import java.net.Socket


/**
 * Uses an [IOWorker] to handle receiving and sending of Packets
 */
abstract class PacketServer(ss: ServerSocket?) : ClassServer(ss){
    override fun handle(socket: Socket) {
        handle(IOWorker(socket))
    }

    /**
     * This function will handle incoming connections.
     * @see IOWorker for how to do that, but basically:
     * worker.read() and worker.write() does the trick.
     */
    abstract fun handle(worker: IOWorker)

}