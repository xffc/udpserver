import io.github.xffc.udpserver.UDPServer
import io.github.xffc.udpserver.packets.Packet
import io.github.xffc.udpserver.packets.PacketClass
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.SocketAddress
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlin.random.Random
import kotlin.random.nextInt

suspend fun main() {
    val defer = CompletableDeferred<String>()

    val port = Random.nextInt(20000..30000).toUShort()
    val addr = InetSocketAddress("localhost", port.toInt())

    Packet.registry.add(ExamplePacket)

    val server = UDPServer.build(
        addr,
        sendHandler = { packet, data ->
            val output = Buffer()
            output.writeInt(packet.getClass().id)
            output.write(data, data.size)
            output
        }
    )

    server.onPacket<Packet> {
        println("received $this")
    }

    server.onPacket<ExamplePacket> {
        defer.complete(text)
    }

    val client = aSocket(SelectorManager(Dispatchers.IO))
        .udp().connect(addr)

    client.send(
        Datagram(
            Buffer().also {
                it.writeInt(0)
                ExamplePacket(addr, "Hello, World!").write(it)
            },
            addr
        )
    )

    val text = defer.await()
    client.close()
    server.socket.close()

    println("End text: $text")
}

class ExamplePacket(
    override val address: SocketAddress,
    val text: String
) : Packet {
    override fun write(buffer: Buffer) {
        buffer.writeString(text)
    }

    companion object : PacketClass<ExamplePacket>(0, ExamplePacket::class) {
        override fun read(source: Source): Array<Any> = arrayOf(
            source.readString()
        )
    }
}