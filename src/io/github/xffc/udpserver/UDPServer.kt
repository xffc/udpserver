package io.github.xffc.udpserver

import io.github.xffc.udpserver.packets.Packet
import io.github.xffc.udpserver.packets.PacketClass
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.SocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.isClosed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.io.Buffer

class UDPServer private constructor(
    val socket: BoundDatagramSocket,
    internal val packetHandler: suspend (Datagram) -> PacketClass<*>,
    internal val sendHandler: suspend (Packet, Buffer) -> Buffer
) {
    private val _packetHandlers = MutableSharedFlow<Packet>(extraBufferCapacity = 10)
    val packetHandlers = _packetHandlers.asSharedFlow()

    val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            while (!socket.isClosed) {
                val datagram = socket.receive()

                val packetClass = packetHandler(datagram)
                val packet = packetClass.build(
                    datagram.packet,
                    datagram.address
                )

                handle(packet)
            }
        }
    }

    internal fun handle(packet: Packet) {
        scope.launch {
            _packetHandlers.tryEmit(packet)
        }
    }

    suspend fun send(packet: Packet, to: SocketAddress) {
        val output = Buffer()
        packet.write(output)
        sendHandler(packet, output)

        socket.send(Datagram(output, to))
    }

    inline fun <reified T: Packet> onPacket(crossinline action: suspend T.() -> Unit) =
        scope.launch {
            packetHandlers
                .filterIsInstance<T>()
                .collect { packet -> action.invoke(packet) }
        }

    companion object {
        suspend fun build(
            listen: InetSocketAddress,
            packetHandler: suspend (Datagram) -> PacketClass<*> = { datagram ->
                val id = datagram.packet.readInt()
                Packet.registry.find { it.id == id } ?: throw IllegalArgumentException("Unknown packet with id $id")
            },
            sendHandler: suspend (Packet, Buffer) -> Buffer = { _, buf -> buf }
        ): UDPServer {
            val socket = aSocket(SelectorManager(Dispatchers.IO))
                .udp().bind(listen)

            return UDPServer(socket, packetHandler, sendHandler)
        }

        suspend fun build(
            address: String, port: UShort,
            packetHandler: suspend (Datagram) -> PacketClass<*>,
            sendHandler: suspend (Packet, Buffer) -> Buffer = { _, buf -> buf }
        ): UDPServer =
            build(
                InetSocketAddress(address, port.toInt()),
                packetHandler, sendHandler
            )
    }
}