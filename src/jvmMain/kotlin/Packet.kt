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

import CommsKeywords
import nl.joozd.serializing.toByteArray


/**
 * A Packet is an amount of data to be sent to a JoozdLogServer
 * It contains only raw data, with meta information about size and an identifier
 */

class Packet() {
    val header = CommsKeywords.HEADER.toByteArray(Charsets.UTF_8)

    constructor(msg: ByteArray): this(){
        message = msg
    }
    constructor(msg: String): this(){
        message = msg.toByteArray(Charsets.UTF_8)
    }
    constructor(msg: List<Byte>): this(){
        message = msg.toByteArray()
    }

    /**
     * The actual message
     */
    var message = ByteArray(0)
        private set

    /**
     * Size of the actual message, in bytes
     */
    val messageSize: ByteArray
        get() = message.size.toByteArray()

    /**
     * The complete content of this Packet ([header] + [messageSize] + [message]
     */
    val content: ByteArray
        get() = header + messageSize + message

    companion object{
        val HEADER_LENGTH = CommsKeywords.HEADER.length
    }
}